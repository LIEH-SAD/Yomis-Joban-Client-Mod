package com.jsblock.scripting.pids;

import com.jsblock.screen.IDrawingJoban;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.data.IGui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;

/**
 * A text label drawn by a PIDS script.
 * @author LX86 (JCM)
 */
public class TextWrapper extends PIDSDrawCall<TextWrapper> {
    private String text = "";
    private int color = IGui.ARGB_WHITE;
    private boolean shadowed = false;
    private boolean naturalLight = false;
    private float scale = 1;
    private boolean keepRatio = true;
    private IGui.HorizontalAlignment alignment = IGui.HorizontalAlignment.LEFT;
    private String font = "mtr:mtr";

    public TextWrapper() {
        this.width = 100;
        this.height = 25;
    }

    public static TextWrapper create() {
        return new TextWrapper();
    }

    /** {@code debugName} is accepted for compatibility with JCM scripts and is currently unused. */
    public static TextWrapper create(String debugName) {
        return new TextWrapper();
    }

    public TextWrapper text(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    public TextWrapper color(Object color) {
        this.color = parseColor(color, IGui.ARGB_WHITE);
        return this;
    }

    public TextWrapper shadowed(boolean shadowed) {
        this.shadowed = shadowed;
        return this;
    }

    public TextWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    public TextWrapper scale(float scale) {
        this.scale = scale <= 0 ? 1 : scale;
        return this;
    }

    public TextWrapper leftAlign() {
        this.alignment = IGui.HorizontalAlignment.LEFT;
        return this;
    }

    public TextWrapper centerAlign() {
        this.alignment = IGui.HorizontalAlignment.CENTER;
        return this;
    }

    public TextWrapper rightAlign() {
        this.alignment = IGui.HorizontalAlignment.RIGHT;
        return this;
    }

    public TextWrapper font(String font) {
        this.font = font == null || font.isEmpty() ? "mtr:mtr" : font;
        return this;
    }

    /** Use the vanilla font instead of MTR's sign font. */
    public TextWrapper fontMC() {
        this.font = "minecraft";
        return this;
    }

    public TextWrapper fontMTR() {
        this.font = "mtr:mtr";
        return this;
    }

    /** Stretch the text to fill both the width and the height of the box. */
    public TextWrapper stretchXY() {
        this.keepRatio = false;
        return this;
    }

    public TextWrapper keepRatio() {
        this.keepRatio = true;
        return this;
    }

    @Override
    public boolean validate() {
        return super.validate() && text != null && !text.isEmpty();
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font fontRenderer, Direction facing, int light) {
        IDrawingJoban.drawStringWithFont(matrices, fontRenderer, immediate, text, alignment, IGui.VerticalAlignment.TOP, x, y, width, height, scale, keepRatio, color, shadowed, resolveLight(naturalLight, light), font);
        immediate.endBatch();
    }
}
