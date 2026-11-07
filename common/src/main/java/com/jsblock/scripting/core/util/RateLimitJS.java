package com.jsblock.scripting.core.util;

/**
 * Exposed to scripts as the {@code RateLimit} constructor: {@code const limiter = new RateLimit(0.5)}.<br>
 * Lets a render function do expensive work at most once per interval instead of once per frame.
 * @author LX86 (JCM)
 */
public class RateLimitJS {
    private final long intervalMillis;
    private long lastUpdateMillis = -1;

    public RateLimitJS(double intervalSeconds) {
        this.intervalMillis = Math.max(0, Math.round(intervalSeconds * 1000.0));
    }

    /** @return whether the interval has passed since the last call; resets the cooldown when it has */
    public boolean shouldUpdate() {
        return shouldUpdate(true);
    }

    /**
     * @param consume whether a successful check also restarts the cooldown
     * @return whether the interval has passed since the last check
     */
    public boolean shouldUpdate(boolean consume) {
        final long now = System.currentTimeMillis();
        if (lastUpdateMillis >= 0 && now - lastUpdateMillis < intervalMillis) return false;
        if (consume) lastUpdateMillis = now;
        return true;
    }

    public void resetCoolDown() {
        lastUpdateMillis = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "RateLimit[" + intervalMillis + "ms]";
    }
}
