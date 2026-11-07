package com.jsblock.screen;

import com.jsblock.client.JobanCustomResources;
import com.jsblock.packet.PacketClient;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.screen.PIDSConfigScreen;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PIDS Projector configuration: the preset it draws, the platforms it draws for, and where it draws
 * them.
 *
 * <p>A projector has no companion block, so it cannot use the mod's usual two-block PIDS screen;
 * this one carries the projector's own placement fields (offset, rotation, scale) next to the parts
 * of the ordinary PIDS screen it can use -- the preset, the platform filter, the custom messages
 * and the hidden rows. Everything is sent to the server when the screen closes, like the other
 * configuration screens in the mod.</p>
 */
public class PIDSProjectorScreen extends ScreenMapper implements IGui {

	private final BlockPos pos;
	private final WidgetSuggestionTextField textBoxPreset;
	/** offset x/y/z, then rotation x/y/z, then scale. */
	private final WidgetBetterTextField[] fields = new WidgetBetterTextField[7];
	private final WidgetBetterTextField[] textFieldMessages;
	private final WidgetBetterCheckbox[] buttonsRowHidden;
	private final WidgetBetterCheckbox hidePlatformCheckbox;
	private final boolean[] initialRowHidden;
	private final String[] initialMessages;
	private final int maxArrivals;
	private final Component messageText = Text.translatable("gui.mtr.pids_message");
	private final Component rowHiddenText = Text.translatable("gui.jsblock.pids.listview.widget.row_hidden");
	private final Component hidePlatformText = Text.translatable("gui.jsblock.hide_platform_number");

	/** MTR's own pair for picking platforms, reused so this screen filters exactly like the others. */
	private final WidgetBetterCheckbox selectAllCheckbox;
	private final Button filterButton;
	private final Set<Long> filterPlatformIds = new HashSet<>();
	/** The count last written on the filter button, so it is only rebuilt when it actually changes. */
	private int lastFilteredCount = -1;

	private final String initialPresetID;
	private final double[] initialValues;

	private static final double[] DEFAULTS = {0, 0, 0, 0, 0, 0, 1};
	private static final int MESSAGE_MAX_LENGTH = 100;
	private static final int COLUMN_WIDTH = 58;
	private static final int COLUMN_GAP = 4;
	private static final int WIDE_FIELD_WIDTH = COLUMN_WIDTH * 3 + COLUMN_GAP * 2;
	private static final int TEXT_PADDING = 16;
	private static final int FINAL_TEXT_HEIGHT = TEXT_HEIGHT + TEXT_PADDING;
	/** The row the message label is drawn on; a field's row, since the fields start there. */
	private int messageLabelRow = -1;

	public PIDSProjectorScreen(BlockPos pos, String presetID, String[] messages, boolean[] rowHidden,
							   boolean hidePlatformNumber, Set<Long> platformIds,
							   double offsetX, double offsetY, double offsetZ,
							   double rotateX, double rotateY, double rotateZ, double scale) {
		super(Text.literal(""));
		this.pos = pos;

		this.maxArrivals = messages == null || messages.length == 0 ? 4 : messages.length;
		this.initialMessages = new String[this.maxArrivals];
		this.initialRowHidden = new boolean[this.maxArrivals];
		this.textFieldMessages = new WidgetBetterTextField[this.maxArrivals];
		this.buttonsRowHidden = new WidgetBetterCheckbox[this.maxArrivals];
		for (int i = 0; i < this.maxArrivals; i++) {
			this.initialMessages[i] = messages != null && i < messages.length && messages[i] != null ? messages[i] : "";
			this.initialRowHidden[i] = rowHidden != null && i < rowHidden.length && rowHidden[i];
			this.textFieldMessages[i] = new WidgetBetterTextField("", MESSAGE_MAX_LENGTH);
			this.buttonsRowHidden[i] = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, rowHiddenText, checked -> {
			});
		}

