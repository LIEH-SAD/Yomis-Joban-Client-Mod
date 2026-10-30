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
    private final boolean builtin;

    public PIDSPresetEntry(String id, PIDSPreset preset, boolean builtin) {
        this.id = id;
        this.preset = preset;
        this.builtin = builtin;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return id;
    }

    public PIDSPreset getPreset() {
        return preset;
    }

    public ResourceLocation getThumbnail() {
        return preset.image;
    }

    public boolean hasThumbnail() {
        return preset.image != null;
    }

    public int getTextColor() {
        return preset.color == null ? DEFAULT_TEXT_COLOR : (0xFF000000 | preset.color);
    }

    public boolean isRowHidden(int row) {
        return preset.visibility != null && row >= 0 && row < preset.visibility.length && preset.visibility[row];
    }

    /**
     * Whether this preset may be used by the given PIDS type.
     * This fork has no per-type blacklist, so every preset is allowed.
     */
    public boolean typeAllowed(String pidsType) {
        return true;
    }

    public boolean isBuiltin() {
        return builtin;
    }
}
