package com.jsblock.scripting.pids;

import com.jsblock.scripting.api.AbstractScriptContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Context object for PIDS scripts. Collects draw calls produced by {@code draw()}.
 * @author LX86 (JCM)
 */
public class PIDSScriptContext extends AbstractScriptContext {
    private final List<PIDSDrawCall<?>> drawCalls = Collections.synchronizedList(new ArrayList<>());

    public PIDSScriptContext() {
        super("pids");
    }

    /** Queue an object for rendering. Non-draw-call arguments are silently ignored. */
    public void draw(Object obj) {
        if (obj instanceof PIDSDrawCall<?>) {
            drawCalls.add((PIDSDrawCall<?>) obj);
        }
    }

    public void drawAll(Object... objs) {
        if (objs == null) return;
        for (final Object obj : objs) {
            draw(obj);
        }
    }

    public List<PIDSDrawCall<?>> getDrawCalls() {
        synchronized (drawCalls) {
            return new ArrayList<>(drawCalls);
        }
    }

    @Override
    public void resetForNextRun() {
        super.resetForNextRun();
        drawCalls.clear();
    }
}
