package com.jsblock.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jsblock.Joban;
import com.jsblock.data.PIDSPreset;
import com.jsblock.data.PIDSPresetEntry;
import com.jsblock.scripting.Scripting;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Class responsible for handling custom resources stuff.
 * @author LX86
 * @since 1.1.4
 */
public class JobanCustomResources {
    private final static PIDSPreset defaultPreset1 = new PIDSPreset(new ResourceLocation(Joban.MOD_ID, "textures/pids_screen/door_cls_apg.png"), true, true, false, new boolean[]{true, true, true, false},null, null, null);
    private final static PIDSPreset defaultPreset2 = new PIDSPreset(new ResourceLocation(Joban.MOD_ID, "textures/pids_screen/door_cls_psd.png"), true, true, false, new boolean[]{true, true, true, false},null, null, null);
    private final static PIDSPreset defaultPreset3 = new PIDSPreset(new ResourceLocation(Joban.MOD_ID, "textures/pids_screen/door_cls_train.png"), true, true, false, new boolean[]{true, true, true, false},null, null, null);
    public static final String CUSTOM_RESOURCES_ID = "joban_custom_resources";
    public static final String customResourcePath = Joban.MOD_ID + ":" + CUSTOM_RESOURCES_ID + ".json";
    /** Presets shipped with the mod, in display order. */
    public static final List<String> BUILTIN_PRESET_IDS = Collections.unmodifiableList(Arrays.asList("door_cls_apg", "door_cls_psd", "door_cls_train"));
    public static HashMap<String, PIDSPreset> PIDSPresets = new HashMap<>();

    /**
     * @return the built-in presets, in display order
     */
    public static List<PIDSPresetEntry> getBuiltInPresets() {
        final List<PIDSPresetEntry> presets = new ArrayList<>();

        for (String id : BUILTIN_PRESET_IDS) {
            final PIDSPreset preset = PIDSPresets.get(id);

            if (preset != null) {
                presets.add(new PIDSPresetEntry(id, preset, true));
            }
        }

        return presets;
    }

    /**
     * @return the presets added through {@code joban_custom_resources.json}, sorted by id
     */
    public static List<PIDSPresetEntry> getCustomPresets() {
        final List<PIDSPresetEntry> presets = new ArrayList<>();
        final List<String> ids = new ArrayList<>();

        for (String id : PIDSPresets.keySet()) {
            if (!BUILTIN_PRESET_IDS.contains(id)) {
                ids.add(id);
            }
        }

        Collections.sort(ids);

        for (String id : ids) {
            presets.add(new PIDSPresetEntry(id, PIDSPresets.get(id), false));
        }

        return presets;
    }

    /**
     * @return the entry with the given id, or {@code null} if it does not exist
     */
    public static PIDSPresetEntry getPresetEntry(String id) {
        final PIDSPreset preset = id == null ? null : PIDSPresets.get(id);
        return preset == null ? null : new PIDSPresetEntry(id, preset, BUILTIN_PRESET_IDS.contains(id));
    }

    /**
     * Should be called when resource manager is being reloaded.
     */
    public static void reload(ResourceManager manager) {
        PIDSPresets.clear();
        /* Add default preset */
        PIDSPresets.put("door_cls_apg", defaultPreset1);
        PIDSPresets.put("door_cls_psd", defaultPreset2);
        PIDSPresets.put("door_cls_train", defaultPreset3);

        /* Read resource */
        readResource(manager, customResourcePath, jsonConfig -> {
            try {
                if(!jsonConfig.has("pids_images") || !jsonConfig.get("pids_images").isJsonArray()) {
                    Joban.LOGGER.warn("[JCM] Invalid joban_custom_resources.json!");
                    Joban.LOGGER.warn("[JCM] \"pids_images\" must be an array");
                    return;
                }

                jsonConfig.get("pids_images").getAsJsonArray().forEach(jsonElement -> {
                    String id = jsonElement.getAsJsonObject().get("id").getAsString();

                    if(PIDSPresets.containsKey(id)) {
                        Joban.LOGGER.warn("[Joban Client] PIDS Preset ID: " + id + " already added.");
                        return;
                    }

                    PIDSPreset preset = PIDSPreset.fromJson(jsonElement);
                    PIDSPresets.put(id, preset);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Joban.LOGGER.info("[Joban Client] Loaded PIDS Preset: " + String.join(", ", PIDSPresets.keySet()));

        /* Re-parse scripts once the resource packs (and therefore the custom presets) are available */
        Scripting.reload();
    }

    private static void readResource(ResourceManager manager, String path, Consumer<JsonObject> callback) {
        try {
            UtilitiesClient.getResources(manager, new ResourceLocation(path)).forEach(resource -> {
                try (final InputStream stream = Utilities.getInputStream(resource)) {
                    callback.accept(new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Utilities.closeResource(resource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception ignored) {
        }
    }
}
