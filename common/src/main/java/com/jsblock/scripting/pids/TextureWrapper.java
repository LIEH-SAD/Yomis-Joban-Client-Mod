package com.jsblock.scripting.pids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.render.MoreRenderLayers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * A texture drawn by a PIDS script.
 * @author LX86 (JCM)
 */
public class TextureWrapper extends PIDSDrawCall<TextureWrapper> {
    private ResourceLocation texture;
    private int color = IGui.ARGB_WHITE;
    private boolean naturalLight = false;
    private float u1 = 0;
    private float v1 = 0;
    private float u2 = 1;
    private float v2 = 1;

    public TextureWrapper() {
        this.width = 20;
        this.height = 20;
    }

    public static TextureWrapper create() {
        return new TextureWrapper();
    }

    /** {@code debugName} is accepted for compatibility with JCM scripts and is currently unused. */
    public static TextureWrapper create(String debugName) {
        return new TextureWrapper();
    }

    public TextureWrapper texture(Object texture) {
        this.texture = toTexture(texture);
        return this;
    }

    public TextureWrapper color(Object color) {
        this.color = opaque(parseColor(color, IGui.ARGB_WHITE));
        return this;
    }

    public TextureWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    public TextureWrapper uv(float u2, float v2) {
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    public TextureWrapper uv(float u1, float v1, float u2, float v2) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    /**
     * Select the render layer. Only {@code EXTERIOR} layers take the world light level;
     * the others are drawn fully lit.
     */
    public TextureWrapper renderType(String renderType) {
        this.naturalLight = naturalLight || (renderType != null && renderType.toUpperCase(java.util.Locale.ROOT).startsWith("EXTERIOR"));
        return this;
    }

    @Override
    public TextureWrapper zOrder(int order) {
        super.zOrder(order);
        return this;
    }

    @Override
    public TextureWrapper matrices(com.jsblock.scripting.api.Matrices matrices) {
        super.matrices(matrices);
        return this;
    }

    @Override
    public boolean validate() {
        return super.validate() && texture != null;
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, Direction facing, int light) {
        /* The alpha switch has to be on: a script texture is routinely mostly transparent, and
           through the opaque layer that transparent area comes out as a solid white square. */
        final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(MoreRenderLayers.getLight(texture, true));
        IDrawing.drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, resolveLight(naturalLight, light));
    }

    static ResourceLocation toTexture(Object obj) {
        if (obj == null || obj == com.jsblock.lib.org.mozilla.javascript.Undefined.instance) return null;
        /* Packs name their files with capitals and other characters a ResourceLocation rejects, so the
           lookup goes through the same lenient resolution the rest of the script API uses. */
        return com.jsblock.scripting.core.util.ScriptResourceUtil.resolveExisting(String.valueOf(obj));
    }
}