		this.hidePlatformCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, hidePlatformText, checked -> {
		});
		this.hidePlatformCheckbox.setChecked(hidePlatformNumber);

		final List<String> presetIds = new ArrayList<>(JobanCustomResources.getPresetIds());
		presetIds.sort(String::compareTo);
		this.initialPresetID = presetID == null ? "" : presetID;
		this.textBoxPreset = new WidgetSuggestionTextField(initialPresetID, presetIds, 128, false);

		this.initialValues = new double[] {offsetX, offsetY, offsetZ, rotateX, rotateY, rotateZ, scale};
		for (int i = 0; i < fields.length; i++) {
			/* The value is passed as the widget's default: setValue alone does not make the text
			   appear, and the boxes would stay blank until clicked. */
			fields[i] = new WidgetBetterTextField(format(initialValues[i]), 16);
		}

		if (platformIds != null) {
			filterPlatformIds.addAll(platformIds);
		}
		this.selectAllCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE,
				Text.translatable("gui.mtr.automatically_detect_nearby_platform"), checked -> {
		});
		this.selectAllCheckbox.setChecked(filterPlatformIds.isEmpty());
		this.filterButton = PIDSConfigScreen.getPlatformFilterButton(pos, selectAllCheckbox, filterPlatformIds, this);
	}

	private static String format(double value) {
		return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

	private static double parse(WidgetBetterTextField field, double fallback) {
		try {
			return Double.parseDouble(field.getValue().trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	/** The left edge of column {@code index} of a three-column row, counted from the right. */
	private int columnX(int index) {
		return width - SQUARE_SIZE - WIDE_FIELD_WIDTH + index * (COLUMN_WIDTH + COLUMN_GAP);
	}

	@Override
	protected void init() {
		super.init();
		int row = 1;

		/* Values first, then position and add: setting them here rather than in the constructor is
		   what makes the text appear without the box having to be clicked. */
		textBoxPreset.setValue(initialPresetID);
		for (int i = 0; i < fields.length; i++) {
			fields[i].setValue(format(initialValues[i]));
		}

		IDrawing.setPositionAndWidth(textBoxPreset, width - SQUARE_SIZE - WIDE_FIELD_WIDTH,
				FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, WIDE_FIELD_WIDTH);
		addDrawableChild(textBoxPreset);
		row++;

		/* Offset and rotation are three values each, so they get three columns of their own; the
		   scale is the seventh field and starts a row of its own. */
		for (int i = 0; i < fields.length; i++) {
			final int column = i % 3;
			IDrawing.setPositionAndWidth(fields[i], columnX(column), FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, COLUMN_WIDTH);
			addDrawableChild(fields[i]);
			if (column == 2) {
				row++;
			}
		}

		row += 2;
		IDrawing.setPositionAndWidth(selectAllCheckbox, SQUARE_SIZE, FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, 220);
		addDrawableChild(selectAllCheckbox);
		row++;
		IDrawing.setPositionAndWidth(filterButton, columnX(0), FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, WIDE_FIELD_WIDTH);
		addDrawableChild(filterButton);

		row++;
		IDrawing.setPositionAndWidth(hidePlatformCheckbox, SQUARE_SIZE,
				FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, WIDE_FIELD_WIDTH);
		addDrawableChild(hidePlatformCheckbox);
		row++;

		messageLabelRow = row;
		final int checkboxWidth = font.width(rowHiddenText) + SQUARE_SIZE + TEXT_PADDING;
		for (int i = 0; i < maxArrivals; i++) {
			IDrawing.setPositionAndWidth(textFieldMessages[i], SQUARE_SIZE,
					FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, width - SQUARE_SIZE * 2 - checkboxWidth);
			textFieldMessages[i].setValue(initialMessages[i]);
			addDrawableChild(textFieldMessages[i]);

			IDrawing.setPositionAndWidth(buttonsRowHidden[i], width - SQUARE_SIZE - checkboxWidth + TEXT_PADDING,
					FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, checkboxWidth);
			buttonsRowHidden[i].setChecked(initialRowHidden[i]);
			addDrawableChild(buttonsRowHidden[i]);
			row++;
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(guiGraphics);
			guiGraphics.drawCenteredString(font, Text.translatable("gui.jsblock.pids_projector"), width / 2, TEXT_PADDING, ARGB_WHITE);

			int row = 1;
			drawRowLabel(guiGraphics, "preset", row++);
			drawRowLabel(guiGraphics, "offset", row++);
			drawRowLabel(guiGraphics, "rotate", row++);
			drawRowLabel(guiGraphics, "scale", row);

			if (messageLabelRow > 0) {
				guiGraphics.drawString(font, messageText, SQUARE_SIZE,
						FINAL_TEXT_HEIGHT * messageLabelRow + SQUARE_SIZE, ARGB_WHITE);
			}

			final int filteredCount = selectAllCheckbox.selected() ? 0 : filterPlatformIds.size();
			if (filteredCount != lastFilteredCount) {
				lastFilteredCount = filteredCount;
				filterButton.setMessage(Text.translatable("gui.mtr.filtered_platforms", filteredCount));
			}

			super.render(guiGraphics, mouseX, mouseY, delta);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void drawRowLabel(GuiGraphics guiGraphics, String key, int row) {
		/* The same offset init() gives the fields, so a label sits on its own field's line. */
		guiGraphics.drawString(font, Text.translatable("gui.jsblock.pids_projector." + key),
				SQUARE_SIZE, FINAL_TEXT_HEIGHT * row + SQUARE_SIZE, ARGB_WHITE);
	}

	@Override
	public void onClose() {
		final double[] values = new double[fields.length];
		for (int i = 0; i < fields.length; i++) {
			values[i] = parse(fields[i], DEFAULTS[i]);
		}
		if (values[6] <= 0) {
			values[6] = DEFAULTS[6];
		}
		if (selectAllCheckbox.selected()) {
			filterPlatformIds.clear();
		}
		final String[] messages = new String[maxArrivals];
		final boolean[] rowHidden = new boolean[maxArrivals];
		for (int i = 0; i < maxArrivals; i++) {
			messages[i] = textFieldMessages[i].getValue();
			rowHidden[i] = buttonsRowHidden[i].selected();
		}
		PacketClient.sendPIDSProjectorC2S(pos, textBoxPreset.getValue(), messages, rowHidden,
				hidePlatformCheckbox.selected(), filterPlatformIds,
				values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
