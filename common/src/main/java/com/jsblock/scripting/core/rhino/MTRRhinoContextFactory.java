package com.jsblock.scripting.core.rhino;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Rhino context factory that aborts runaway scripts after a fixed amount of wall-clock time.
 * @author LX86 (JCM)
 */
public class MTRRhinoContextFactory extends ContextFactory {
    private static final int MAX_EXECUTION_TIME_MS = 10000;

    public static class MTRRhinoContext extends Context {
        long startTime = -1;

        public MTRRhinoContext(ContextFactory contextFactory) {
            super(contextFactory);
            setInstructionObserverThreshold(10000);
        }
    }

    public static void init() {
        ContextFactory.initGlobal(new MTRRhinoContextFactory());
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
