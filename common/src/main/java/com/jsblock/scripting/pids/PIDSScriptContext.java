package com.jsblock.scripting.pids;

import com.jsblock.scripting.api.AbstractScriptContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Context object for PIDS scripts. Collects draw calls produced by {@code draw()}.
 * @author LX86 (JCM)
 */
public class PIDSScriptContext extends AbstractScriptContext {
    private static final double DEFAULT_Z_ORDER_STEP = 0.0002;

    private final List<PIDSDrawCall<?>> drawCalls = Collections.synchronizedList(new ArrayList<>());
    private final ScriptRenderManager renderManager = new ScriptRenderManager(this);
    private final ScriptSoundManager soundManager = new ScriptSoundManager();
    private final Map<String, Object> debugInfo = Collections.synchronizedMap(new LinkedHashMap<>());
    private double zOrderStep = DEFAULT_Z_ORDER_STEP;
    private boolean autoZOrdering = true;
    private int currentZOrder = 0;

    public PIDSScriptContext() {
        super("pids");
    }

    /**
     * Queue an object for rendering. Non-draw-call arguments are silently ignored.
     * When auto z-ordering is enabled, each queued draw call is assigned the next z-order slot
     * so that overlapping draws do not z-fight.
     */
    public void draw(Object obj) {
        if (obj instanceof PIDSDrawCall<?>) {
            final PIDSDrawCall<?> drawCall = (PIDSDrawCall<?>) obj;
            if (autoZOrdering) {
                drawCall.setAutoZOrder(currentZOrder);
                currentZOrder++;
            }
            drawCalls.add(drawCall);
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

    public ScriptRenderManager getRenderManager() {
        return renderManager;
    }

    public ScriptSoundManager getSoundManager() {
        return soundManager;
    }

    public void setAutoZOrdering(boolean autoZOrdering) {
        this.autoZOrdering = autoZOrdering;
    }

    public boolean isAutoZOrdering() {
        return autoZOrdering;
    }

    /** @param zOrderStep the distance in blocks between two consecutive z-order slots */
    public void setZOrderStep(double zOrderStep) {
        this.zOrderStep = zOrderStep;
    }

    public double getZOrderStep() {
        return zOrderStep;
    }

    public int getCurrentZOrder() {
        return currentZOrder;
    }

    /** Record a value to display on the script debug overlay. */
    public void setDebugInfo(String key, Object value) {
        debugInfo.put(key == null ? "<Untitled>" : key, value);
    }

    /** Record a value to display on the script debug overlay, using {@code <Untitled>} as key. */
    public void setDebugInfo(Object value) {
        setDebugInfo("<Untitled>", value);
    }

    public Map<String, Object> getDebugInfo() {
        return Collections.unmodifiableMap(debugInfo);
    }

    @Override
    public void resetForNextRun() {
        super.resetForNextRun();
        drawCalls.clear();
        debugInfo.clear();
        currentZOrder = 0;
    }
}
