package com.jsblock.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsblock.scripting.primitive.ScriptContent;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A PIDS screen style.<br>
 * Mirrors JCM 2.3's {@code PIDSPresetBase} / {@code JsonPIDSPreset} / {@code ScriptPIDSPreset}:
 * a preset is either drawn from a background texture with the built-in row renderer, or drawn
 * entirely by the script(s) attached to it.
 */
public class PIDSPreset {
    public static final String DEFAULT_SCRIPT_LANGUAGE = "js";
    public static final String DEFAULT_SCRIPT_THUMBNAIL = "jsblock:textures/gui/pids_preview_js.png";

    /** Preset id as declared in {@code joban_custom_resources.json}, {@code null} for renderer fallback presets. */
    public final String id;
    /** Display name, {@code null} to fall back to {@link #id}. */
    public final String name;
    /** Whether this preset is shipped by the mod itself. */
    public final boolean builtin;
    /** PIDS type ids this preset is not allowed on (see {@code PIDSPresetScreen#getPIDSType}). */
    public final List<String> blacklist;

    /** Background texture, {@code null} for script-only presets. */
    public ResourceLocation image;
    /** Thumbnail texture, {@code null} to fall back to {@link #image}. */
    public ResourceLocation thumbnail;
    public Integer color;
    public String font;
    private Int2IntArrayMap carLengthColorMap;
    public boolean[] visibility;
    public boolean customTextPushArrival;
    public boolean showWeather;
    public boolean showClock;
    public boolean hidePlatform;
    public boolean topPadding;

    /** Script canvas width in script units, defaults to the background texture width. */
    public final Integer scriptWidth;
    /** Script canvas height in script units, defaults to the background texture height. */
    public final Integer scriptHeight;
    public final String scriptLang;
    public final double scriptRateLimit;
    /** Inline script chunks ({@code scriptInput}, {@code scriptTexts} and the legacy {@code script} key). */
    public final List<ScriptContent> scriptContents;
    /** Scripts read from the resource pack when the preset is first used. */
    public final List<ResourceLocation> scriptFiles;

    public PIDSPreset(ResourceLocation image, boolean showWeather, boolean showClock, boolean customTextPushArrival, boolean[] visibility, Integer color, String font, Int2IntArrayMap carLengthColorMap) {
        this(null, null, false, Collections.emptyList(), image, null, showWeather, showClock, customTextPushArrival, visibility, color, font, carLengthColorMap, false, true,
                null, null, DEFAULT_SCRIPT_LANGUAGE, 0, Collections.emptyList(), Collections.emptyList());
    }

    private PIDSPreset(String id, String name, boolean builtin, List<String> blacklist, ResourceLocation image, ResourceLocation thumbnail, boolean showWeather, boolean showClock,
                       boolean customTextPushArrival, boolean[] visibility, Integer color, String font, Int2IntArrayMap carLengthColorMap, boolean hidePlatform, boolean topPadding,
                       Integer scriptWidth, Integer scriptHeight, String scriptLang, double scriptRateLimit, List<ScriptContent> scriptContents, List<ResourceLocation> scriptFiles) {
        this.id = id;
        this.name = name;
        this.builtin = builtin;
        this.blacklist = blacklist;
        this.image = image;
        this.thumbnail = thumbnail;
        this.showWeather = showWeather;
        this.showClock = showClock;
        this.customTextPushArrival = customTextPushArrival;
        this.visibility = visibility;
        this.color = color;
        this.font = font;
        this.carLengthColorMap = carLengthColorMap;
        this.hidePlatform = hidePlatform;
        this.topPadding = topPadding;
        this.scriptWidth = scriptWidth;
        this.scriptHeight = scriptHeight;
        this.scriptLang = scriptLang;
        this.scriptRateLimit = scriptRateLimit;
        this.scriptContents = scriptContents;
        this.scriptFiles = scriptFiles;
    }

