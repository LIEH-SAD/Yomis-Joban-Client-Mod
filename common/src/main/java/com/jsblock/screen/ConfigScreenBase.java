package com.jsblock.screen;

import com.jsblock.vermappings.render.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The base configuration screen, styled after JCM 2.3's {@code ClientConfigScreen}.<br>
 * It renders an animated background together with a scrollable list of config entries,
 * each entry optionally having a widget aligned to the right hand side.
 * @author LX86
 * @since 1.2.12
 */
public class ConfigScreenBase extends ScreenMapper implements IGui {
	private static final ResourceLocation BACKGROUND = new ResourceLocation("jsblock:textures/gui/background/bg.png");
	private static final ResourceLocation STAR_BACKGROUND = new ResourceLocation("jsblock:textures/gui/background/stars.png");
	private static final ResourceLocation TERRAIN_BACKGROUND = new ResourceLocation("jsblock:textures/gui/background/terrain.png");

	protected static final int TEXT_PADDING = 10;
	protected static final int TITLE_SCALE = 2;
	protected static final int BUTTON_HEIGHT = TEXT_HEIGHT + 12;
	private static final int ROW_HEIGHT = 22;
	private static final int ENTRY_PADDING = 5;
	private static final int FOOTER_MARGIN = 10;
	private static final int FOOTER_GAP = 10;
	private static final int MAX_CONTENT_WIDTH = 400;
	private static final int SCROLLBAR_WIDTH = 3;
	private static final double ANIMATION_LENGTH_SECONDS = 0.3;
	private static final float STAR_ROTATION_LENGTH_MS = 260 * 1000F;

	private final Component title;
	private final Component subtitle;
	private final List<ListItem> items = new ArrayList<>();
	private final List<List<AbstractWidget>> footerRows = new ArrayList<>();

	private double linearAnimationProgress;
	private double animationProgress;
	private boolean closing;
	private long lastFrameTime = -1;

	private int listX;
	private int listY;
	private int listWidth;
	private int listHeight;
	private double scroll;

	public ConfigScreenBase(Component title, Component subtitle) {
		super(title);
		this.title = title;
		this.subtitle = subtitle;
	}

	/**
	 * Add a category header to the entry list.
	 */
	protected void addCategory(Component text) {
		items.add(new ListItem(text, null));
	}

	/**
	 * Add a config entry, with its widget aligned to the right of the list.
	 * @return The same widget, for convenience
	 */
	protected AbstractWidget addEntry(Component text, AbstractWidget widget) {
		items.add(new ListItem(text, widget));
		return widget;
	}

	/**
	 * Add a row of widgets at the bottom of the screen.
	 */
	protected void addFooterRow(AbstractWidget... widgets) {
		footerRows.add(new ArrayList<>(Arrays.asList(widgets)));
	}

	/**
	 * Called when the screen is closed via the save button or by pressing escape.
	 */
	public void onSave() {
	}

	protected void closeScreen(boolean save) {
		if(save) {
			onSave();
		}

		/* Play the closing animation, which will close the screen once it finishes */
		if(linearAnimationProgress <= 0) {
			super.onClose();
		} else {
			closing = true;
		}
	}

