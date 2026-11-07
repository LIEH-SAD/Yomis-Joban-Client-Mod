package com.jsblock.scripting.primitive;

import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.core.util.TimingJS;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A script which has been parsed/executed and is ready for its functions to be invoked.
 * For implementation details see {@link com.jsblock.scripting.core.rhino.RhinoParsedScript}.
 * @author LX86 (JCM)
 */
public abstract class ParsedScript {
    private static final int SCRIPT_RESET_TIME = 4000;

    protected final String displayName;
    protected final String contextName;
    protected final ScriptManager scriptManager;
    protected final TimingJS timing;
    protected final ExecutorService executorService;
    protected final List<ScriptContent> scriptContents;
    protected final double rateLimit;
    protected Exception capturedScriptException = null;
    protected long lastFailedTime = -1;
    private volatile boolean lenientArrivals = false;

    public ParsedScript(ScriptManager scriptManager, String displayName, String contextName, Configuration scriptConfiguration) {
        this.displayName = displayName;
        this.contextName = contextName;
        this.scriptManager = scriptManager;
        this.scriptContents = scriptConfiguration.content();
        this.rateLimit = scriptConfiguration.rateLimit();
        this.timing = new TimingJS();
        this.executorService = scriptManager.getDesignatedScriptExecutor();
    }

    /** Whether we are currently in the cooldown period after an errored execution (the script should not run). */
    public boolean duringFailCooldown() {
        return lastFailedTime != -1 && System.currentTimeMillis() - lastFailedTime <= SCRIPT_RESET_TIME;
    }

    public Exception getCapturedScriptException() {
        return capturedScriptException;
    }

    /**
     * Whether this script is drawn with placeholder arrivals, so {@code pids.arrivals().get(i)}
     * returns an empty arrival past the end of the list instead of {@code null}.<br>
     * Set once, the first time a script throws while reading an arrival it never checked, and kept
     * for the lifetime of the script: a pack that indexes past the end does it every frame.
     */
    public boolean usesLenientArrivals() {
        return lenientArrivals;
    }

    /**
     * @return {@code true} for the call that switched it on, so the caller retries the frame once
     * instead of once per frame
     */
    public boolean adoptLenientArrivals() {
        if (lenientArrivals) return false;
        lenientArrivals = true;
        return true;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getContextName() {
        return this.contextName;
    }

    public double getRateLimit() {
        return this.rateLimit;
    }

    public abstract ParsedScript parse(boolean verboseLogging);

    public abstract void parseScriptContent(String str, String source);

    public abstract Future<?> invokeCreateFunctions(ScriptInstance<?> instance, Runnable finishCallback);

    public abstract void invokeRenderFunctions(ScriptInstance<?> instance, Runnable finishCallback);

    public abstract void invokeDisposeFunctions(ScriptInstance<?> instance, Runnable finishCallback);

    public abstract void exposeClassMethod(String name, Method method);

    public abstract void exposeClass(String name, Class<?> cl);

    public abstract void exposeClassInstance(String name, Object classInstance);

    public static class Configuration {
        private final List<ScriptContent> content;
        private final String scriptingLanguage;
        private final double rateLimit;

        private Configuration(List<ScriptContent> content, String scriptingLanguage, double rateLimit) {
            this.content = content;
            this.scriptingLanguage = scriptingLanguage;
            this.rateLimit = rateLimit;
        }

        public static Configuration create(List<ScriptContent> content, String scriptingLanguage, double rateLimit) {
            return new Configuration(content, scriptingLanguage, rateLimit);
        }

        public String scriptingLanguage() {
            return this.scriptingLanguage;
        }

        public double rateLimit() {
            return this.rateLimit;
        }

        public List<ScriptContent> content() {
            return this.content;
        }
    }
}
