package com.jsblock.scripting.core;

import com.jsblock.scripting.api.MTRClassShutter;
import com.jsblock.scripting.core.util.*;
import com.jsblock.scripting.primitive.ParsedScript;
import com.jsblock.scripting.primitive.ScriptContent;
import com.jsblock.scripting.resource.ScriptResourceLoader;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Owns the script lifecycle: parsing, resource discovery, instance tracking and execution.
 * @author LX86 (JCM)
 */
public class ScriptManager {
    private final Logger logger;
    private final ExecutorService ioExecutorService;
    private final List<ExecutorService> scriptExecutorServices;

    private final MTRClassShutter classShutter;
    private final ScriptInstanceManager scriptInstanceManager;

    private final ScriptResourceLoader resourceLoader;

    public final Event<BiConsumer<String, ParsedScript>> parseScriptEvent = new Event<>();
    public final Event<Consumer<ParsedScript>> finishParseScriptEvent = new Event<>();

    private int lastExecutorIndex = 0;

    public ScriptManager(Logger logger, ExecutorService ioExecutorService, List<ExecutorService> scriptExecutorServices) {
        this.logger = logger;
        this.ioExecutorService = ioExecutorService;
        this.scriptExecutorServices = scriptExecutorServices;
        this.classShutter = new MTRClassShutter();
        this.scriptInstanceManager = new ScriptInstanceManager();
        this.resourceLoader = new ScriptResourceLoader(this, ioExecutorService);
    }

    public ParsedScript parseScript(String displayName, String contextName, ParsedScript.Configuration configuration, boolean verboseLogging) {
        final String language = configuration.scriptingLanguage();
        if (language == null || language.isEmpty()) return null;

        final com.jsblock.scripting.api.ScriptingLanguage scriptingLanguage = ScriptingLanguageRegistry.get(language);
        if (scriptingLanguage == null) {
            logger.error("[YJCM Scripting] Failed to parse script {}: Unknown scripting language \"{}\".", displayName, language);
            return null;
        }

        final ParsedScript parsedScript = scriptingLanguage.createParsedScript(this, displayName, contextName, configuration).parse(verboseLogging);
        /* A script that failed to parse is reported by the backend and is null here; passing it on
           would hand every listener a script whose functions do not exist. */
        if (parsedScript != null) {
            finishParseScriptEvent.invoke(e -> e.accept(parsedScript));
        }
        return parsedScript;
    }

    /** Convenience overload used by PIDS: parse a single inline script for a resource location. */
    public void parseScript(ResourceLocation location, String content, Consumer<ParsedScript> onParsed, boolean verboseLogging) {
        if (content == null || content.isEmpty()) return;
        ioExecutorService.execute(() -> {
            final ParsedScript parsedScript = parseScript(location.toString(), "pids", ParsedScript.Configuration.create(Collections.singletonList(new ScriptContent(location, content)), "js", 0), verboseLogging);
            if (parsedScript != null && onParsed != null) onParsed.accept(parsedScript);
        });
    }

    public void addBuiltInTypes(ParsedScript script) {
        script.exposeClassMethod("include", findMethod(ScriptResourceUtil.class, "includeScript", Object.class));
        script.exposeClassMethod("print", findMethod(ScriptResourceUtil.class, "print", Object[].class));
        script.exposeClassInstance("Resources", new ResourcesJS());
        script.exposeClass("TextUtil", TextUtilJS.class);
        script.exposeClass("MinecraftClient", MinecraftClientJS.class);
        script.exposeClassInstance("Java", new JavaUtilJS());
        script.exposeClassInstance("console", new ConsoleJS());
        script.exposeClassInstance("Timing", new TimingJS());

        /* JCM 2.x globals that a modern preset expects to find already defined. Each one is a plain
           class or instance handed to the scope, so a script that never mentions it pays nothing. */
        script.exposeClass("Vector3f", com.jsblock.scripting.api.Vector3f.class);
        script.exposeClass("StateTracker", com.jsblock.scripting.api.StateTracker.class);
        script.exposeClass("CycleTracker", com.jsblock.scripting.api.CycleTracker.class);
        script.exposeClass("RateLimit", RateLimitJS.class);
        script.exposeClass("Files", FilesJS.class);
        script.exposeClass("Networking", NetworkJS.Networking.class);
        script.exposeClass("NetworkResponse", NetworkJS.NetworkResponse.class);
        script.exposeClass("BackgroundWorker", NetworkJS.BackgroundWorker.class);
        script.exposeClass("MTRClientData", mtr.client.ClientData.class);
    }

    public Future<?> submitScriptTask(ExecutorService executorService, Runnable runnable) {
        return executorService.submit(runnable);
    }

    public ExecutorService getDesignatedScriptExecutor() {
        final int currentIndex = lastExecutorIndex % scriptExecutorServices.size();
        lastExecutorIndex = (lastExecutorIndex + 1) % scriptExecutorServices.size();
        return scriptExecutorServices.get(currentIndex);
    }

    public MTRClassShutter getClassShutter() {
        return classShutter;
    }

    public Logger getLogger() {
        return logger;
    }

    public ScriptInstanceManager getInstanceManager() {
        return scriptInstanceManager;
    }

    public ScriptResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public List<ExecutorService> getScriptExecutors() {
        return scriptExecutorServices;
    }

    public void reset() {
        scriptInstanceManager.reset();
        resourceLoader.reset();
        ScriptResourceUtil.resetCache();
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... args) {
        try {
            return clazz.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Simple ordered listener list. */
    public static class Event<T> {
        private final List<T> listeners = new ArrayList<>();

        public void register(T listener) {
            listeners.add(listener);
        }

        public void invoke(Consumer<T> consumer) {
            for (final T listener : listeners) {
                consumer.accept(listener);
            }
        }

        public void clear() {
            listeners.clear();
        }
    }

    /** Small indirection so the manager does not hard-depend on the Rhino backend. */
    public static class ScriptingLanguageRegistry {
        private static final List<com.jsblock.scripting.api.ScriptingLanguage> LANGUAGES = new ArrayList<>();

        public static void add(com.jsblock.scripting.api.ScriptingLanguage language) {
            LANGUAGES.add(language);
        }

        public static com.jsblock.scripting.api.ScriptingLanguage get(String id) {
            for (final com.jsblock.scripting.api.ScriptingLanguage language : LANGUAGES) {
                if (language.getId().equals(id)) return language;
            }
            return null;
        }

        public static void clear() {
            LANGUAGES.clear();
        }
    }
}
