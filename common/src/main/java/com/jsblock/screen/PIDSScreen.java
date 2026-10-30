package com.jsblock.screen;

import com.jsblock.amberui.gui.screen.BlockConfigListScreen;
import com.jsblock.amberui.gui.widget.ContentItem;
import com.jsblock.amberui.gui.widget.HorizontalWidgetSet;
import com.jsblock.amberui.gui.widget.ListViewWidget;
import com.jsblock.amberui.gui.widget.MappedWidget;
import com.jsblock.block.BlockPIDSBaseHorizontal;
import com.jsblock.block.JobanPIDSBase;
import com.jsblock.client.JobanCustomResources;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.mappings.Text;
import mtr.screen.PIDSConfigScreen;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base screen of the PIDS block config screens.<br>
 * The list layout (preset preview, filtered platform, one row per arrival, hide platform number)
 * is a direct port of JCM 2.3's {@code PIDSScreen}. This fork has two PIDS block families with
 * different update packets, so the actual saving is deferred to the subclasses.<br>
 * The auto-switch settings are exclusive to this fork and are appended at the end of the list
 * as their own categories, so the 2.3 layout is left untouched.
 */
public abstract class PIDSScreen extends BlockConfigListScreen {
	protected static final int MAX_MESSAGE_LENGTH = 100;
	protected static final int ROW_HIDDEN_WIDTH = 95;
	protected static final int ROW_WIDGET_GAP = 18;
	protected static final int AUTO_SWITCH_PRESET_WIDTH = 120;
	protected static final int AUTO_SWITCH_NUMBER_WIDTH = 52;

	protected final BlockPos pos1;
	protected final BlockPos pos2;
	protected final boolean withPlatformNumberOption;

	protected final WidgetBetterTextField[] customMessagesWidgets;
	protected final WidgetBetterCheckbox[] rowHiddenWidgets;
	protected final WidgetBetterCheckbox selectAllCheckbox;
	protected final WidgetBetterCheckbox hidePlatformNumberWidget;
	protected final Button choosePresetButton;
	protected final Button choosePlatformButton;

	protected final String[] customMessages;
	protected final boolean[] rowHidden;
	protected final Set<Long> filteredPlatforms;
	protected String presetId;

	protected final WidgetBetterCheckbox autoSwitchCheckbox;
	protected final WidgetSuggestionTextField autoSwitchPresetTextField;
	protected final WidgetBetterTextField autoSwitchCountdownTextField;
	protected final WidgetBetterTextField autoSwitchDurationTextField;
	protected final WidgetBetterCheckbox depAutoSwitchCheckbox;
	protected final WidgetSuggestionTextField depAutoSwitchPresetTextField;
	protected final WidgetBetterTextField depAutoSwitchCountdownTextField;
	protected final WidgetBetterTextField depAutoSwitchDurationTextField;
	protected final WidgetBetterCheckbox depAutoSwitchUntilCloseCheckbox;

	private final int autoSwitchCountdown;
	private final int autoSwitchDuration;
	private final int depAutoSwitchCountdown;
	private final int depAutoSwitchDuration;

