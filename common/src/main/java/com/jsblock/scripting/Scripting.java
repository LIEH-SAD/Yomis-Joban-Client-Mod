package com.jsblock.scripting;

import com.jsblock.Joban;
import com.jsblock.client.ClientConfig;
import com.jsblock.data.PIDSPreset;
import com.jsblock.scripting.core.ScriptInstanceManager;
import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.core.util.ConsoleJS;
import com.jsblock.scripting.core.util.ScriptResourceUtil;
import com.jsblock.scripting.core.util.TimingJS;
import com.jsblock.scripting.primitive.ParsedScript;
import com.jsblock.scripting.primitive.ScriptContent;
import com.jsblock.scripting.resource.ScriptResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Entry point of the YJCM scripting layer.
 * @author LX86 (JCM)
 */
public final class Scripting {
    public static final String MOD_ID = "jsblock";

    private static final Map<ResourceLocation, String> externalScripts = new ConcurrentHashMap<>();
    private static final Map<PIDSPreset, Optional<ParsedScript>> presetScripts = new ConcurrentHashMap<>();
    /** Answers from {@link #caseFold(ResourceLocation)}, dropped on every reload. */
    private static final Map<String, ResourceLocation> foldedResources = new ConcurrentHashMap<>();
    private static final Pattern SCRIPT_LOCATION_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+:[A-Za-z0-9/._-]+");
    /** Resource manager of the reload currently in progress, see {@link #getResourceManager()}. */
    private static volatile ResourceManager activeResourceManager;
    private static ScriptManager scriptManager;
    private static boolean initialized = false;
    private static ScriptRegistry registry;

