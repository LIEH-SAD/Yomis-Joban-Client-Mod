package com.jsblock.scripting.core.rhino;

import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.core.util.ScriptResourceUtil;
import com.jsblock.scripting.core.util.TimingJS;
import com.jsblock.scripting.primitive.ParsedScript;
import com.jsblock.scripting.primitive.ScriptContent;
import com.jsblock.scripting.primitive.ScriptInstance;
import net.minecraft.resources.ResourceLocation;
import com.jsblock.lib.org.mozilla.javascript.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * A script parsed and executed under the Rhino JS engine.
 * @author LX86 (JCM)
 */
public class RhinoParsedScript extends ParsedScript {
    private Scriptable scope;
    private final List<Function> createFunctions;
    private final List<Function> renderFunctions;
    private final List<Function> disposeFunctions;

    private static final Map<String, Script> compiledScriptCache = new ConcurrentHashMap<>();

    public RhinoParsedScript(ScriptManager scriptManager, String displayName, String contextName, ParsedScript.Configuration configuration) {
        super(scriptManager, displayName, contextName, configuration);
        this.createFunctions = new ArrayList<>();
        this.renderFunctions = new ArrayList<>();
        this.disposeFunctions = new ArrayList<>();
    }

    @Override
    public RhinoParsedScript parse(boolean verboseLogging) {
        try {
            final Context cx = Context.enter();
            try {
                MTRRhinoContextFactory.prepare(cx);
                cx.setClassShutter(new MTRRhinoClassShutter(scriptManager.getClassShutter()));
                scope = new ImporterTopLevel(cx);
                parseScriptContent("\"use strict\";", "Use Strict Statement");
                ScriptResourceUtil.beginParseScript(this);

                scriptManager.addBuiltInTypes(this);
                exposeClassInstance("Timing", timing);
                scriptManager.parseScriptEvent.invoke(e -> e.accept(contextName, this));

                for (final ScriptContent scriptEntry : scriptContents) {
                    final ResourceLocation scriptLocation = scriptEntry.getLocation();
                    ScriptResourceUtil.executeScript(this, scriptLocation, scriptEntry.getContent());

                    tryAndAddFunction("create", createFunctions);
                    tryAndAddFunction("render", renderFunctions);
                    tryAndAddFunction("dispose", disposeFunctions);

                    if (verboseLogging) {
                        scriptManager.getLogger().info("[YJCM Scripting] Loaded script: {}:{}", scriptLocation.getNamespace(), scriptLocation.getPath());
                    }
                }
            } finally {
                ScriptResourceUtil.finishParseScript();
                Context.exit();
            }
        } catch (Exception e) {
            /* A script that failed to parse has no functions at all: keeping it would hand the
               renderer a preset that draws nothing and report success, which is how a broken pack
               ends up looking like a preset that simply renders a blank panel. */
            scriptManager.getLogger().error("[YJCM Scripting] Failed to parse script " + displayName, e);
            return null;
        }
        return this;
    }

    @Override
    public void parseScriptContent(String str, String source) {
        final Context cx = Context.getCurrentContext();
        if (cx == null) throw new IllegalStateException("No rhino context currently available!");
        /* The cache key has to include the source text: several chunks of one preset (scriptInput,
           scriptTexts, the legacy "script" key) and every reload reuse the same names, so keying on
           the name alone hands back a compiled script whose body is no longer the one being parsed. */
        final String cacheKey = source + '\u0000' + str.hashCode() + ':' + str.length();
        Script script = compiledScriptCache.get(cacheKey);
        if (script == null) {
            script = cx.compileString(str, source, 1, null);
            compiledScriptCache.put(cacheKey, script);
        }
        script.exec(cx, scope);
    }

