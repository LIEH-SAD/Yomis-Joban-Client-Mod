package com.jsblock.scripting.pids;

import com.jsblock.screen.IDrawingJoban;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.Config;
import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

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
    private boolean wrapText = false;
    private boolean marquee = false;
    private double marqueeDurationTicks = -1;
    private double marqueeProgressOverride = -1;
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

    /** Drop shadow, as in JCM 2.3 ({@code shadowed()} without arguments). */
    public TextWrapper shadowed() {
        this.shadowed = true;
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

    /** Keep the aspect ratio while scaling the text down so it fits the box. */
    public TextWrapper scaleXY() {
        this.keepRatio = true;
        return this;
    }

    /** Split the text into as many lines as needed to fit the box width. */
    public TextWrapper wrapText() {
        this.wrapText = true;
        return this;
    }

    /** Scroll the text horizontally when it is wider than the box. */
    public TextWrapper marquee() {
        return marquee(-1);
    }

    /**
     * Scroll the text horizontally when it is wider than the box.
     * @param durationTickOverride how long a complete scroll cycle takes, in ticks; {@code -1} derives it from the text length
     */
    public TextWrapper marquee(double durationTickOverride) {
        this.marquee = true;
        this.marqueeDurationTicks = durationTickOverride;
        return this;
    }

    /** Freeze the marquee at a fixed point of its cycle, between 0 and 1. */
    public TextWrapper withMarqueeProgress(double progressOverride) {
        this.marqueeProgressOverride = progressOverride;
        return this;
    }

    /** @return the width of the text in pixels, as measured with the currently selected font */
    public int measureWidth() {
        return measureWidth(Minecraft.getInstance().font, text);
    }

    @Override
    public boolean validate() {
        return super.validate() && text != null && !text.isEmpty();
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font fontRenderer, Direction facing, int light) {
        final int renderLight = resolveLight(naturalLight, light);
        final String renderText = wrapText ? wrapToWidth(fontRenderer, text) : text;

        if (marquee && measureWidth(fontRenderer, renderText) > width / scale) {
            renderMarquee(matrices, immediate, fontRenderer, renderText, renderLight);
        } else {
            IDrawingJoban.drawStringWithFont(matrices, fontRenderer, immediate, renderText, alignment, IGui.VerticalAlignment.TOP, x, y, width, height, scale, keepRatio, color, shadowed, renderLight, font);
        }

        immediate.endBatch();
    }

    private void renderMarquee(PoseStack matrices, MultiBufferSource.BufferSource immediate, Font fontRenderer, String str, int light) {
        final Style style = fontStyle();
        final int[] charWidths = new int[str.length()];
        int totalWidth = 0;

        for (int i = 0; i < str.length(); i++) {
            charWidths[i] = charWidth(fontRenderer, style, str.substring(i, i + 1));
            totalWidth += charWidths[i];
        }

        final double boxWidth = width / scale;
        final double fullWidth = totalWidth + boxWidth;
        final double cycleDuration = Math.max(marqueeDurationTicks > 0 ? marqueeDurationTicks * 20 : str.length() * 10.0, 1);
        final double progress = marqueeProgressOverride >= 0 ? marqueeProgressOverride : (System.currentTimeMillis() / 50.0 % cycleDuration) / cycleDuration;
        final int shadedColor = shadedColor(color, light);

        double pixelOffset = boxWidth - fullWidth * progress;

        for (int i = 0; i < str.length(); i++) {
            if (pixelOffset >= 0 && pixelOffset <= boxWidth) {
                final String character = str.substring(i, i + 1);
                final int extraScale = IGui.isCjk(character) ? 2 : 1;
                matrices.pushPose();
                matrices.translate(x + pixelOffset * scale, y, 0);
                matrices.scale(scale * extraScale, scale * extraScale, scale);
                UtilitiesClient.drawInBatch(fontRenderer, Text.literal(character).setStyle(style).getVisualOrderText(), 0, 0, shadedColor, shadowed, matrices.last().pose(), immediate, 0, light);
                matrices.popPose();
            }

            pixelOffset += charWidths[i];
        }
    }

    /** Split the text into lines that fit the box width, joined by the {@code |} separator MTR renders as a line break. */
    private String wrapToWidth(Font fontRenderer, String str) {
        final Style style = fontStyle();
        final double maxWidth = width / scale;
        final StringBuilder result = new StringBuilder();
        final StringBuilder currentLine = new StringBuilder();
        int widthSoFar = 0;

        for (int i = 0; i < str.length(); i++) {
            final String character = str.substring(i, i + 1);
            final int characterWidth = charWidth(fontRenderer, style, character);

            if (widthSoFar + characterWidth > maxWidth && currentLine.length() > 0) {
                result.append(currentLine).append('|');
                currentLine.setLength(0);
                widthSoFar = 0;
            }

            currentLine.append(character);
            widthSoFar += characterWidth;
        }

        return result.append(currentLine).toString();
    }

    private Style fontStyle() {
        return Config.useMTRFont() ? Style.EMPTY.withFont(new ResourceLocation(font)) : Style.EMPTY;
    }

    private static int charWidth(Font fontRenderer, Style style, String str) {
        final int width = fontRenderer.width(Text.literal(str).setStyle(style).getVisualOrderText());
        return IGui.isCjk(str) ? width * 2 : width;
    }

    private int measureWidth(Font fontRenderer, String str) {
        final Style style = fontStyle();
        int totalWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            totalWidth += charWidth(fontRenderer, style, str.substring(i, i + 1));
        }
        return totalWidth;
    }

    private static int shadedColor(int argb, int light) {
        final float shade = light == IGui.MAX_LIGHT_GLOWING ? 1 : Math.min(LightTexture.block(light) / 16F * 0.1F + 0.7F, 1);
        final int a = (argb >> 24) & 0xFF;
        final int r = (int) (((argb >> 16) & 0xFF) * shade);
        final int g = (int) (((argb >> 8) & 0xFF) * shade);
        final int b = (int) ((argb & 0xFF) * shade);
        return (a << 24) + (r << 16) + (g << 8) + b;
    }
}
