package com.jsblock.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.resources.ResourceLocation;

public class PIDSPreset {
    public ResourceLocation image;
    public Integer color;
    public String font;
    private Int2IntArrayMap carLengthColorMap;
    public boolean[] visibility;
    public boolean customTextPushArrival;
    public boolean showWeather;
    public boolean showClock;
    /** Script attached to this preset: either a resource location ({@code namespace:scripts/foo.js}) or inline JS source. */
    public String script = null;
    /** Canvas width in script units, defaults to the background texture width. */
    public Integer scriptWidth = null;
    /** Canvas height in script units, defaults to the background texture height. */
    public Integer scriptHeight = null;

    public PIDSPreset(ResourceLocation image, boolean showWeather, boolean showClock, boolean customTextPushArrival, boolean[] visibility, Integer color, String font, Int2IntArrayMap carLengthColorMap) {
        this.image = image;
        this.showWeather = showWeather;
        this.showClock = showClock;
        this.visibility = visibility;
        this.color = color;
        this.font = font;
        this.customTextPushArrival = customTextPushArrival;
        this.carLengthColorMap = carLengthColorMap;
    }

    public static PIDSPreset fromJson(JsonElement element) {
        boolean[] hideRowArray = new boolean[4];
        boolean showWeather = element.getAsJsonObject().has("showWeather") ? element.getAsJsonObject().get("showWeather").getAsBoolean() : true;
        boolean showClock = element.getAsJsonObject().has("showClock") ? element.getAsJsonObject().get("showClock").getAsBoolean() : true;
        boolean customTextPushArrival = element.getAsJsonObject().has("customTextPushArrival") && element.getAsJsonObject().get("customTextPushArrival").getAsBoolean();
        String fonts = element.getAsJsonObject().has("fonts") ? element.getAsJsonObject().get("fonts").getAsString() : null;
        String hexColor = element.getAsJsonObject().has("color") ? element.getAsJsonObject().get("color").getAsString() : null;
        JsonArray carLengthColor = element.getAsJsonObject().has("carLengthColor") ? element.getAsJsonObject().get("carLengthColor").getAsJsonArray() : null;
        JsonArray hiddenRowList = element.getAsJsonObject().has("hideRow") ? element.getAsJsonObject().get("hideRow").getAsJsonArray() : null;
        ResourceLocation background = new ResourceLocation(element.getAsJsonObject().get("background").getAsString());

        Int2IntArrayMap carLengthColorMap = null;

        Integer color = null;
        if(hexColor != null) {
            color = Integer.parseInt(hexColor, 16);
        }

        if(hiddenRowList != null) {
            for (int i = 0; i < Math.min(hiddenRowList.size(), hideRowArray.length); i++) {
                hideRowArray[i] = hiddenRowList.get(i).getAsBoolean();
            }
        } else {
            hideRowArray = null;
        }

        if(carLengthColor != null) {
            carLengthColorMap = new Int2IntArrayMap();
            for (int i = 0; i < carLengthColor.size(); i++) {
                JsonElement colorElement = carLengthColor.get(i);
                if(!colorElement.isJsonNull()) {
                    carLengthColorMap.put(i, Integer.parseInt(colorElement.getAsString(), 16));
                }
            }
        }
        final PIDSPreset preset = new PIDSPreset(background, showWeather, showClock, customTextPushArrival, hideRowArray, color, fonts, carLengthColorMap);
        if (element.getAsJsonObject().has("script") && !element.getAsJsonObject().get("script").isJsonNull()) {
            preset.script = element.getAsJsonObject().get("script").getAsString();
        }
        if (element.getAsJsonObject().has("scriptWidth") && !element.getAsJsonObject().get("scriptWidth").isJsonNull()) {
            preset.scriptWidth = element.getAsJsonObject().get("scriptWidth").getAsInt();
        }
        if (element.getAsJsonObject().has("scriptHeight") && !element.getAsJsonObject().get("scriptHeight").isJsonNull()) {
            preset.scriptHeight = element.getAsJsonObject().get("scriptHeight").getAsInt();
        }
        return preset;
    }

    public PIDSPreset withScript(String script) {
        this.script = script;
        return this;
    }

    public PIDSPreset withScriptSize(Integer width, Integer height) {
        this.scriptWidth = width;
        this.scriptHeight = height;
        return this;
    }

    public Integer getCarColor(int car) {
        if(carLengthColorMap == null || !carLengthColorMap.containsKey(car - 1)) return null;
        return carLengthColorMap.get(car - 1);
    }
}
