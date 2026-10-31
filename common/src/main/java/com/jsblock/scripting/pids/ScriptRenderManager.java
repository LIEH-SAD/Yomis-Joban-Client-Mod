package com.jsblock.scripting.pids;

/**
 * Minimal stand-in for JCM 2.3's {@code ScriptRenderManager}, exposed to scripts through
 * {@code ctx.getRenderManager()}. PIDS scripts normally queue their draw calls with
 * {@code draw(ctx)}; this allows the {@code getRenderManager().draw(...)} form as well.
 * @author LX86 (JCM)
 */
public class ScriptRenderManager {
    private final PIDSScriptContext context;

    ScriptRenderManager(PIDSScriptContext context) {
        this.context = context;
    }

    public void draw(Object obj) {
        if (obj instanceof PIDSDrawCall<?>) {
            context.draw(obj);
        }
    }

    public void queue(Object obj) {
        draw(obj);
    }

    /**
     * Model rendering is not part of the PIDS canvas on MTR 3.x; the call is accepted so that
     * scripts using it keep running.
     */
    public void drawModel(Object... args) {
    }

    public void drawDynamicModel(Object... args) {
    }
}
