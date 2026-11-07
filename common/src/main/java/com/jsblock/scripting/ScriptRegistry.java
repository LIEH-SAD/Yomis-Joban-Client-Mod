package com.jsblock.scripting;

import com.jsblock.client.ClientConfig;
import com.jsblock.scripting.api.ClassRule;
import com.jsblock.scripting.api.Matrices;
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
        // Minecraft value types that cross the script boundary: Resources.id() returns a
        // ResourceLocation and PIDSWrapper.blockPos() returns a BlockPos. Without these rules
        // Rhino throws "Access to Java class ... is prohibited" while wrapping the return value.
        classShutter.allowClass(
                ClassRule.parse("net.minecraft.resources.ResourceLocation"),
                ClassRule.parse("net.minecraft.core.BlockPos"),
                // MTRClientData hands scripts MTR's own cached data, so the value types it returns
                // (Route, Station, Platform, DataCache) have to stay reachable.
                ClassRule.parse("mtr.data.*"),
                ClassRule.parse("mtr.client.ClientData")
        );

        scriptManager.parseScriptEvent.register((contextName, script) -> {
            if ("pids".equals(contextName)) {
                script.exposeClass("Text", TextWrapper.class);
                script.exposeClass("Texture", TextureWrapper.class);
                /* JCM 2.x calls the draw call Rectangle; earlier builds of this mod called it Rect, so
                   both names point at the same class and a pack written for either one still loads. */
                script.exposeClass("Rectangle", RectangleWrapper.class);
                script.exposeClass("Rect", RectangleWrapper.class);
                script.exposeClass("Matrices", Matrices.class);
            }
        });
    }

    public void refreshRestrictions() {
        scriptManager.getClassShutter().setEnabled(!ClientConfig.getDisableScriptRestrictions());
    }
}
