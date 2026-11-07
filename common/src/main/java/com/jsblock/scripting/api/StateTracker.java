package com.jsblock.scripting.api;

import java.util.Objects;

/**
 * Remembers the previous value of something, so a script can act on the change instead of on the
 * state itself.<br>
 * JCM 2.x documents this as the {@code StateTracker} global; without it a preset that plays a sound
 * "when the train arrives" fires on every frame the condition holds.
 * @author LX86 (JCM)
 */
public class StateTracker {
    private Object now;
    private Object last;
    private boolean hasLast;
    private boolean changedThisRun;
    private long changedAtMillis = System.currentTimeMillis();

    public void setState(Object value) {
        if (!Objects.equals(now, value)) {
            last = now;
            hasLast = true;
            now = value;
            changedThisRun = true;
            changedAtMillis = System.currentTimeMillis();
        } else {
            changedThisRun = false;
        }
    }

    public Object stateNow() {
        return now;
    }

    /** @return the state before the last change, or {@code null} when there is none */
    public Object stateLast() {
        return hasLast ? last : null;
    }

    /** @return how long, in seconds, the current state has lasted */
    public double stateNowDuration() {
        return (System.currentTimeMillis() - changedAtMillis) / 1000.0;
    }

    /** @return whether the state changed on the last {@link #setState(Object)} call */
    public boolean stateNowFirst() {
        return changedThisRun;
    }

    public boolean changedTo(Object value) {
        return changedThisRun && Objects.equals(now, value);
    }

    public boolean changedFromTo(Object oldValue, Object value) {
        return changedThisRun && hasLast && Objects.equals(last, oldValue) && Objects.equals(now, value);
    }

    @Override
    public String toString() {
        return "StateTracker[now=" + now + ", last=" + (hasLast ? last : null) + "]";
    }
}
