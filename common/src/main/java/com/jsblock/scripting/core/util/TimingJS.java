package com.jsblock.scripting.core.util;

/**
 * Global + per-script timing information, exposed to scripts as {@code Timing}.
 * @author LX86 (JCM) / Zbx1425
 */
public class TimingJS {
    private static double totalRunningTime = 0;
    private static long lastUpdateNano = -1;
    private double timeElapsedForScript = 0;
    private double frameDeltaForScript = 0;

    /**
     * Advance the global clock. Should be called once per client frame.
     * @param paused whether the game is currently paused (time stops advancing)
     */
    public static void update(boolean paused) {
        final long now = System.nanoTime();
        if (lastUpdateNano == -1) {
            lastUpdateNano = now;
            return;
        }
        final double elapsedMs = (now - lastUpdateNano) / 1000000.0;
        lastUpdateNano = now;
        if (!paused) {
            totalRunningTime += elapsedMs / 1000.0;
        }
    }

    public void prepareForScript(com.jsblock.scripting.primitive.ScriptInstance<?> scriptInstance) {
        timeElapsedForScript = totalRunningTime;
        frameDeltaForScript = totalRunningTime - scriptInstance.lastExecuteTime;
        scriptInstance.lastExecuteTime = timeElapsedForScript;
    }

    public static double globalElapsed() {
        return totalRunningTime;
    }

    public double elapsed() {
        return timeElapsedForScript;
    }

    public double delta() {
        return frameDeltaForScript;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long nanoTime() {
        return System.nanoTime();
    }
}
