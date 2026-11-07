package com.jsblock.screen;

import com.jsblock.amberui.gui.widget.ListViewWidget;
import com.jsblock.amberui.gui.widget.MappedWidget;
import com.jsblock.client.JobanCustomResources;
import mtr.mappings.Text;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.core.BlockPos;

// 基于 AmberUI 和 JCM 2.3 的屏幕移植
public abstract class PIDSScreen extends PIDSBlockConfigScreen {
	protected static final int AUTO_SWITCH_PRESET_WIDTH = 120;
	protected static final int AUTO_SWITCH_NUMBER_WIDTH = 52;

	protected final BlockPos pos2;

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
		super(pos1, maxArrivals, withPlatformNumberOption, presetId, hidePlatformNumber, null, null, null);
		this.pos2 = pos2;
		this.autoSwitchCountdown = autoSwitchCountdown > 0 ? autoSwitchCountdown : 10;
		this.autoSwitchDuration = autoSwitchDuration > 0 ? autoSwitchDuration : 10;
		this.depAutoSwitchCountdown = depAutoSwitchCountdown > 0 ? depAutoSwitchCountdown : 10;
		this.depAutoSwitchDuration = depAutoSwitchDuration > 0 ? depAutoSwitchDuration : 10;

		this.autoSwitchCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.jsblock.auto_switch_enabled"), checked -> {
		});
		this.autoSwitchCheckbox.setChecked(autoSwitchEnabled);

		this.autoSwitchPresetTextField = new WidgetSuggestionTextField("None", JobanCustomResources.getPresetIds(), MAX_MESSAGE_LENGTH, true);
		this.autoSwitchPresetTextField.setResponder(text -> {
		});
		TextFields.setValue(this.autoSwitchPresetTextField, autoSwitchPreset);

		this.autoSwitchCountdownTextField = new WidgetBetterTextField("", 4);
		TextFields.setValue(this.autoSwitchCountdownTextField, String.valueOf(this.autoSwitchCountdown));
		this.autoSwitchDurationTextField = new WidgetBetterTextField("", 4);
		TextFields.setValue(this.autoSwitchDurationTextField, String.valueOf(this.autoSwitchDuration));

		this.depAutoSwitchCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.jsblock.dep_auto_switch_enabled"), checked -> {
		});
		this.depAutoSwitchCheckbox.setChecked(depAutoSwitchEnabled);

		this.depAutoSwitchPresetTextField = new WidgetSuggestionTextField("None", JobanCustomResources.getPresetIds(), MAX_MESSAGE_LENGTH, true);
		this.depAutoSwitchPresetTextField.setResponder(text -> {
		});
		TextFields.setValue(this.depAutoSwitchPresetTextField, depAutoSwitchPreset);

		this.depAutoSwitchCountdownTextField = new WidgetBetterTextField("", 4);
		TextFields.setValue(this.depAutoSwitchCountdownTextField, String.valueOf(this.depAutoSwitchCountdown));
		this.depAutoSwitchDurationTextField = new WidgetBetterTextField("", 4);
		TextFields.setValue(this.depAutoSwitchDurationTextField, String.valueOf(this.depAutoSwitchDuration));
		this.depAutoSwitchDurationTextField.setEditable(!depAutoSwitchUntilClose);

		this.depAutoSwitchUntilCloseCheckbox = new WidgetBetterCheckbox(0, 0, SQUARE_SIZE, SQUARE_SIZE, Text.translatable("gui.jsblock.dep_auto_switch_until_close"), checked -> {
		});
		this.depAutoSwitchUntilCloseCheckbox.setChecked(depAutoSwitchUntilClose);
	}

	@Override
	public void addConfigEntries(ListViewWidget listViewWidget) {
		super.addConfigEntries(listViewWidget);
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

		autoSwitchPresetTextField.tick();
		autoSwitchCountdownTextField.tick();
		autoSwitchDurationTextField.tick();
		depAutoSwitchPresetTextField.tick();
		depAutoSwitchCountdownTextField.tick();
		depAutoSwitchDurationTextField.tick();
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
