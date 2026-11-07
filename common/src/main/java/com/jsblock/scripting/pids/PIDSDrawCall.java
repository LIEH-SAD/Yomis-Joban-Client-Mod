package com.jsblock.scripting.pids;

import com.jsblock.scripting.api.Matrices;
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
    /** Explicit z-order, or {@code -1} when the draw call relies on the automatic ordering. */
    protected int zOrder = -1;
    /** Automatic z-order slot assigned by {@link PIDSScriptContext#draw(Object)}, or {@code -1}. */
    protected int autoZOrder = -1;
    protected Matrices matrices = null;

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

    /**
     * Draw this call at a fixed z-order. Calls without an explicit z-order are ordered by the
     * sequence in which they are queued, which resolves z-fighting between overlapping draw calls.
     * @param order the z-order, which must be 0 or above
     */
    @SuppressWarnings("unchecked")
    public T zOrder(int order) {
        if (order < 0) {
            throw new IllegalArgumentException("Z-Order must be 0 or above.");
        }
        this.zOrder = order;
        return (T) this;
    }

    /** Apply a transformation on top of the PIDS canvas before this call is drawn. */
    @SuppressWarnings("unchecked")
    public T matrices(Matrices matrices) {
        this.matrices = matrices;
        return (T) this;
    }

    public int getZOrder() {
        if (zOrder >= 0) return zOrder;
        return Math.max(autoZOrder, 0);
    }

    public boolean hasExplicitZOrder() {
        return zOrder >= 0;
    }

    public boolean hasAutoZOrder() {
        return autoZOrder >= 0;
    }

    public void setAutoZOrder(int order) {
        this.autoZOrder = order;
    }

    public Matrices getMatrices() {
        return matrices;
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

    /**
     * Makes a draw call's colour opaque, which is what every JCM 2.x wrapper does to it.
     *
     * <p>A preset writes a plain RGB literal -- {@code .color(0xFFFFFF)} for white,
     * {@code .color(0x009944)} for a route colour -- and JCM 2.x draws both through
     * {@code ARGB_BLACK + color}. MTR's {@code IDrawing.drawTexture} takes the alpha straight
     * from {@code color >> 24} and does not add one, so without this the quad is fully
     * transparent and simply vanishes, while the text drawn over it still shows because
     * vanilla's font renderer forces opaque on its own.</p>
     */
    protected static int opaque(int color) {
        return color | IGui.ARGB_BLACK;
    }

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
        if (obj == com.jsblock.lib.org.mozilla.javascript.Undefined.instance) return defaultColor;

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
