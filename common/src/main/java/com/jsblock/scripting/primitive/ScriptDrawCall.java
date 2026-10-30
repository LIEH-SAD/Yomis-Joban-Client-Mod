package com.jsblock.scripting.primitive;

/**
 * Marker for a scripted object that may be handed to the {@code draw} function.
 * @author LX86 (JCM)
 */
public interface ScriptDrawCall {
    int getWidth();

    int getHeight();
}