    @Override
    public Future<?> invokeCreateFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        return invokeJSFunctions(instance, createFunctions, () -> {
            if (finishCallback != null) finishCallback.run();
            instance.setCreateFunctionInvoked(true);
        }, () -> instance.setCreateFunctionInvoked(false), instance.getContextObject());
    }

    @Override
    public void invokeRenderFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        if (instance.shouldInvalidate() || instance.scriptTask != null && !instance.scriptTask.isDone()) {
            return;
        }
        if (instance.isCreateFunctionInvoked()) {
            instance.scriptTask = invokeJSFunctions(instance, renderFunctions, finishCallback, null, instance.getContextObject());
        } else {
            instance.scriptTask = invokeCreateFunctions(instance, finishCallback);
        }
    }

    @Override
    public void invokeDisposeFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        invokeJSFunctions(instance, disposeFunctions, finishCallback, null, instance.getContextObject());
    }

    private Future<?> invokeJSFunctions(ScriptInstance<?> scriptInstance, List<Function> functions, Runnable finishExecutionCallback, Runnable errorCallback, Object contextObject) {
        if (duringFailCooldown()) return null;
        if (TimingJS.globalElapsed() - scriptInstance.lastExecuteTime < rateLimit) return null;

        return scriptManager.submitScriptTask(executorService, () -> {
            if (duringFailCooldown()) return;
            if (Thread.currentThread().isInterrupted()) return;

            scriptInstance.getContextObject().resetForNextRun();
            boolean ok = true;

            try {
                final Context cx = Context.enter();
                try {
                    MTRRhinoContextFactory.prepare(cx);
                    cx.setClassShutter(new MTRRhinoClassShutter(scriptManager.getClassShutter()));
                    timing.prepareForScript(scriptInstance);
                    scriptInstance.beginTrackExecuteTime();
                    callFunctions(cx, scriptInstance, functions, contextObject);
                } finally {
                    scriptInstance.endTrackExecuteTime();
                    Context.exit();
                }
            } catch (Exception e) {
                ok = false;
                scriptManager.getLogger().error("[YJCM Scripting] Error executing JS script {}", displayName);
                if (e instanceof RhinoException) {
                    final RhinoException re = (RhinoException) e;
                    scriptManager.getLogger().error(cleanRhinoExceptionMessage(re.getMessage()));
                    if (re.getScriptStackTrace() != null) {
                        for (final String ste : re.getScriptStackTrace().split("\n")) {
                            scriptManager.getLogger().error("{}", ste);
                        }
                    }
                } else {
                    scriptManager.getLogger().error("", e);
                }

                lastFailedTime = System.currentTimeMillis();
                capturedScriptException = new Exception(cleanRhinoExceptionMessage(e.getMessage()), e);
                if (errorCallback != null) errorCallback.run();
            }

            if (ok && finishExecutionCallback != null) {
                try {
                    finishExecutionCallback.run();
                } catch (Exception e) {
                    scriptManager.getLogger().error("[YJCM Scripting] Internal error during finalization work after script execution, this is a bug!", e);
                }
            }
        });
    }

    /**
     * Calls every function of {@code functions} with the three arguments JCM 2.x passes, retrying
     * once with placeholder arrivals when the script threw while reading an arrival it never checked.
     *
     * <p>A PIDS preset cannot be edited from here, and the commonest way one breaks is
     * {@code pids.arrivals().get(1).arrivalTime()} on a platform with a single train: JCM 2.x returns
     * {@code null} there and the whole {@code render()} dies, leaving the panel showing only whatever
     * was drawn before the throw -- usually its background. A board showing a placeholder row beats a
     * board showing nothing, so the choice is made once per script and kept.</p>
     */
    private void callFunctions(Context cx, ScriptInstance<?> scriptInstance, List<Function> functions, Object contextObject) {
        boolean retried = false;
        while (true) {
            try {
                for (final Function func : functions) {
                    func.call(cx, scope, scope, new Object[]{
                            contextObject,
                            scriptInstance.getOrCreateStateObject(() -> cx.newObject(scope)),
                            scriptInstance.getWrapperObject()
                    });
                }
                return;
            } catch (RuntimeException e) {
                if (retried || !adoptLenientArrivals(scriptInstance, e)) throw e;
                retried = true;
                /* The first attempt may already have queued draw calls; replaying them on top of the
                   retry would draw every label twice. */
                scriptInstance.getContextObject().resetForNextRun();
            }
        }
    }

    /**
     * Switches the script to placeholder arrivals if -- and only if -- the throw looks like an
     * unguarded {@code arrivals().get(i)}: a null dereference while it has a PIDS wrapper in hand.
     *
     * @return {@code true} when the caller should retry, which can only happen once per script
     */
    @SuppressWarnings("unchecked")
    private boolean adoptLenientArrivals(ScriptInstance<?> scriptInstance, Exception error) {
        if (!mentionsNull(error)) return false;
        final Object wrapper = scriptInstance.getWrapperObject();
        if (!(wrapper instanceof com.jsblock.scripting.pids.PIDSWrapper)) return false;
        final com.jsblock.scripting.pids.PIDSWrapper pidsWrapper = (com.jsblock.scripting.pids.PIDSWrapper) wrapper;
        if (pidsWrapper.isLenientArrivals() || !adoptLenientArrivals()) return false;

        /* The wrapper type parameter is the caller's; a PIDS script only ever carries a PIDSWrapper. */
        ((ScriptInstance<com.jsblock.scripting.pids.PIDSWrapper>) scriptInstance).setWrapperObject(pidsWrapper.withLenientArrivals());
        scriptManager.getLogger().warn("[YJCM Scripting] {} reads arrivals().get(i) past the end of the list"
                + " without checking for null, which is what JCM 2.x returns there. It is now drawn with a"
                + " placeholder arrival so the panel is not left blank; the preset should test for null.", displayName);
        return true;
    }

    private static boolean mentionsNull(Throwable error) {
        final String message = error == null ? null : error.getMessage();
        return message != null && message.toLowerCase(java.util.Locale.ROOT).contains("null");
    }

    private void tryAndAddFunction(String name, List<Function> listToAdd) {
        final Object func = scope.get(name, scope);
        if (func != Scriptable.NOT_FOUND) {
            if (func instanceof Function) {
                listToAdd.add((Function) func);
            }
            scope.delete(name);
        }
    }

    @Override
    public void exposeClassMethod(String name, Method method) {
        scope.put(name, scope, new NativeJavaMethod(method, name));
    }

    @Override
    public void exposeClass(String name, Class<?> cl) {
        scope.put(name, scope, new NativeJavaClass(scope, cl));
    }

    @Override
    public void exposeClassInstance(String name, Object classInstance) {
        scope.put(name, scope, new NativeJavaObject(scope, classInstance, (Class<?>) null));
    }

    public static void clearScriptCache() {
        compiledScriptCache.clear();
    }

    private static String cleanRhinoExceptionMessage(String s) {
        String errorMessage = s == null ? "Unknown script error" : s;
        if (likelyDueToUndefinedParam(errorMessage)) {
            errorMessage = errorMessage.replace("com.jsblock.lib.org.mozilla.javascript.Undefined", "UNDEFINED");
        }
        return errorMessage;
    }

    private static boolean likelyDueToUndefinedParam(String s) {
        return s != null && (s.startsWith("Can't find method") || s.startsWith("Java constructor for")) && s.contains("com.jsblock.lib.org.mozilla.javascript.Undefined");
    }
}
