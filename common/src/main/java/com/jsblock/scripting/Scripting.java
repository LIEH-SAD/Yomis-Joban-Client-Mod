package com.jsblock.scripting;

import com.jsblock.Joban;
import com.jsblock.client.ClientConfig;
import com.jsblock.data.PIDSPreset;
import com.jsblock.scripting.core.ScriptInstanceManager;
import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.core.util.ScriptResourceUtil;
import com.jsblock.scripting.core.util.TimingJS;
import com.jsblock.scripting.primitive.ParsedScript;
import com.jsblock.scripting.primitive.ScriptContent;
import com.jsblock.scripting.resource.ScriptResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
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
    private static final Map<String, ParsedScript> inlineScripts = new ConcurrentHashMap<>();
    private static final Pattern SCRIPT_LOCATION_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
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

    public static void reload() {
        if (scriptManager == null) return;
        scriptManager.reset();
        clearInlineScripts();
        scriptManager.getResourceLoader().reload();
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
            return new ByteArrayInputStream(external.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return null;
        try {
            final Optional<Resource> resource = minecraft.getResourceManager().getResource(location);
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
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        try {
            return minecraft.getResourceManager().getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean shouldDisableRestrictions() {
        return ClientConfig.getDisableScriptRestrictions();
    }

    /**
     * Resolve the script attached to a PIDS preset.
     * The {@code script} field may be either a resource location ({@code namespace:scripts/foo.js})
     * or inline JS source code.
     */
    public static ParsedScript resolvePidsScript(PIDSPreset preset) {
        if (scriptManager == null || preset == null || preset.script == null || preset.script.isEmpty()) return null;
        final String script = preset.script;

        if (!script.contains("\n") && SCRIPT_LOCATION_PATTERN.matcher(script).matches()) {
            final ResourceLocation location = new ResourceLocation(script);
            final ParsedScript loaded = scriptManager.getResourceLoader().getParsedScript(location);
            if (loaded != null) return loaded;

            final String content = ScriptResourceUtil.readString(location);
            return getInlineScript(script, content == null ? null : content);
        }

        return getInlineScript(script, script);
    }

    private static ParsedScript getInlineScript(String cacheKey, String source) {
        if (source == null || source.isEmpty()) return null;
        return inlineScripts.computeIfAbsent(cacheKey, key -> scriptManager.parseScript("inline:" + key,
                "pids",
                ParsedScript.Configuration.create(Collections.singletonList(new ScriptContent(new ResourceLocation(MOD_ID, "scripts/inline.js"), source)),
                        com.jsblock.scripting.api.ScriptingLanguage.RHINO_JS, 0),
                false));
    }

    public static void clearInlineScripts() {
        inlineScripts.clear();
    }
}
