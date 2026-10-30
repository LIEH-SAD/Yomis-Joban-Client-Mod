package com.jsblock.scripting.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-script-instance context object handed to the script's create/render/dispose functions.
 * @author LX86 (JCM)
 */
public abstract class AbstractScriptContext {
    private final String name;
    private final Map<String, Object> debugInfoMap;
    private int keylessDebugInfoCounter = 1;

    public AbstractScriptContext(String name) {
        this.name = name;
        this.debugInfoMap = new LinkedHashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public void setDebugInfo(Object value) {
        final String key = keylessDebugInfoCounter == 1 ? "<Untitled>" : "<Untitled" + keylessDebugInfoCounter + ">";
        setDebugInfo(key, value);
        keylessDebugInfoCounter++;
    }

    public void setDebugInfo(String key, Object value) {
        debugInfoMap.put(key, value);
    }

    public Map<String, Object> getDebugInfo() {
        return debugInfoMap;
    }

    public void resetForNextRun() {
        keylessDebugInfoCounter = 1;
    }
}
