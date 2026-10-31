package com.jsblock.data;

import net.minecraft.resources.ResourceLocation;

/**
 * A named {@link PIDSPreset}, as listed in the PIDS preset picker.<br>
 * Equivalent of JCM 2.3's {@code PIDSPresetBase}, minus the rendering code
 * (this fork renders presets through {@code PIDSPreset} instead).
 */
public class PIDSPresetEntry {
    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;

    private final String id;
    private final PIDSPreset preset;

    public PIDSPresetEntry(String id, PIDSPreset preset) {
        this.id = id;
        this.preset = preset;
    }

    public String getId() {
        return id;
    }

    /** Display name of the preset, as declared by {@code name} in the JSON (defaults to the id). */
    public String getName() {
        return preset.name == null || preset.name.isEmpty() ? id : preset.name;
    }

    public PIDSPreset getPreset() {
        return preset;
    }

    public ResourceLocation getThumbnail() {
        return preset.getThumbnail();
    }

    public boolean hasThumbnail() {
        return preset.hasThumbnail();
    }

    public int getTextColor() {
        return preset.color == null ? DEFAULT_TEXT_COLOR : (0xFF000000 | preset.color);
    }

    public boolean isRowHidden(int row) {
        return preset.visibility != null && row >= 0 && row < preset.visibility.length && preset.visibility[row];
    }

    /** Whether this preset may be used by the given PIDS type. */
    public boolean typeAllowed(String pidsType) {
        return preset.typeAllowed(pidsType);
    }

    public boolean isBuiltin() {
        return preset.builtin;
    }

    /** Whether the preset draws itself through a script rather than the built-in row renderer. */
    public boolean isScriptPreset() {
        return preset.hasScript();
    }
}
