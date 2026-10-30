package com.jsblock.scripting;

import com.jsblock.client.ClientConfig;
import com.jsblock.scripting.api.ClassRule;
import com.jsblock.scripting.api.MTRClassShutter;
import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.pids.RectangleWrapper;
import com.jsblock.scripting.pids.TextWrapper;
import com.jsblock.scripting.pids.TextureWrapper;

/**
 * Registers the globals and allowed classes made available to scripts.
 * @author LX86 (JCM)
 */
public class ScriptRegistry {
    private final ScriptManager scriptManager;

    public ScriptRegistry(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public void register() {
        final MTRClassShutter classShutter = scriptManager.getClassShutter();
        classShutter.setEnabled(!ClientConfig.getDisableScriptRestrictions());
        classShutter.allowClass(ClassRule.parse("com.jsblock.scripting.*"));

        scriptManager.parseScriptEvent.register((contextName, script) -> {
            if ("pids".equals(contextName)) {
                script.exposeClass("Text", TextWrapper.class);
                script.exposeClass("Texture", TextureWrapper.class);
                script.exposeClass("Rect", RectangleWrapper.class);
            }
        });
    }

    public void refreshRestrictions() {
        scriptManager.getClassShutter().setEnabled(!ClientConfig.getDisableScriptRestrictions());
    }
}