	public PIDSScreen(BlockPos pos1, BlockPos pos2, int maxArrivals, boolean withPlatformNumberOption, boolean hidePlatformNumber, String presetId,
					  boolean autoSwitchEnabled, String autoSwitchPreset, int autoSwitchCountdown, int autoSwitchDuration,
					  boolean depAutoSwitchEnabled, String depAutoSwitchPreset, int depAutoSwitchCountdown, int depAutoSwitchDuration, boolean depAutoSwitchUntilClose) {
		super(pos1);
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.withPlatformNumberOption = withPlatformNumberOption;
		this.presetId = presetId;
		this.autoSwitchCountdown = autoSwitchCountdown > 0 ? autoSwitchCountdown : 10;
		this.autoSwitchDuration = autoSwitchDuration > 0 ? autoSwitchDuration : 10;
		this.depAutoSwitchCountdown = depAutoSwitchCountdown > 0 ? depAutoSwitchCountdown : 10;
		this.depAutoSwitchDuration = depAutoSwitchDuration > 0 ? depAutoSwitchDuration : 10;

		final int arrivalCount = Math.max(1, maxArrivals);
		this.customMessages = new String[arrivalCount];
		this.rowHidden = new boolean[arrivalCount];

		Set<Long> platformIds = new HashSet<>();
		final Level level = Minecraft.getInstance().level;
		final BlockEntity blockEntity = level == null ? null : level.getBlockEntity(pos1);

		if (blockEntity instanceof BlockPIDSBaseHorizontal.TileEntityBlockPIDSBaseHorizontal) {
			platformIds = ((BlockPIDSBaseHorizontal.TileEntityBlockPIDSBaseHorizontal) blockEntity).getPlatformIds();
		}

		final JobanPIDSBase.TileEntityBlockJobanPIDS pidsEntity = blockEntity instanceof JobanPIDSBase.TileEntityBlockJobanPIDS
				? (JobanPIDSBase.TileEntityBlockJobanPIDS) blockEntity
				: null;

		for (int i = 0; i < arrivalCount; i++) {
			if (pidsEntity != null && i < pidsEntity.getMaxArrivals()) {
				customMessages[i] = pidsEntity.getMessage(i);
				rowHidden[i] = pidsEntity.getHideArrival(i);
			} else {
				customMessages[i] = "";
				rowHidden[i] = false;
			}
		}

		this.filteredPlatforms = platformIds;

		this.customMessagesWidgets = new WidgetBetterTextField[arrivalCount];
		this.rowHiddenWidgets = new WidgetBetterCheckbox[arrivalCount];

		for (int i = 0; i < arrivalCount; i++) {
			final WidgetBetterTextField messageField = new WidgetBetterTextField("", MAX_MESSAGE_LENGTH);
			messageField.setHint(Text.translatable("gui.jsblock.pids.listview.widget.custom_message"));
			messageField.setValue(customMessages[i] == null ? "" : customMessages[i]);
			this.customMessagesWidgets[i] = messageField;

			final WidgetBetterCheckbox rowHiddenCheckbox = new WidgetBetterCheckbox(0, 0, ROW_HIDDEN_WIDTH, SQUARE_SIZE, Text.translatable("gui.jsblock.pids.listview.widget.row_hidden"), checked -> {
			});
			rowHiddenCheckbox.setChecked(rowHidden[i]);
			this.rowHiddenWidgets[i] = rowHiddenCheckbox;
		}

		this.hidePlatformNumberWidget = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Component.empty(), checked -> {
		});
		this.hidePlatformNumberWidget.setChecked(hidePlatformNumber);