    private Scripting() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        final ExecutorService ioExecutorService = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "YJCM-Script-IO");
            thread.setDaemon(true);
            return thread;
        });

        final CopyOnWriteArrayList<ExecutorService> scriptExecutors = new CopyOnWriteArrayList<>();
        scriptExecutors.add(Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "YJCM-Script-Worker-0");
            thread.setDaemon(true);
            return thread;
        }));

        scriptManager = new ScriptManager(Joban.LOGGER, ioExecutorService, scriptExecutors);
        ScriptResourceLoader.registerDefaultLanguage();

        registry = new ScriptRegistry(scriptManager);
        registry.register();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static ScriptRegistry getRegistry() {
        return registry;
    }

    public static ScriptInstanceManager getInstanceManager() {
        return scriptManager == null ? null : scriptManager.getInstanceManager();
    }

    /** Called once per client tick. */
    public static void tick() {
        if (scriptManager == null) return;
        final Minecraft minecraft = Minecraft.getInstance();
        TimingJS.update(minecraft != null && minecraft.isPaused());
        getInstanceManager().tick();
    }

    /**
     * Reload all scripts.
     * @param resourceManager the resource manager of the reload in progress; it holds the new pack contents
     *                        while {@code Minecraft#getResourceManager()} still resolves against the old ones
     */
    public static void reload(ResourceManager resourceManager) {
        if (scriptManager == null) return;
        if (resourceManager != null) activeResourceManager = resourceManager;
        presetScripts.clear();
        foldedResources.clear();
        com.jsblock.scripting.core.rhino.RhinoParsedScript.clearScriptCache();
        scriptManager.reset();
        scriptManager.getResourceLoader().reload();
    }

    /**
     * @return the resource manager of the latest reload, preferring it over {@code Minecraft#getResourceManager()}
     */
    public static ResourceManager getResourceManager() {
        final ResourceManager reloading = activeResourceManager;
        if (reloading != null) return reloading;
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getResourceManager();
    }

    public static void registerExternalScript(ResourceLocation location, String content) {
        externalScripts.put(location, content);
    }

    public static void clearExternalScripts() {
        externalScripts.clear();
    }

    public static InputStream readResource(ResourceLocation location) {
        final String external = externalScripts.get(location);
        if (external != null) {
            return new ByteArrayInputStream(external.getBytes(StandardCharsets.UTF_8));
        }

        final ResourceManager resourceManager = getResourceManager();
        if (resourceManager == null) return null;
        try {
            final Optional<Resource> resource = resourceManager.getResource(location);
            return resource.map(r -> {
                try {
                    return r.open();
                } catch (Exception e) {
                    return null;
                }
            }).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean resourceExists(ResourceLocation location) {
        if (externalScripts.containsKey(location)) return true;
        final ResourceManager resourceManager = getResourceManager();
        if (resourceManager == null) return false;
        try {
            return resourceManager.getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Map a location onto the file a pack actually ships.<br>
     * A {@link ResourceLocation} cannot hold capitals, so a pack that names its file
     * {@code jsblock:scripts/Digital_Rail.js} is unreachable through the obvious lookup. This finds the
     * real spelling by listing the containing directory once and caching the answer until the next reload.
     * @return the exact location when it exists, otherwise the input unchanged
     */
    public static ResourceLocation caseFold(ResourceLocation location) {
        if (location == null) return null;
        if (resourceExists(location)) return location;
        final String key = location.getNamespace() + ":" + location.getPath();
        return foldedResources.computeIfAbsent(key, k -> {
            final ResourceManager resourceManager = getResourceManager();
            if (resourceManager == null) return location;
            final String path = location.getPath();
            final int slash = path.lastIndexOf('/');
            final String directory = slash < 0 ? "" : path.substring(0, slash);
            try {
                for (final ResourceLocation candidate : resourceManager.listResources(directory,
                        id -> id.getNamespace().equals(location.getNamespace())).keySet()) {
                    if (candidate.getPath().equalsIgnoreCase(path)) return candidate;
                }
            } catch (Exception ignored) {
                /* A malformed directory must not take the script down; fall through to the input. */
            }
            return location;
        });
    }

    /**
     * Resolve a script file a preset or an {@code include()} names onto the file a pack actually
     * ships, tolerating a misspelt folder.
     *
     * <p>{@link #caseFold(ResourceLocation)} only repairs capitalisation, because it lists the
     * directory the reference names. A pack that ships {@code pids/script/crt_pids_1.js} but writes
     * {@code include("nanbin:pids/scripts/crt_pids_1.js")} names a directory that does not exist at
     * all, so nothing in it can be listed. Packs do this — the shipped CRT pack does — and the only
     * way to find the file is to ask the namespace for every script and match the file name.</p>
     *
     * <p>Deliberately narrow: the exact and case-folded spellings are tried first, only an
     * <b>unambiguous</b> single match by file name is accepted, and the substitution is logged so the
     * pack author learns their path is wrong. Guessing between two candidates is refused, because the
     * wrong script draws the wrong board.</p>
     *
     * @return the location to read, which is {@code location} itself when nothing better was found
     */
    public static ResourceLocation resolveScriptFile(ResourceLocation location) {
        if (location == null) return null;
        final ResourceLocation folded = caseFold(location);
        if (resourceExists(folded)) return folded;
        final String key = "script:" + folded.getNamespace() + ":" + folded.getPath();
        return foldedResources.computeIfAbsent(key, k -> {
            final ResourceManager resourceManager = getResourceManager();
            if (resourceManager == null) return folded;
            final String path = folded.getPath();
            final String fileName = path.substring(path.lastIndexOf('/') + 1);
            ResourceLocation match = null;
            try {
                for (final ResourceLocation candidate : resourceManager.listResources("",
                        id -> id.getNamespace().equals(folded.getNamespace())).keySet()) {
                    final String candidatePath = candidate.getPath();
                    if (!candidatePath.equals(fileName) && !candidatePath.endsWith("/" + fileName)) continue;
                    /* Two files with the same name mean the pack's path has to be trusted; picking
                       one of them would show the wrong board rather than report the problem. */
                    if (match != null) return folded;
                    match = candidate;
                }
            } catch (Exception ignored) {
                return folded;
            }
            if (match == null) return folded;
            Joban.LOGGER.warn("[YJCM Scripting] Script {}:{} does not exist; using {} from the same pack"
                    + " instead. The pack should name the file it ships.", folded.getNamespace(), folded.getPath(), match);
            return match;
        });
    }

    public static boolean shouldDisableRestrictions() {
        return ClientConfig.getDisableScriptRestrictions();
    }

    /** Whether the given text is a script resource location rather than inline source code. */
    public static boolean isScriptLocation(String text) {
        return text != null && !text.contains("\n") && SCRIPT_LOCATION_PATTERN.matcher(text).matches();
    }

    /**
     * Resolve (and cache) the script attached to a PIDS preset.<br>
     * Parsing errors are logged and make the preset render nothing, exactly like JCM 2.3.
     * @return the parsed script, or {@code null} if the preset has no (working) script
     */
    public static ParsedScript resolvePidsScript(PIDSPreset preset) {
        if (scriptManager == null || preset == null || !preset.hasScript()) return null;
        if (ClientConfig.getSkipScriptParsing()) return null;
        return presetScripts.computeIfAbsent(preset, p -> Optional.ofNullable(parsePresetScript(p))).orElse(null);
    }

    private static ParsedScript parsePresetScript(PIDSPreset preset) {
        final List<ScriptContent> contents = new ArrayList<>();

        /* JCM 2.x always defines SCRIPT_INPUT, so a preset may read it without a guard. Defining it here,
           and not as a preset content, keeps hasScript() answering "does this preset have a script" rather
           than "true for every preset". */
        final boolean declaresScriptInput = preset.scriptContents.stream()
                .anyMatch(content -> content.getLocation().getPath().contains("script_input"));
        if (!declaresScriptInput) {
            contents.add(new ScriptContent(new ResourceLocation(MOD_ID, "internal/pids/script_input/default"), "const SCRIPT_INPUT = {};"));
        }

        for (ScriptContent scriptContent : preset.scriptContents) {
            final String content = scriptContent.getContent();
            /* The legacy "script" key may either hold inline source or point at a script resource */
            if (isScriptLocation(content)) {
                final ResourceLocation location = Scripting.resolveScriptFile(ScriptResourceUtil.resolveExisting(content));
                final String read = ScriptResourceUtil.readString(location);
                if (read == null || read.trim().isEmpty()) {
                    logError(preset, "Script file " + location + " is empty or missing.");
                    continue;
                }
                contents.add(new ScriptContent(location, read));
            } else {
                contents.add(scriptContent);
            }
        }

        for (ResourceLocation declaredLocation : preset.scriptFiles) {
            final ResourceLocation location = resolveScriptFile(declaredLocation);
            final String read = ScriptResourceUtil.readString(location);
            if (read == null || read.trim().isEmpty()) {
                logError(preset, "Script file " + declaredLocation + " is empty or missing.");
                continue;
            }
            contents.add(new ScriptContent(location, read));
        }

        if (contents.isEmpty()) return null;

        final String displayName = "pids_preset:" + (preset.id == null ? "unnamed" : preset.id);
        final ParsedScript parsedScript = scriptManager.parseScript(displayName, "pids",
                ParsedScript.Configuration.create(contents, preset.scriptLang, preset.scriptRateLimit), false);

        if (parsedScript == null) {
            logError(preset, "Failed to parse the script of this preset.");
        } else {
            Joban.LOGGER.info("[YJCM Scripting] Loaded PIDS preset script {}", displayName);
        }

        return parsedScript;
    }

    private static void logError(PIDSPreset preset, String message) {
        final String text = "[YJCM Scripting] PIDS preset \"" + (preset.id == null ? "unnamed" : preset.id) + "\": " + message;
        Joban.LOGGER.error(text);
        ConsoleJS.errorRaw(text);
    }

    public static void clearPresetScripts() {
        presetScripts.clear();
    }
}