	@Override
	protected void init() {
		super.init();

		final int contentWidth = Math.min((int) (width * 0.75), MAX_CONTENT_WIDTH);
		listX = (width - contentWidth) / 2;
		listY = TEXT_PADDING * 5;
		listWidth = contentWidth;
		listHeight = Math.max(ROW_HEIGHT, (int) ((height - 60) * 0.75));

		final int footerTop = listY + listHeight + FOOTER_MARGIN;

		if(!footerRows.isEmpty()) {
			final int availableHeight = Math.max(0, height - footerTop - FOOTER_MARGIN);
			final int rowHeight = Math.min(20, availableHeight / footerRows.size());

			for (int i = 0; i < footerRows.size(); i++) {
				final List<AbstractWidget> row = footerRows.get(i);

				if(row.isEmpty()) {
					continue;
				}

				final double widgetWidth = ((double) contentWidth - FOOTER_GAP * (row.size() - 1)) / row.size();

				for (int j = 0; j < row.size(); j++) {
					final AbstractWidget widget = row.get(j);
					final int widgetX = listX + (int) Math.round(j * (widgetWidth + FOOTER_GAP));
					IDrawing.setPositionAndWidth(widget, widgetX, footerTop + i * rowHeight, (int) Math.round(widgetWidth));
				}
			}
		}

		for (ListItem item : items) {
			if(item.widget != null) {
				addWidget(item.widget);
			}
		}

		scroll = Math.max(0, Math.min(getMaxScroll(), scroll));
		positionEntryWidgets();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		final long now = Util.getMillis();

		if(lastFrameTime < 0) {
			lastFrameTime = now;
		}

		final double frameDelta = Math.min(0.1, (now - lastFrameTime) / 1000.0);
		lastFrameTime = now;
		tickAnimation(frameDelta);

		renderBackground(guiGraphics);
		renderTitle(guiGraphics);
		renderList(guiGraphics, mouseX, mouseY, delta);
		super.render(guiGraphics, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics) {
		final PoseStack matrices = guiGraphics.pose();
		final Tesselator tesselator = Tesselator.getInstance();
		final BufferBuilder bufferBuilder = tesselator.getBuilder();
		final double translateY = height * (1 - animationProgress);

		/* Background */
		renderTexture(matrices, bufferBuilder, BACKGROUND, width, height, 1, 1);
		tesselator.end();

		/* Stars */
		final int starBackgroundSize = Math.max(width, height) * 4;
		final float starUVSize = starBackgroundSize / 384F;

		matrices.pushPose();
		matrices.translate(0, translateY * 0.2, 0);
		matrices.translate(width / 2.0, height / 2.0, 0);
		final float starRotation = (System.currentTimeMillis() % (long) STAR_ROTATION_LENGTH_MS) / STAR_ROTATION_LENGTH_MS;
		matrices.mulPose(new Quaternionf().rotationZ(starRotation * (float) (Math.PI * 2)));
		matrices.translate(-width / 2.0, -height / 2.0, 0);
		renderTexture(matrices, bufferBuilder, STAR_BACKGROUND, starBackgroundSize, starBackgroundSize, starUVSize, starUVSize);
		matrices.popPose();
		tesselator.end();

		/* Terrain */
		final int terrainHeight = (int) (width / 3.75);
		matrices.pushPose();
		matrices.translate(0, translateY + height - terrainHeight, 0);
		renderTexture(matrices, bufferBuilder, TERRAIN_BACKGROUND, width, terrainHeight, 1, 1);
		matrices.popPose();
		tesselator.end();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if(mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
			scroll = Math.max(0, Math.min(getMaxScroll(), scroll - amount * ROW_HEIGHT));
			positionEntryWidgets();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public void onClose() {
		/* Delay the actual close so that the reverse animation can be played */
		closing = true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == 256) {
			closeScreen(true);
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	protected static Component getBooleanButtonText(boolean state) {
		return mtr.mappings.Text.translatable(state ? "options.mtr.on" : "options.mtr.off");
	}

	private int getContentHeight() {
		return items.size() * ROW_HEIGHT;
	}

	private double getMaxScroll() {
		return Math.max(0, getContentHeight() - listHeight);
	}

	private void positionEntryWidgets() {
		int y = listY - (int) scroll;

		for (ListItem item : items) {
			if(item.widget != null) {
				final int widgetY = y + (ROW_HEIGHT - item.widget.getHeight()) / 2;
				item.widget.setX(listX + listWidth - ENTRY_PADDING - item.widget.getWidth());
				item.widget.setY(widgetY);
				item.widget.visible = widgetY + item.widget.getHeight() > listY && widgetY < listY + listHeight;
			}

			y += ROW_HEIGHT;
		}
	}

	private void renderTitle(GuiGraphics guiGraphics) {
		final int titleHeight = TEXT_HEIGHT * TITLE_SCALE;
		final PoseStack matrices = guiGraphics.pose();

		matrices.pushPose();
		matrices.translate(width / 2.0, TEXT_PADDING - (titleHeight + TEXT_PADDING) * (1 - animationProgress), 0);
		matrices.scale(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE);
		guiGraphics.drawCenteredString(font, title, 0, 0, ARGB_WHITE);
		matrices.popPose();

		matrices.pushPose();
		matrices.translate(width / 2.0, TEXT_PADDING - (titleHeight + TEXT_PADDING) * (1 - animationProgress) + titleHeight + TEXT_PADDING / 2F, 0);
		guiGraphics.drawCenteredString(font, subtitle, 0, 0, ARGB_WHITE);
		matrices.popPose();
	}

	private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		positionEntryWidgets();

		guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x4F4C4C4C);
		guiGraphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

		int y = listY - (int) scroll;

		for (ListItem item : items) {
			final boolean inSight = y + ROW_HEIGHT > listY && y < listY + listHeight;

			if(inSight) {
				if(item.widget == null) {
					guiGraphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, 0x99999999);
					guiGraphics.drawCenteredString(font, item.text, listX + listWidth / 2, y + (ROW_HEIGHT - TEXT_HEIGHT) / 2, ARGB_WHITE);
				} else {
					if(mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT) {
						guiGraphics.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, 0x33FFFFFF);
					}

					final int availableWidth = Math.max(0, listWidth - ENTRY_PADDING * 3 - item.widget.getWidth());
					final String text = font.plainSubstrByWidth(item.text.getString(), availableWidth);
					guiGraphics.drawString(font, text, listX + ENTRY_PADDING, y + (ROW_HEIGHT - TEXT_HEIGHT) / 2, ARGB_WHITE, false);
				}
			}

			y += ROW_HEIGHT;
		}

		for (ListItem item : items) {
			if(item.widget != null && item.widget.visible) {
				item.widget.render(guiGraphics, mouseX, mouseY, delta);
			}
		}

		guiGraphics.disableScissor();

		final double maxScroll = getMaxScroll();

		if(maxScroll > 0) {
			final int trackX = listX + listWidth - SCROLLBAR_WIDTH;
			final int scrollbarHeight = Math.max(12, (int) (listHeight * (listHeight / (double) getContentHeight())));
			final int scrollbarY = listY + (int) ((listHeight - scrollbarHeight) * (scroll / maxScroll));
			guiGraphics.fill(trackX, listY, trackX + SCROLLBAR_WIDTH, listY + listHeight, 0x33FFFFFF);
			guiGraphics.fill(trackX, scrollbarY, trackX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFFFFFFFF);
		}
	}

