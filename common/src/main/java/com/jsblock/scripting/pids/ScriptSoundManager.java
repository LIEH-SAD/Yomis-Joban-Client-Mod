package com.jsblock.scripting.pids;

/**
 * Stand-in for JCM 2.3's {@code ScriptSoundManager}, exposed to scripts through
 * {@code ctx.getSoundManager()}. MTR 3.x PIDS screens have no positional audio support,
 * so every call is accepted and ignored.
 * @author LX86 (JCM)
 */
public class ScriptSoundManager {
    public void playSound(Object... args) {
    }

    public void queue(Object... args) {
    }

    public void stopSound(Object... args) {
    }

    public void stopAll() {
    }
}
