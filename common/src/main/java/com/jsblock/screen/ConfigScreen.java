package com.jsblock.screen;

import com.jsblock.Joban;
import com.jsblock.client.ClientConfig;
import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The configuration screen for JCM.<br>
 * The background panorama and list layout is provided by {@link ConfigScreenBase}.
 * @author LX86
 * @since 1.2.12
 */
public class ConfigScreen extends ConfigScreenBase implements IGui {
	private static final Component TITLE_TEXT = Text.translatable("gui.jsblock.brand");
	private static final Component VERSION_TEXT = Text.translatable("gui.jsblock.version", Joban.getVersion());
	private static final Component CATEGORY_GENERAL = Text.translatable("gui.jsblock.config.category.general");
	private static final Component CATEGORY_SCRIPTING = Text.translatable("gui.jsblock.config.category.scripting");

	private boolean entriesRegistered;

	private WidgetCheckbox renderingCheckbox;
	private WidgetCheckbox railRenderingCheckbox;
	private WidgetCheckbox ridingVehicleCheckbox;
	private WidgetCheckbox versionCheckCheckbox;
	private WidgetCheckbox debugModeCheckbox;
	private WidgetCheckbox skipScriptParsingCheckbox;
	private WidgetCheckbox disableScriptRestrictionsCheckbox;
	private WidgetCheckbox scriptDebugModeCheckbox;
	private WidgetCheckbox shipLogSourceCheckbox;

	public ConfigScreen() {
		super(TITLE_TEXT, VERSION_TEXT);
	}

	@Override
	protected void init() {
		if(!entriesRegistered) {
			entriesRegistered = true;

			/* General */
			addCategory(CATEGORY_GENERAL);

			renderingCheckbox = addCheckboxEntry("gui.jsblock.config.enable_render", ClientConfig.getRenderDisabled(), ClientConfig::setRenderDisabled);
			railRenderingCheckbox = addCheckboxEntry("gui.jsblock.config.disable_rail_render", ClientConfig.getRailRenderDisabled(), ClientConfig::setRailRenderDisabled);
			ridingVehicleCheckbox = addCheckboxEntry("gui.jsblock.config.hide_riding_vehicle", ClientConfig.getHideRidingVehicle(), ClientConfig::setHideRidingVehicle);
			versionCheckCheckbox = addCheckboxEntry("gui.jsblock.config.ignore_ver_check", ClientConfig.getVersionCheckDisabled(), ClientConfig::setVersionCheckDisabled);
			debugModeCheckbox = addCheckboxEntry("gui.jsblock.config.debug_mode", ClientConfig.getDebugModeEnabled(), ClientConfig::setDebugMode);

			/* Scripting */
			addCategory(CATEGORY_SCRIPTING);

			skipScriptParsingCheckbox = addCheckboxEntry("gui.jsblock.config.skip_script_parsing", ClientConfig.getSkipScriptParsing(), ClientConfig::setSkipScriptParsing);
			disableScriptRestrictionsCheckbox = addCheckboxEntry("gui.jsblock.config.disable_script_restrictions", ClientConfig.getDisableScriptRestrictions(), ClientConfig::setDisableScriptRestrictions);
			scriptDebugModeCheckbox = addCheckboxEntry("gui.jsblock.config.script_debug_mode", ClientConfig.getScriptDebugMode(), ClientConfig::setScriptDebugMode);
			shipLogSourceCheckbox = addCheckboxEntry("gui.jsblock.config.script_show_log_source", ClientConfig.getScriptShowLogSource(), ClientConfig::setScriptShowLogSource);

			/* Footer */
			final Button latestLogButton = UtilitiesClient.newButton(Text.translatable("gui.jsblock.config.openlog"), button -> showLog());
			final Button crashLogButton = UtilitiesClient.newButton(Text.translatable("gui.jsblock.config.opencrashlog"), button -> showCrashLog());

			final Button saveButton = UtilitiesClient.newButton(Text.translatable("gui.jsblock.config.save"), button -> closeScreen(true));
			final Button discardButton = UtilitiesClient.newButton(Text.translatable("gui.jsblock.config.discard"), button -> discardChanges());
			final Button resetButton = UtilitiesClient.newButton(Text.translatable("gui.jsblock.config.reset"), button -> resetToDefault());

			addFooterRow(latestLogButton, crashLogButton);
			addFooterRow(saveButton, discardButton, resetButton);
		}

		super.init();
	}

