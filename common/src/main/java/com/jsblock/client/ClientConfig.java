package com.jsblock.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jsblock.Joban;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Class responsible for reading/saving the config value.<br>
 * For config screen, see {@link com.jsblock.screen.ClientConfigScreen}
 * @author LX86
 * @since 1.0.6
 */
public class ClientConfig {
    private static final String CONFIG_PATH = System.getProperty("user.dir") + "/config/" + "jsclient.json";
    private static boolean bypassServerVersionCheck = false;
    private static boolean debugMode = false;
    private static boolean skipScriptParsing = false;
    private static boolean disableScriptRestrictions = false;
    private static boolean scriptDebugMode = false;
    private static boolean scriptShowLogSource = true;

    /**
     * This loads the config file and sets the variable internally
     */
    public static void loadConfig() {
        if (!Files.exists(Paths.get(CONFIG_PATH))) {
            Joban.LOGGER.warn("[Joban Client] Config file not found, generating one...");
            try {
                writeConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        Joban.LOGGER.info("[Joban Client] Reading Config...");
        try {
            final JsonObject jsonConfig = new JsonParser().parse(String.join("", Files.readAllLines(Paths.get(CONFIG_PATH)))).getAsJsonObject();

            if(jsonConfig.has("bypassVersionCheck")) {
                bypassServerVersionCheck = jsonConfig.get("bypassVersionCheck").getAsBoolean();
            }

            if(jsonConfig.has("debugMode")) {
                debugMode = jsonConfig.get("debugMode").getAsBoolean();
            }

            if(jsonConfig.has("skipScriptParsing")) {
                skipScriptParsing = jsonConfig.get("skipScriptParsing").getAsBoolean();
            }

            if(jsonConfig.has("disableScriptRestrictions")) {
                disableScriptRestrictions = jsonConfig.get("disableScriptRestrictions").getAsBoolean();
            }

            if(jsonConfig.has("scriptDebugMode")) {
                scriptDebugMode = jsonConfig.get("scriptDebugMode").getAsBoolean();
            }

            if(jsonConfig.has("scriptShowLogSource")) {
                scriptShowLogSource = jsonConfig.get("scriptShowLogSource").getAsBoolean();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                writeConfig();
            } catch (Exception er) {
                er.printStackTrace();
            }
        }
    }

    /**
     * This writes the current config
     */
    public static void writeConfig() {
        Joban.LOGGER.info("[Joban Client] Writing Config...");
        final JsonObject jsonConfig = new JsonObject();
        jsonConfig.addProperty("bypassVersionCheck", bypassServerVersionCheck);
        jsonConfig.addProperty("debugMode", debugMode);
        jsonConfig.addProperty("skipScriptParsing", skipScriptParsing);
        jsonConfig.addProperty("disableScriptRestrictions", disableScriptRestrictions);
        jsonConfig.addProperty("scriptDebugMode", scriptDebugMode);
        jsonConfig.addProperty("scriptShowLogSource", scriptShowLogSource);

        try {
            Files.write(Paths.get(CONFIG_PATH), Collections.singleton(new GsonBuilder().setPrettyPrinting().create().toJson(jsonConfig)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset every option to its default value (does not write the file)
     */
    public static void loadDefaults() {
        bypassServerVersionCheck = false;
        debugMode = false;
        skipScriptParsing = false;
        disableScriptRestrictions = false;
        scriptDebugMode = false;
        scriptShowLogSource = true;
    }

    public static boolean getVersionCheckDisabled() {
        return bypassServerVersionCheck;
    }

    public static boolean getDebugModeEnabled() {
        return debugMode;
    }

    public static boolean getSkipScriptParsing() {
        return skipScriptParsing;
    }

    public static boolean getDisableScriptRestrictions() {
        return disableScriptRestrictions;
    }

    public static boolean getScriptDebugMode() {
        return scriptDebugMode;
    }

    public static boolean getScriptShowLogSource() {
        return scriptShowLogSource;
    }

    public static void setSkipScriptParsing(boolean skip) {
        skipScriptParsing = skip;
    }

    public static void setDisableScriptRestrictions(boolean disabled) {
        disableScriptRestrictions = disabled;
    }

    public static void setScriptDebugMode(boolean enabled) {
        scriptDebugMode = enabled;
    }

    public static void setScriptShowLogSource(boolean enabled) {
        scriptShowLogSource = enabled;
    }

    public static boolean setDebugMode(boolean enabled) {
        debugMode = enabled;
        return debugMode;
    }

    public static boolean setVersionCheckDisabled(boolean disabled) {
        bypassServerVersionCheck = disabled;
        return bypassServerVersionCheck;
    }
}
