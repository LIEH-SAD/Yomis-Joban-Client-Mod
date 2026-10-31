package com.jsblock.scripting.core.rhino;

import com.jsblock.lib.org.mozilla.javascript.Callable;
import com.jsblock.lib.org.mozilla.javascript.Context;
import com.jsblock.lib.org.mozilla.javascript.ContextFactory;
import com.jsblock.lib.org.mozilla.javascript.Scriptable;

/**
 * Rhino context factory that aborts runaway scripts after a fixed amount of wall-clock time.
 * @author LX86 (JCM)
 */
public class MTRRhinoContextFactory extends ContextFactory {
    private static final int MAX_EXECUTION_TIME_MS = 10000;
    /**
     * Interpreted mode, i.e. never generate bytecode for scripts.<p>
     * Rhino's optimizer writes the engine's own package name into the descriptors of the classes it
     * emits, and the Forgix-merged distribution jar re-prefixes every package it packs, so those
     * descriptors stop resolving and compilation dies with
     * {@code NoClassDefFoundError: com/jsblock/lib/org/mozilla/javascript/Context} (or a
     * {@code VerifyError} when only part of the constants were rewritten). Interpreted mode compiles
     * no classes at all, which keeps the engine working both in the dev workspace and in the merged jar.
     */
    public static final int INTERPRETED_OPTIMIZATION_LEVEL = -1;

    public static class MTRRhinoContext extends Context {
        long startTime = -1;

        public MTRRhinoContext(ContextFactory contextFactory) {
            super(contextFactory);
            setInstructionObserverThreshold(10000);
            setOptimizationLevel(INTERPRETED_OPTIMIZATION_LEVEL);
        }
    }

    public static void init() {
        if (!ContextFactory.hasExplicitGlobal()) {
            ContextFactory.initGlobal(new MTRRhinoContextFactory());
        }
    }

    /**
     * Applies the settings every script compilation/execution needs.
     * Used on the current context as well, because contexts created before {@link #init()} ran (or
     * created by another mod's factory) do not inherit the settings of {@link #makeContext()}.
     */
    public static void prepare(Context cx) {
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setOptimizationLevel(INTERPRETED_OPTIMIZATION_LEVEL);
    }

    @Override
    protected Context makeContext() {
        return new MTRRhinoContext(this);
    }

    @Override
    protected void observeInstructionCount(Context ctx, int instructionCount) {
        final long startTime = ((MTRRhinoContext) ctx).startTime;
        if (startTime == -1) return;
        if (System.currentTimeMillis() - startTime > MAX_EXECUTION_TIME_MS) {
            throw new RuntimeException("[YJCM Scripting] Script execution exceeded the maximum of " + (MAX_EXECUTION_TIME_MS / 1000) + "s!");
        }
    }

    @Override
    protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ((MTRRhinoContext) cx).startTime = System.currentTimeMillis();
        return super.doTopCall(callable, cx, scope, thisObj, args);
    }
}
