package com.jsblock.scripting.api;

import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.primitive.ParsedScript;

/**
 * A scripting language backend (only Rhino/JS is provided).
 * @author LX86 (JCM)
 */
public abstract class ScriptingLanguage {
    public static final String RHINO_JS = "js";

    private final String id;

    protected ScriptingLanguage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract ParsedScript createParsedScript(ScriptManager scriptManager, String displayName, String contextName, ParsedScript.Configuration configuration);
}
