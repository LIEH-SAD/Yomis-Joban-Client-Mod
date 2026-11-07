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
 * A solid colour rectangle drawn by a PIDS script.
 * @author LX86 (JCM)
 */
public class RectangleWrapper extends PIDSDrawCall<RectangleWrapper> {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("mtr", "textures/block/white.png");

    private int color = IGui.ARGB_WHITE;
    private boolean naturalLight = false;

    public RectangleWrapper() {
        this.width = 20;
        this.height = 20;
    }

    public static RectangleWrapper create() {
        return new RectangleWrapper();
    }

    /** {@code debugName} is accepted for compatibility with JCM scripts and is currently unused. */
    public static RectangleWrapper create(String debugName) {
        return new RectangleWrapper();
    }

    public RectangleWrapper color(Object color) {
        this.color = opaque(parseColor(color, IGui.ARGB_WHITE));
        return this;
    }

    public RectangleWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    /**
     * Select the render layer. Only {@code EXTERIOR} layers take the world light level;
     * the others are drawn fully lit.
     */
    public RectangleWrapper renderType(String renderType) {
        this.naturalLight = naturalLight || (renderType != null && renderType.toUpperCase(java.util.Locale.ROOT).startsWith("EXTERIOR"));
        return this;
    }

    @Override
    public RectangleWrapper zOrder(int order) {
        super.zOrder(order);
        return this;
    }

    @Override
    public RectangleWrapper matrices(com.jsblock.scripting.api.Matrices matrices) {
        super.matrices(matrices);
        return this;
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, Direction facing, int light) {
        final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(MoreRenderLayers.getLight(WHITE_TEXTURE, false));
        IDrawing.drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, color, resolveLight(naturalLight, light));
    }
}
