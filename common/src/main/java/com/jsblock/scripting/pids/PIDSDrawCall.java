package com.jsblock.scripting.pids;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.data.IGui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;

/**
 * A single 2D drawing instruction produced by a PIDS script.
 * Coordinates are in canvas units, with (0, 0) at the top-left of the PIDS screen.
 * @author LX86 (JCM)
 */
public abstract class PIDSDrawCall<T extends PIDSDrawCall<T>> {
    protected float x = 0;
    protected float y = 0;
    protected float width = 20;
    protected float height = 20;

    @SuppressWarnings("unchecked")
    public T pos(float x, float y) {
        this.x = x;
        this.y = y;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T size(float width, float height) {
        this.width = width;
        this.height = height;
        return (T) this;
    }

    /**
     * Queue this draw call onto a PIDS script context, allowing the fluent style
     * {@code Text.create("name").text("...").draw(ctx)}.
     */
    @SuppressWarnings("unchecked")
    public T draw(Object ctx) {
        if (ctx instanceof PIDSScriptContext) {
            ((PIDSScriptContext) ctx).draw(this);
        }
        return (T) this;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /** Reject calls that would not draw anything. */
    public boolean validate() {
        return width > 0 && height > 0;
    }

    public abstract void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, Direction facing, int light);

    protected static int resolveLight(boolean naturalLight, int light) {
        return naturalLight ? light : IGui.MAX_LIGHT_GLOWING;
    }

    /**
     * Parse a colour given either as a number or as a hex string ({@code "#RRGGBB"} / {@code "#AARRGGBB"}).
     * @author LX86 (JCM)
     */
    public static int parseColor(Object obj, int defaultColor) {
        if (obj == null) return defaultColor;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj == org.mozilla.javascript.Undefined.instance) return defaultColor;

        String str = String.valueOf(obj).trim();
        if (str.startsWith("#")) str = str.substring(1);
        try {
            final long value = Long.parseLong(str, 16);
            return str.length() <= 6 ? (int) (0xFF000000L | value) : (int) value;
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }
}
