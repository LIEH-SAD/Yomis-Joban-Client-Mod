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
    /* JCM 2.x's overflow modes; which one a script asked for decides whether its box
       constrains the run at all. */
    public static final int OVERFLOW_NONE = 0;
    public static final int OVERFLOW_STRETCH_XY = 1;
    public static final int OVERFLOW_SCALE_XY = 2;
    public static final int OVERFLOW_WRAP = 3;
    public static final int OVERFLOW_MARQUEE = 4;

    private String text = "";
    private int color = IGui.ARGB_WHITE;
    private boolean shadowed = false;
    private boolean naturalLight = false;
    private float scale = 1;
    private int overflowMode = OVERFLOW_NONE;
    private boolean keepRatio = true;
    private boolean wrapText = false;
    private boolean marquee = false;
    private double marqueeDurationTicks = -1;
    private double marqueeProgressOverride = -1;
    private IGui.HorizontalAlignment alignment = IGui.HorizontalAlignment.LEFT;
    private String font = "mtr:mtr";
    private String renderType = "LIGHT";
    private boolean bold = false;
    private boolean italic = false;
    private float lineHeightFactor = 1;

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
        if (font == null || font.isEmpty()) {
            this.font = "mtr:mtr";
        } else {
            /* The name ends up in a ResourceLocation at render time, so fold exactly once here and a
               pack that writes "MTR:MTR" keeps drawing text instead of throwing every frame. */
            this.font = com.jsblock.scripting.core.util.ScriptResourceUtil.lenientLocation(font).toString();
        }
        return this;
    }

    /**
     * {@code font(Object)} — JCM 2.x lets a script hand in the AWT font that
     * {@code Resources.getSystemFont(...)} returns.<br>
     * World-space text here is drawn with the game's glyph renderer, which takes a font
     * {@link ResourceLocation} rather than an AWT instance, so an AWT font keeps the default MTR font
     * instead of breaking the label.
     */
    public TextWrapper font(Object font) {
        if (font instanceof CharSequence) return font(font.toString());
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
        this.overflowMode = OVERFLOW_STRETCH_XY;
        return this;
    }

    public TextWrapper keepRatio() {
        this.keepRatio = true;
        return this;
    }

    /** Keep the aspect ratio while scaling the text down so it fits the box. */
    public TextWrapper scaleXY() {
        this.keepRatio = true;
        this.overflowMode = OVERFLOW_SCALE_XY;
        return this;
    }

    /** Split the text into as many lines as needed to fit the box width. */
    public TextWrapper wrapText() {
        this.wrapText = true;
        this.overflowMode = OVERFLOW_WRAP;
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
        this.overflowMode = OVERFLOW_MARQUEE;
        return this;
    }

    /** Freeze the marquee at a fixed point of its cycle, between 0 and 1. */
    public TextWrapper withMarqueeProgress(double progressOverride) {
        this.marqueeProgressOverride = progressOverride;
        return this;
    }

    /** Draw bold text. */
    public TextWrapper bold() {
        this.bold = true;
        return this;
    }

    /** Draw italic text. */
    public TextWrapper italic() {
        this.italic = true;
        return this;
    }

    /** @param lineHeight the line height multiplier, {@code 1} being the default */
    public TextWrapper lineHeight(double lineHeight) {
        this.lineHeightFactor = lineHeight <= 0 ? 1 : (float) lineHeight;
        return this;
    }

    /**
     * Select the render layer, as in JCM 2.3 ({@code EXTERIOR}, {@code EXTERIOR_TRANSLUCENT},
     * {@code INTERIOR}, {@code INTERIOR_TRANSLUCENT}, {@code LIGHT} or {@code LIGHT_TRANSLUCENT}).
     * Only {@code EXTERIOR} layers take the world light level; the others are drawn fully lit.
     */
    public TextWrapper renderType(String renderType) {
        this.renderType = renderType == null ? "LIGHT" : renderType.toUpperCase(java.util.Locale.ROOT);
        return this;
    }

    @Override
    public TextWrapper zOrder(int order) {
        super.zOrder(order);
        return this;
    }

    @Override
    public TextWrapper matrices(com.jsblock.scripting.api.Matrices matrices) {
        super.matrices(matrices);
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
        final int renderLight = resolveTextLight(light);
        final String renderText = wrapText ? wrapToWidth(fontRenderer, text) : text;

        if (marquee && measureWidth(fontRenderer, renderText) > width / scale) {
            renderMarquee(matrices, immediate, fontRenderer, renderText, renderLight);
            immediate.endBatch();
            return;
        }

        /* drawStringWithFont divides the pose by its scale argument, so 1/textScale is what
           makes .scale(k) multiply the glyph size -- which is how a preset author writes it. */
        final float drawScale = scale == 0 ? 1F : 1F / scale;

        /* The box only constrains a run that asked for an overflow mode. Without one it is
           just where the label sits: MTR would otherwise squeeze the glyphs to fit it, which
           is not what size(...) means to a preset author. */
        final float maxWidth;
        final float maxHeight;
        switch (overflowMode) {
            case OVERFLOW_STRETCH_XY:
            case OVERFLOW_SCALE_XY:
                maxWidth = width;
                maxHeight = height;
                break;
            case OVERFLOW_WRAP:
            case OVERFLOW_MARQUEE:
                maxWidth = width;
                maxHeight = -1F;
                break;
            default:
                maxWidth = -1F;
                maxHeight = -1F;
                break;
        }

        /* sameSize = true, i.e. no CJK doubling: a JCM 2.x preset is laid out for MTR 4's
           renderer, which drew its text without the extra multiplier. Leaving it on made every
           Chinese label twice the size its author wrote, straight off the panel. */
        IDrawingJoban.drawStringWithFontStyled(matrices, fontRenderer, immediate, renderText, alignment, IGui.VerticalAlignment.TOP, alignment, x, y, maxWidth, maxHeight, drawScale, keepRatio, color, shadowed, renderLight, font, true, null, bold, italic, lineHeightFactor);

        immediate.endBatch();
    }

    /** {@code EXTERIOR} layers take the world light, everything else is drawn fully lit. */
    private int resolveTextLight(int light) {
        final boolean exterior = naturalLight || renderType.startsWith("EXTERIOR");
        return exterior ? light : IGui.MAX_LIGHT_GLOWING;
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
                matrices.pushPose();
                matrices.translate(x + pixelOffset * scale, y, 0);
                matrices.scale(scale, scale, scale);
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
        final Style style = Config.useMTRFont() ? Style.EMPTY.withFont(new ResourceLocation(font)) : Style.EMPTY;
        return style.withBold(bold).withItalic(italic);
    }

    private static int charWidth(Font fontRenderer, Style style, String str) {
        /* No CJK doubling: the run is drawn with sameSize = true, so the widths that decide
           wrapping and measuring have to be the widths the renderer will draw. */
        return fontRenderer.width(Text.literal(str).setStyle(style).getVisualOrderText());
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