    /**
     * Parse a preset entry of {@code pids_images}.
     * @throws IllegalArgumentException if the preset is neither renderable from a background nor from a script
     */
    public static PIDSPreset fromJson(JsonObject json, String id) {
        if (json == null) throw new IllegalArgumentException("Preset " + id + " is not a JSON object");

        boolean[] hideRowArray = new boolean[4];
        final JsonArray hiddenRowList = getArray(json, "hideRow");
        if (hiddenRowList != null) {
            for (int i = 0; i < Math.min(hiddenRowList.size(), hideRowArray.length); i++) {
                hideRowArray[i] = hiddenRowList.get(i).getAsBoolean();
            }
        } else {
            hideRowArray = null;
        }

        Int2IntArrayMap carLengthColorMap = null;
        final JsonArray carLengthColor = getArray(json, "carLengthColor");
        if (carLengthColor != null) {
            carLengthColorMap = new Int2IntArrayMap();
            for (int i = 0; i < carLengthColor.size(); i++) {
                final JsonElement colorElement = carLengthColor.get(i);
                if (!colorElement.isJsonNull()) {
                    carLengthColorMap.put(i, Integer.parseInt(colorElement.getAsString(), 16));
                }
            }
        }

        final List<String> blacklist = new ArrayList<>();
        final JsonArray blacklistArray = getArray(json, "blacklist");
        if (blacklistArray != null) {
            for (final JsonElement element : blacklistArray) {
                blacklist.add(element.getAsString());
            }
        }

        final ResourceLocation background = getResourceLocation(json, "background");
        final List<ScriptContent> scriptContents = new ArrayList<>();
        final List<ResourceLocation> scriptFiles = new ArrayList<>();

        if (json.has("scriptInput") && !json.get("scriptInput").isJsonNull()) {
            scriptContents.add(new ScriptContent(internalId("script_input", id, 0), "const SCRIPT_INPUT = " + json.get("scriptInput") + ";"));
        }

        final JsonArray scriptTexts = getArray(json, "scriptTexts");
        if (scriptTexts != null) {
            for (int i = 0; i < scriptTexts.size(); i++) {
                scriptContents.add(new ScriptContent(internalId("script_text", id, i), scriptTexts.get(i).getAsString()));
            }
        }

        final JsonArray scriptFileArray = getArray(json, "scriptFiles");
        if (scriptFileArray != null) {
            for (final JsonElement element : scriptFileArray) {
                scriptFiles.add(com.jsblock.scripting.core.util.ScriptResourceUtil.lenientLocation(element.getAsString()));
            }
        }

        if (json.has("script") && !json.get("script").isJsonNull()) {
            scriptContents.add(new ScriptContent(internalId("script", id, 0), json.get("script").getAsString()));
        }

        if (background == null && scriptContents.isEmpty() && scriptFiles.isEmpty()) {
            throw new IllegalArgumentException("Preset " + id + " has no \"background\" and no script");
        }

        final ResourceLocation thumbnail = getResourceLocation(json, "thumbnail");
        /* JCM 2.3's ScriptPIDSPreset never draws the clock or the weather icon */
        final boolean scripted = !scriptContents.isEmpty() || !scriptFiles.isEmpty();
        final String hexColor = json.has("color") && !json.get("color").isJsonNull() ? json.get("color").getAsString() : null;
        final String fonts = json.has("fonts") && !json.get("fonts").isJsonNull() ? json.get("fonts").getAsString() : null;
        final String scriptLang = json.has("scriptLang") && !json.get("scriptLang").isJsonNull() ? json.get("scriptLang").getAsString() : DEFAULT_SCRIPT_LANGUAGE;

        return new PIDSPreset(
                id,
                json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : id,
                json.has("builtin") && json.get("builtin").isJsonPrimitive() && json.get("builtin").getAsBoolean(),
                Collections.unmodifiableList(blacklist),
                background,
                thumbnail,
                getBoolean(json, "showWeather", true) && !scripted,
                getBoolean(json, "showClock", true) && !scripted,
                getBoolean(json, "customTextPushArrival", false),
                hideRowArray,
                hexColor == null ? null : Integer.parseInt(hexColor, 16),
                fonts,
                carLengthColorMap,
                getBoolean(json, "hidePlatform", false),
                getBoolean(json, "topPadding", true),
                getInt(json, "scriptWidth"),
                getInt(json, "scriptHeight"),
                scriptLang,
                getDouble(json, "scriptRateLimit", 0),
                Collections.unmodifiableList(scriptContents),
                Collections.unmodifiableList(scriptFiles)
        );
    }

    /** Whether this preset draws its content through a script instead of the built-in row renderer. */
    public boolean hasScript() {
        return !scriptContents.isEmpty() || !scriptFiles.isEmpty();
    }

    public ResourceLocation getThumbnail() {
        if (thumbnail != null) return thumbnail;
        if (image != null) return image;
        return new ResourceLocation(DEFAULT_SCRIPT_THUMBNAIL);
    }

    public boolean hasThumbnail() {
        return thumbnail != null || image != null;
    }

    public boolean typeAllowed(String pidsType) {
        return pidsType == null || !blacklist.contains(pidsType);
    }

    public Integer getCarColor(int car) {
        if (carLengthColorMap == null || !carLengthColorMap.containsKey(car - 1)) return null;
        return carLengthColorMap.get(car - 1);
    }

    private static JsonArray getArray(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsJsonArray() : null;
    }

    private static ResourceLocation getResourceLocation(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? com.jsblock.scripting.core.util.ScriptResourceUtil.lenientLocation(json.get(key).getAsString())
                : null;
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsBoolean() : fallback;
    }

    private static Integer getInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : null;
    }

    private static double getDouble(JsonObject json, String key, double fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : fallback;
    }

    private static ResourceLocation internalId(String kind, String presetId, int index) {
        final String safeId = presetId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return new ResourceLocation("jsblock", "internal/pids/" + kind + "/" + safeId + (index == 0 ? "" : "_" + index));
    }
}
