package com.jsblock.scripting.core.rhino;

import com.jsblock.scripting.api.ScriptingLanguage;
import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.primitive.ParsedScript;

/**
 * Rhino/ECMAScript backend for the scripting layer.
 * @author LX86 (JCM)
 */
public class RhinoJSLanguage extends ScriptingLanguage {
    public RhinoJSLanguage() {
        super(ScriptingLanguage.RHINO_JS);
    }

    @Override
    public ParsedScript createParsedScript(ScriptManager scriptManager, String displayName, String contextName, ParsedScript.Configuration configuration) {
        return new RhinoParsedScript(scriptManager, displayName, contextName, configuration);
    }
}
