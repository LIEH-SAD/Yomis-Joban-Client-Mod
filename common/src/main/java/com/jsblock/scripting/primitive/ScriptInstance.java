package com.jsblock.scripting.primitive;

import com.jsblock.scripting.api.AbstractScriptContext;

import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * A live instance of a {@link ParsedScript}, bound to one object in the world (e.g. a PIDS block).
 * @author LX86 (JCM)
 */
public abstract class ScriptInstance<T> {
    private final AbstractScriptContext contextObject;
    private Object stateObject;
    private T wrapperObject;

    private final ParsedScript parsedScript;
    private double lastExecutionDurationMs;
    private double beginExecuteTime = -1;
    private boolean createFunctionInvoked = false;
    public Future<?> scriptTask;
    public double lastExecuteTime;

    public ScriptInstance(AbstractScriptContext contextObject, ParsedScript script) {
        this.contextObject = contextObject;
        this.parsedScript = script;
    }

    public AbstractScriptContext getContextObject() {
        return contextObject;
    }

    public T getWrapperObject() {
        return wrapperObject;
    }

    public void setWrapperObject(T obj) {
        this.wrapperObject = obj;
    }

    public ParsedScript getScript() {
        return parsedScript;
    }

    public Object getOrCreateStateObject(Supplier<Object> createState) {
        if (this.stateObject == null) this.stateObject = createState.get();
        return stateObject;
    }

    public void beginTrackExecuteTime() {
        this.beginExecuteTime = System.nanoTime();
    }

    public void endTrackExecuteTime() {
        this.lastExecutionDurationMs = (System.nanoTime() - this.beginExecuteTime) / 1000000.0;
        this.beginExecuteTime = -1;
    }

    public double getLastExecutionDurationMs() {
        return this.lastExecutionDurationMs;
    }

    public void setCreateFunctionInvoked(boolean isTrue) {
        this.createFunctionInvoked = isTrue;
    }

    public boolean isCreateFunctionInvoked() {
        return this.createFunctionInvoked;
    }

    public boolean executionTimedOut(int thresholdMs) {
        return this.beginExecuteTime > 0 && (System.nanoTime() - this.beginExecuteTime) > (thresholdMs * 1000000L);
    }

    public abstract boolean shouldInvalidate();
}
