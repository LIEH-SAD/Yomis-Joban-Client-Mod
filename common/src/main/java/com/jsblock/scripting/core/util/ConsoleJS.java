package com.jsblock.scripting.core.util;

import com.jsblock.Joban;
import com.jsblock.client.ClientConfig;
import com.jsblock.lib.org.mozilla.javascript.Context;
import com.jsblock.lib.org.mozilla.javascript.NativeJSON;
import com.jsblock.lib.org.mozilla.javascript.NativeObject;
import com.jsblock.lib.org.mozilla.javascript.RhinoException;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the {@code console} object for scripts.
 * @author LX86 (JCM)
 */
public class ConsoleJS {
    private static final Throwable DUMMY_EXCEPTION = new Throwable();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Long> timers = new HashMap<>();

    public ConsoleJS() {
    }

    public void time() {
        time("default");
    }

    public void time(String label) {
        if (timers.containsKey(label)) {
            warn("Timer \"" + label + "\" already exists.");
        } else {
            timers.put(label, TimingJS.nanoTime());
        }
    }

    public void timeLog() {
        timeLog("default");
    }

    public void timeLog(String label) {
        final long timeNow = TimingJS.nanoTime();
        if (timers.containsKey(label)) {
            log(label + ": " + ((timeNow - timers.get(label)) / 1000 / 1000) + "ms");
        } else {
            warn("Timer \"" + label + "\" doesn't exist.");
        }
    }
    public void timeEnd() {
        timeEnd("default");
    }

    public void timeEnd(String label) {
        final long timeNow = TimingJS.nanoTime();
        if (timers.containsKey(label)) {
            log(label + ": " + ((timeNow - timers.get(label)) / 1000 / 1000) + "ms - timer ended");
            timers.remove(label);
        } else {
            warn("Timer \"" + label + "\" doesn't exist.");
        }
    }

    public void count() {
        count("default");
    }

    public void count(String label) {
        final int newVal = counters.getOrDefault(label, 0) + 1;
        counters.put(label, newVal);
        log(label + ": " + newVal);
    }

    public void countReset() {
        countReset("default");
    }

    public void countReset(String label) {
        if (counters.containsKey(label)) {
            counters.remove(label);
            log(label + ": 0");
        } else {
            warn("Counter " + label + " doesn't exist.");
        }
    }

    public static void logRaw(Object... str) {
        Joban.LOGGER.info("{} {}", buildLogPrefix(), buildArgument(str));
    }

    public static void warnRaw(Object... str) {
        Joban.LOGGER.warn("{} {}", buildLogPrefix(), buildArgument(str));
    }

    public static void errorRaw(Object... str) {
        Joban.LOGGER.error("{} {}", buildLogPrefix(), buildArgument(str));
    }

    public static void debugRaw(Object... str) {
        if (ClientConfig.getScriptDebugMode()) {
            Joban.LOGGER.info("{} {}", buildLogPrefix(), buildArgument(str));
        }
    }

    public void log(Object... objs) {
        logRaw(objs);
    }

    public void warn(Object... objs) {
        warnRaw(objs);
    }

    public void error(Object... objs) {
        errorRaw(objs);
    }

    public void debug(Object... objs) {
        debugRaw(objs);
    }

    private static String buildArgument(Object... objs) {
        final StringBuilder sb = new StringBuilder();
        if (objs == null) {
            sb.append("null");
        } else {
            for (final Object obj : objs) {
                if (obj instanceof NativeObject) {
                    final Context cx = Context.getCurrentContext();
                    if (cx != null) {
                        sb.append(NativeJSON.stringify(cx, (com.jsblock.lib.org.mozilla.javascript.Scriptable) obj, obj, null, " "));
                    } else {
                        sb.append(obj);
                    }
                } else {
                    sb.append(obj);
                }
            }
        }
        return sb.toString();
    }

    private static String buildLogPrefix() {
        final StringBuilder sb = new StringBuilder("[YJCM Scripting]");

        if (ClientConfig.getScriptShowLogSource()) {
            final String source;
            final int lineNo;
            try {
                throw Context.throwAsScriptRuntimeEx(DUMMY_EXCEPTION);
            } catch (RhinoException ex) {
                source = ex.sourceName();
                lineNo = ex.lineNumber();
            }
            if (source != null) {
                sb.append(" (").append(source).append("#").append(lineNo).append(")");
            }
        }
        return sb.toString();
    }
}
