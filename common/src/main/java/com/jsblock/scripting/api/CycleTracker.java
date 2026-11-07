package com.jsblock.scripting.api;

import com.jsblock.lib.org.mozilla.javascript.Scriptable;
import com.jsblock.lib.org.mozilla.javascript.ScriptableObject;

/**
 * A {@link StateTracker} that moves on by itself after a set time.<br>
 * Constructed from a JavaScript array alternating state and duration in seconds, exactly as
 * JCM 2.x documents it: {@code new CycleTracker(["route", 5, "nextStation", 5])}.
 * @author LX86 (JCM)
 */
public class CycleTracker extends StateTracker {
    private final Object[] states;
    private final double[] durationsSeconds;
    private long enteredAtMillis = System.currentTimeMillis();
    private int index;

    public CycleTracker(Scriptable params) {
        final Object lengthValue = params == null ? null : ScriptableObject.getProperty(params, "length");
        final int length = lengthValue instanceof Number ? ((Number) lengthValue).intValue() : 0;
        final int pairs = Math.max(0, length / 2);
        this.states = new Object[pairs];
        this.durationsSeconds = new double[pairs];
        for (int i = 0; i < pairs; i++) {
            states[i] = ScriptableObject.getProperty(params, i * 2);
            final Object duration = ScriptableObject.getProperty(params, i * 2 + 1);
            durationsSeconds[i] = duration instanceof Number ? ((Number) duration).doubleValue() : 0.0;
        }
        if (pairs > 0) {
            setState(states[0]);
        }
    }

    /** Advance to the next state when the current one has lasted long enough. */
    public void tick() {
        if (states.length == 0) return;
        final double held = (System.currentTimeMillis() - enteredAtMillis) / 1000.0;
        if (held >= durationsSeconds[index]) {
            index = (index + 1) % states.length;
            enteredAtMillis = System.currentTimeMillis();
            setState(states[index]);
        }
    }

    @Override
    public String stateNow() {
        final Object value = super.stateNow();
        return value == null ? "" : value.toString();
    }

    @Override
    public String stateLast() {
        final Object value = super.stateLast();
        return value == null ? null : value.toString();
    }
}