	private void tickAnimation(double frameDelta) {
		final double step = frameDelta / ANIMATION_LENGTH_SECONDS;

		if(closing) {
			linearAnimationProgress = Math.max(0, linearAnimationProgress - step);

			if(linearAnimationProgress <= 0) {
				super.onClose();
				return;
			}
		} else {
			linearAnimationProgress = Math.min(1, linearAnimationProgress + step);
		}

		animationProgress = 1 - Math.pow(1 - linearAnimationProgress, 5);
	}

	private void renderTexture(PoseStack matrices, BufferBuilder bufferBuilder, ResourceLocation identifier, float width, float height, float u, float v) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderHelper.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderHelper.setTexture(minecraft, identifier);

		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

		final PoseStack.Pose pose = matrices.last();

		bufferBuilder.vertex(pose.pose(), 0, height, 0).uv(0, v).color(255, 255, 255, 255).endVertex();
		bufferBuilder.vertex(pose.pose(), width, height, 0).uv(u, v).color(255, 255, 255, 255).endVertex();
		bufferBuilder.vertex(pose.pose(), width, 0, 0).uv(u, 0).color(255, 255, 255, 255).endVertex();
		bufferBuilder.vertex(pose.pose(), 0, 0, 0).uv(0, 0).color(255, 255, 255, 255).endVertex();
	}

	private static class ListItem {
		private final Component text;
		private final AbstractWidget widget;

		private ListItem(Component text, AbstractWidget widget) {
			this.text = text;
			this.widget = widget;
		}
	}
}
