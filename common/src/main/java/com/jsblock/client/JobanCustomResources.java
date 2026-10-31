package com.jsblock.client;

import com.google.gson.JsonElement;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Class responsible for handling custom resources stuff.<br>
 * Presets are declared in {@code assets/jsblock/joban_custom_resources.json} (shipped by the mod and
 * overridable/extendable by resource packs), mirroring JCM 2.3's {@code PIDSManager}.
 * @author LX86
 * @since 1.1.4
 */
public class JobanCustomResources {
    public static final String CUSTOM_RESOURCES_ID = "joban_custom_resources";
    public static final String customResourcePath = Joban.MOD_ID + ":" + CUSTOM_RESOURCES_ID + ".json";
    /** Every loaded preset, in declaration order (built-in presets of the mod come first). */
    public static volatile Map<String, PIDSPreset> PIDSPresets = Collections.emptyMap();

    /**
     * @return the built-in presets, in declaration order
     */
    public static List<PIDSPresetEntry> getBuiltInPresets() {
        final List<PIDSPresetEntry> presets = new ArrayList<>();

        for (Map.Entry<String, PIDSPreset> entry : PIDSPresets.entrySet()) {
            if (entry.getValue().builtin) {
                presets.add(new PIDSPresetEntry(entry.getKey(), entry.getValue()));
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

        for (Map.Entry<String, PIDSPreset> entry : PIDSPresets.entrySet()) {
            if (!entry.getValue().builtin) {
                ids.add(entry.getKey());
            }
        }

        Collections.sort(ids);

        for (String id : ids) {
            presets.add(new PIDSPresetEntry(id, PIDSPresets.get(id)));
        }

        return presets;
    }

    /**
     * @return the entry with the given id, or {@code null} if it does not exist
     */
    public static PIDSPresetEntry getPresetEntry(String id) {
        final PIDSPreset preset = id == null ? null : PIDSPresets.get(id);
        return preset == null ? null : new PIDSPresetEntry(id, preset);
    }

    /** @return all preset ids, in declaration order */
    public static List<String> getPresetIds() {
        return new ArrayList<>(PIDSPresets.keySet());
    }

    /**
     * Should be called when resource manager is being reloaded.
     */
    public static void reload(ResourceManager manager) {
        final Map<String, PIDSPreset> presets = new LinkedHashMap<>();

        /* Read resource. The mod ships its own copy, resource packs can add to it or override it entirely. */
        readResource(manager, customResourcePath, jsonConfig -> loadJson(presets, jsonConfig));

        /* Swap the whole map in one go so the render thread never sees a half-populated preset list */
        PIDSPresets = presets;

        Joban.LOGGER.info("[Joban Client] Loaded PIDS Preset: " + String.join(", ", presets.keySet()));

        /* Re-parse scripts once the resource packs (and therefore the custom presets) are available */
        Scripting.reload(manager);
    }

    private static void loadJson(Map<String, PIDSPreset> presets, JsonObject jsonConfig) {
        if (!jsonConfig.has("pids_images") || !jsonConfig.get("pids_images").isJsonArray()) {
            Joban.LOGGER.warn("[JCM] Invalid joban_custom_resources.json!");
            Joban.LOGGER.warn("[JCM] \"pids_images\" must be an array");
            return;
        }

        for (JsonElement jsonElement : jsonConfig.get("pids_images").getAsJsonArray()) {
            if (!jsonElement.isJsonObject() || !jsonElement.getAsJsonObject().has("id")) {
                Joban.LOGGER.warn("[Joban Client] Skipped a PIDS preset entry without an \"id\".");
                continue;
            }

            final JsonObject presetObject = jsonElement.getAsJsonObject();
            final String id = presetObject.get("id").getAsString();

            try {
                final PIDSPreset preset = PIDSPreset.fromJson(presetObject, id);

                if (presets.containsKey(id) && !preset.builtin) {
                    Joban.LOGGER.warn("[Joban Client] PIDS Preset ID: " + id + " already added.");
                    continue;
                }

                presets.put(id, preset);
            } catch (Exception e) {
                Joban.LOGGER.error("[Joban Client] Failed to load PIDS Preset ID: " + id + " (" + e + ")");
            }
        }
    }

    private static void readResource(ResourceManager manager, String path, Consumer<JsonObject> callback) {
        try {
            UtilitiesClient.getResources(manager, new ResourceLocation(path)).forEach(resource -> {
                try (final InputStream stream = Utilities.getInputStream(resource)) {
                    callback.accept(new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (Exception e) {
                    Joban.LOGGER.error("[Joban Client] Failed to read " + path, e);
                }
                try {
                    Utilities.closeResource(resource);
                } catch (IOException e) {
                    Joban.LOGGER.error("[Joban Client] Failed to close " + path, e);
                }
            });
        } catch (Exception ignored) {
        }
    }
}