		this.selectAllCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.mtr.automatically_detect_nearby_platform"), checked -> {
		});
		this.selectAllCheckbox.setChecked(filteredPlatforms.isEmpty());

		this.choosePresetButton = Button.builder(Text.translatable("gui.jsblock.pids.listview.widget.choose_preset"), btn -> openPresetPicker())
				.bounds(0, 0, 60, 20)
				.build();

		this.choosePlatformButton = PIDSConfigScreen.getPlatformFilterButton(pos1, selectAllCheckbox, filteredPlatforms, this);
		this.choosePlatformButton.setMessage(Text.translatable("gui.jsblock.pids.listview.widget.change_platform"));
		this.choosePlatformButton.setWidth(60);

		this.autoSwitchCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.jsblock.auto_switch_enabled"), checked -> {
		});
		this.autoSwitchCheckbox.setChecked(autoSwitchEnabled);

		this.autoSwitchPresetTextField = new WidgetSuggestionTextField("None", JobanCustomResources.PIDSPresets.keySet(), MAX_MESSAGE_LENGTH, true);
		this.autoSwitchPresetTextField.setResponder(text -> {
		});
		this.autoSwitchPresetTextField.setValue(autoSwitchPreset == null ? "" : autoSwitchPreset);

		this.autoSwitchCountdownTextField = new WidgetBetterTextField("", 4);
		this.autoSwitchCountdownTextField.setValue(String.valueOf(this.autoSwitchCountdown));
		this.autoSwitchDurationTextField = new WidgetBetterTextField("", 4);
		this.autoSwitchDurationTextField.setValue(String.valueOf(this.autoSwitchDuration));

		this.depAutoSwitchCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.jsblock.dep_auto_switch_enabled"), checked -> {
		});
		this.depAutoSwitchCheckbox.setChecked(depAutoSwitchEnabled);

		this.depAutoSwitchPresetTextField = new WidgetSuggestionTextField("None", JobanCustomResources.PIDSPresets.keySet(), MAX_MESSAGE_LENGTH, true);
		this.depAutoSwitchPresetTextField.setResponder(text -> {
		});
		this.depAutoSwitchPresetTextField.setValue(depAutoSwitchPreset == null ? "" : depAutoSwitchPreset);

		this.depAutoSwitchCountdownTextField = new WidgetBetterTextField("", 4);
		this.depAutoSwitchCountdownTextField.setValue(String.valueOf(this.depAutoSwitchCountdown));
		this.depAutoSwitchDurationTextField = new WidgetBetterTextField("", 4);
		this.depAutoSwitchDurationTextField.setValue(String.valueOf(this.depAutoSwitchDuration));
		this.depAutoSwitchDurationTextField.setEditable(!depAutoSwitchUntilClose);

		this.depAutoSwitchUntilCloseCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.jsblock.dep_auto_switch_until_close"), checked -> {
		});
		this.depAutoSwitchUntilCloseCheckbox.setChecked(depAutoSwitchUntilClose);
	}

	@Override
	public Component getScreenTitle() {
		final Level level = Minecraft.getInstance().level;

		if (level != null) {
			return Text.translatable(level.getBlockState(pos1).getBlock().getDescriptionId());
		}

		return Text.translatable("block.jsblock.pids_1a");
	}

	private void openPresetPicker() {
		Minecraft.getInstance().setScreen(new PIDSPresetScreen(pos1, presetId, preset -> this.presetId = preset).withPreviousScreen(this));
	}

	@Override
	public void addConfigEntries(ListViewWidget listViewWidget) {
		// Preset button
		addWidget(choosePresetButton);
		final ContentItem presetEntry = new ContentItem(Text.translatable("gui.jsblock.pids.listview.title.pids_preset"), new MappedWidget(choosePresetButton), 26);
		presetEntry.setIconCallback((guiGraphics, startX, startY, width, height) -> PIDSPresetScreen.drawPIDSPreview(JobanCustomResources.getPresetEntry(presetId), guiGraphics, startX, startY, width, height, true));
		listViewWidget.add(presetEntry);

		// Filter Platform button
		final List<String> platformList = new ArrayList<>();

		for (long filteredPlatform : filteredPlatforms) {
			String platformName = null;

			for (Platform platform : ClientData.PLATFORMS) {
				if (platform.id == filteredPlatform) {
					platformName = platform.name;
					break;
				}
			}

			platformList.add(platformName == null ? "?" : platformName);
		}

		final Component filteredPlatformTitle = filteredPlatforms.isEmpty()
				? Text.translatable("gui.jsblock.pids.listview.title.filtered_platform.nearby")
				: Text.translatable("gui.jsblock.pids.listview.title.filtered_platform", String.join(",", platformList));
		listViewWidget.add(filteredPlatformTitle, new MappedWidget(choosePlatformButton));
		addWidget(choosePlatformButton);

		// One row per arrival: custom message + hide arrival
		for (int i = 0; i < customMessagesWidgets.length; i++) {
			addWidget(customMessagesWidgets[i]);
			addWidget(rowHiddenWidgets[i]);

			final int rowWidth = listViewWidget.getWidth();
			rowHiddenWidgets[i].setWidth(ROW_HIDDEN_WIDTH);
			customMessagesWidgets[i].setWidth(rowWidth - rowHiddenWidgets[i].getWidth() - ROW_WIDGET_GAP);

			final HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
			widgetSet.addWidget(new MappedWidget(customMessagesWidgets[i]));
			widgetSet.addWidget(new MappedWidget(rowHiddenWidgets[i]));
			widgetSet.setXYSize(listViewWidget.getX(), 20, listViewWidget.getWidth(), 20);
			listViewWidget.add((Component) null, new MappedWidget(widgetSet));
		}

		if (withPlatformNumberOption) {
			addWidget(hidePlatformNumberWidget);
			listViewWidget.add(Text.translatable("gui.jsblock.pids.listview.title.hide_platform_number"), new MappedWidget(hidePlatformNumberWidget));
		}

		addAutoSwitchEntries(listViewWidget);
	}

	/**
	 * Fork-only auto preset switching settings, appended after the 2.3 entries.
	 */
	private void addAutoSwitchEntries(ListViewWidget listViewWidget) {
		listViewWidget.addCategory(Text.translatable("gui.jsblock.arrival_switch_section"));

		addWidget(autoSwitchCheckbox);
		listViewWidget.add(Text.translatable("gui.jsblock.auto_switch_enabled"), new MappedWidget(autoSwitchCheckbox));

		autoSwitchPresetTextField.setWidth(AUTO_SWITCH_PRESET_WIDTH);
		addWidget(autoSwitchPresetTextField);
		listViewWidget.add(Text.translatable("gui.jsblock.auto_switch_preset"), new MappedWidget(autoSwitchPresetTextField));

		autoSwitchCountdownTextField.setWidth(AUTO_SWITCH_NUMBER_WIDTH);
		addWidget(autoSwitchCountdownTextField);
		listViewWidget.add(Text.translatable("gui.jsblock.auto_switch_countdown"), new MappedWidget(autoSwitchCountdownTextField));

		autoSwitchDurationTextField.setWidth(AUTO_SWITCH_NUMBER_WIDTH);
		addWidget(autoSwitchDurationTextField);
		listViewWidget.add(Text.translatable("gui.jsblock.auto_switch_duration"), new MappedWidget(autoSwitchDurationTextField));

		listViewWidget.addCategory(Text.translatable("gui.jsblock.departure_switch_section"));

		addWidget(depAutoSwitchCheckbox);
		listViewWidget.add(Text.translatable("gui.jsblock.dep_auto_switch_enabled"), new MappedWidget(depAutoSwitchCheckbox));

		depAutoSwitchPresetTextField.setWidth(AUTO_SWITCH_PRESET_WIDTH);
		addWidget(depAutoSwitchPresetTextField);
		listViewWidget.add(Text.translatable("gui.jsblock.dep_auto_switch_preset"), new MappedWidget(depAutoSwitchPresetTextField));

		depAutoSwitchCountdownTextField.setWidth(AUTO_SWITCH_NUMBER_WIDTH);
		addWidget(depAutoSwitchCountdownTextField);
		listViewWidget.add(Text.translatable("gui.jsblock.dep_auto_switch_countdown"), new MappedWidget(depAutoSwitchCountdownTextField));

		depAutoSwitchDurationTextField.setWidth(AUTO_SWITCH_NUMBER_WIDTH);
		addWidget(depAutoSwitchDurationTextField);
		listViewWidget.add(Text.translatable("gui.jsblock.dep_auto_switch_duration"), new MappedWidget(depAutoSwitchDurationTextField));

		addWidget(depAutoSwitchUntilCloseCheckbox);
		listViewWidget.add(Text.translatable("gui.jsblock.dep_auto_switch_until_close"), new MappedWidget(depAutoSwitchUntilCloseCheckbox));
	}

	@Override
	public void tick() {
		super.tick();

		for (WidgetBetterTextField textField : customMessagesWidgets) {
			textField.tick();
		}

		autoSwitchPresetTextField.tick();
		autoSwitchCountdownTextField.tick();
		autoSwitchDurationTextField.tick();
		depAutoSwitchPresetTextField.tick();
		depAutoSwitchCountdownTextField.tick();
		depAutoSwitchDurationTextField.tick();
	}

	protected String[] collectCustomMessages() {
		final String[] collected = new String[customMessagesWidgets.length];

		for (int i = 0; i < collected.length; i++) {
			collected[i] = customMessagesWidgets[i].getValue();
		}

		return collected;
	}

	protected boolean[] collectRowHidden() {
		final boolean[] collected = new boolean[rowHiddenWidgets.length];

		for (int i = 0; i < collected.length; i++) {
			collected[i] = rowHiddenWidgets[i].selected();
		}

		return collected;
	}

	protected boolean isHidePlatformNumberSelected() {
		return hidePlatformNumberWidget.selected();
	}

	protected boolean isAutoSwitchEnabled() {
		return autoSwitchCheckbox.selected();
	}

	protected String getAutoSwitchPreset() {
		return autoSwitchPresetTextField.getValue();
	}

	protected int getAutoSwitchCountdown() {
		return parseInteger(autoSwitchCountdownTextField, 10);
	}

	protected int getAutoSwitchDuration() {
		return parseInteger(autoSwitchDurationTextField, 10);
	}

	protected boolean isDepAutoSwitchEnabled() {
		return depAutoSwitchCheckbox.selected();
	}

	protected String getDepAutoSwitchPreset() {
		return depAutoSwitchPresetTextField.getValue();
	}

	protected int getDepAutoSwitchCountdown() {
		return parseInteger(depAutoSwitchCountdownTextField, 10);
	}

	protected int getDepAutoSwitchDuration() {
		return parseInteger(depAutoSwitchDurationTextField, 10);
	}

	protected boolean isDepAutoSwitchUntilClose() {
		return depAutoSwitchUntilCloseCheckbox.selected();
	}

	private static int parseInteger(WidgetBetterTextField textField, int fallback) {
		try {
			return Integer.parseInt(textField.getValue().trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
