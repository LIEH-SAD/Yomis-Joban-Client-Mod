package com.jsblock.scripting.core.util;

import com.jsblock.Joban;
import net.minecraft.resources.ResourceLocation;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Set;

/**
 * Resource helpers exposed to scripts as the {@code Resources} global.
 * Must be a public top-level class, otherwise Rhino cannot reach its members.
 * @author LX86 (JCM)
 */
public class ResourcesJS {
    /** Logical families AWT has to be able to answer even before the first window is created. */
    private static final Set<String> LOGICAL_FAMILIES = Set.of("dialog", "dialoginput", "monospaced", "serif", "sansserif");

    public DataReaderJS read(Object id) {
        return ScriptResourceUtil.read(String.valueOf(id));
    }

    public boolean exist(Object id) {
        return ScriptResourceUtil.exist(String.valueOf(id));
    }

    public ResourceLocation id(Object id) {
        return ScriptResourceUtil.id(String.valueOf(id));
    }

    public ResourceLocation idr(Object id) {
        return ScriptResourceUtil.idr(String.valueOf(id));
    }

    /** @return the MTR version in use; an empty string when MTR is missing */
    public String getMTRVersion() {
        final String version = Joban.getMTRVersion();
        return version == null ? "" : version;
    }

    /**
     * @param modId {@code jcm} or {@code jsblock}
     * @return this mod's version, or an empty string for any other mod id
     */
    public String getAddonVersion(String modId) {
        if (modId == null) return "";
        return "jcm".equals(modId) || Joban.MOD_ID.equals(modId) ? Joban.getVersion() : "";
    }

    /** Decode a texture from a resource location so a script can inspect or draw its pixels. */
    public BufferedImage readBufferedImage(Object id) {
        final DataReaderJS reader = read(id);
        return reader == null ? null : reader.asBufferedImage();
    }

    /**
     * {@code Resources.getSystemFont("serif")} — a logical AWT font by family name.<br>
     * JCM 2.x also resolves families from the fonts a running game exposes; logical families and
     * installed families are what this build can honour, and an unknown name falls back to the
     * default font rather than throwing, so a preset naming a font the player lacks still renders.
     */
    public Font getSystemFont(String name) {
        if (name == null || name.isEmpty()) return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        final String family = name.toLowerCase(Locale.ROOT);
        if (LOGICAL_FAMILIES.contains(family)) return new Font(family, Font.PLAIN, 12);
        for (final String installed : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (installed.equalsIgnoreCase(name)) return new Font(installed, Font.PLAIN, 12);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }
}