	private WidgetCheckbox addCheckboxEntry(String translationKey, boolean initialValue, Consumer<Boolean> setter) {
		final WidgetCheckbox checkbox = new WidgetCheckbox(0, 0, initialValue, setter);
		addEntry(Text.translatable(translationKey), checkbox);
		return checkbox;
	}

	private void discardChanges() {
		/* Reload the on-disk values and refresh the checkbox states */
		ClientConfig.loadConfig();
		refreshCheckboxes();
		closeScreen(false);
	}

	private void resetToDefault() {
		ClientConfig.setRenderDisabled(false);
		ClientConfig.setRailRenderDisabled(false);
		ClientConfig.setHideRidingVehicle(false);
		ClientConfig.setVersionCheckDisabled(false);
		ClientConfig.setDebugMode(false);
		ClientConfig.setSkipScriptParsing(false);
		ClientConfig.setDisableScriptRestrictions(false);
		ClientConfig.setScriptDebugMode(false);
		ClientConfig.setScriptShowLogSource(true);
		refreshCheckboxes();
	}

	private void refreshCheckboxes() {
		setChecked(renderingCheckbox, ClientConfig::getRenderDisabled);
		setChecked(railRenderingCheckbox, ClientConfig::getRailRenderDisabled);
		setChecked(ridingVehicleCheckbox, ClientConfig::getHideRidingVehicle);
		setChecked(versionCheckCheckbox, ClientConfig::getVersionCheckDisabled);
		setChecked(debugModeCheckbox, ClientConfig::getDebugModeEnabled);
		setChecked(skipScriptParsingCheckbox, ClientConfig::getSkipScriptParsing);
		setChecked(disableScriptRestrictionsCheckbox, ClientConfig::getDisableScriptRestrictions);
		setChecked(scriptDebugModeCheckbox, ClientConfig::getScriptDebugMode);
		setChecked(shipLogSourceCheckbox, ClientConfig::getScriptShowLogSource);
	}

	private static void setChecked(WidgetCheckbox checkbox, Supplier<Boolean> getter) {
		if(checkbox != null) {
			checkbox.setChecked(getter.get());
		}
	}

	private void showLog() {
		final File latestLog = Paths.get(minecraft.gameDirectory.toString(), "logs", "latest.log").toFile();

		if(latestLog.exists()) {
			Util.getPlatform().openFile(latestLog);
		}
	}

	private void showCrashLog() {
		final Path logDir = Paths.get(minecraft.gameDirectory.toString(), "crash-reports");

		if(Files.exists(logDir)) {
			final File[] crashLogList = logDir.toFile().listFiles();

			if(crashLogList != null && crashLogList.length > 0) {
				Arrays.sort(crashLogList, Comparator.comparingLong(File::lastModified));
				Util.getPlatform().openFile(crashLogList[0]);
			} else {
				showToast("gui.jsblock.config.nocrashlogfound");
			}
		} else {
			showToast("gui.jsblock.config.nocrashlogfound");
		}
	}

	private void showToast(String translationKey) {
		final SystemToast toast = SystemToast.multiline(minecraft, SystemToast.SystemToastIds.TUTORIAL_HINT, TITLE_TEXT, Text.translatable(translationKey));
		minecraft.getToasts().addToast(toast);
	}

	@Override
	public void onSave() {
		try {
			ClientConfig.writeConfig();
		} catch (Exception e) {
			showToast("gui.jsblock.config.savefailed");
		}
	}
}
