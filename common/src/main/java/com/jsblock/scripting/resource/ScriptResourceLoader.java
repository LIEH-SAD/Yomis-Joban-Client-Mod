package com.jsblock.scripting.resource;

import com.jsblock.client.ClientConfig;
import com.jsblock.scripting.Scripting;
import com.jsblock.scripting.core.ScriptManager;
import com.jsblock.scripting.core.ScriptManager.ScriptingLanguageRegistry;
import com.jsblock.scripting.core.rhino.RhinoJSLanguage;
import com.jsblock.scripting.core.rhino.RhinoParsedScript;
import com.jsblock.scripting.core.util.ScriptResourceUtil;
import com.jsblock.scripting.primitive.ParsedScript;
import com.jsblock.scripting.primitive.ScriptContent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Discovers and parses {@code .js} scripts from resource packs ({@code assets/<namespace>/scripts/**})
 * and from the {@code config/jsblock/scripts} directory.
 * @author LX86 (JCM)
 */
public class ScriptResourceLoader {
    private static final String SCRIPT_DIRECTORY = "scripts";
    private static final File CUSTOM_SCRIPT_DIRECTORY = new File("config/jsblock/scripts");

    private final ScriptManager scriptManager;
    private final ExecutorService ioExecutorService;
    private final Map<ResourceLocation, ParsedScript> parsedScripts = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, String> rawScripts = new ConcurrentHashMap<>();

    public ScriptResourceLoader(ScriptManager scriptManager, ExecutorService ioExecutorService) {
        this.scriptManager = scriptManager;
        this.ioExecutorService = ioExecutorService;
    }

    public void reload() {
        ioExecutorService.execute(this::doReload);
    }

    private void doReload() {
        parsedScripts.clear();
        rawScripts.clear();
        Scripting.clearExternalScripts();
        ScriptResourceUtil.resetCache();
        RhinoParsedScript.clearScriptCache();

        if (ClientConfig.getSkipScriptParsing()) {
            scriptManager.getLogger().info("[YJCM Scripting] Script parsing is disabled in the config, skipping.");
            return;
        }

        final Map<ResourceLocation, String> discovered = new LinkedHashMap<>();
        discovered.putAll(readFromResourcePacks());
        discovered.putAll(readFromCustomDirectory());

        rawScripts.putAll(discovered);
        for (final Map.Entry<ResourceLocation, String> entry : discovered.entrySet()) {
            final ParsedScript parsedScript = scriptManager.parseScript(entry.getKey().toString(), "pids",
                    ParsedScript.Configuration.create(Collections.singletonList(new ScriptContent(entry.getKey(), entry.getValue())), com.jsblock.scripting.api.ScriptingLanguage.RHINO_JS, 0), true);
            if (parsedScript != null) {
                parsedScripts.put(entry.getKey(), parsedScript);
            }
        }

        scriptManager.getLogger().info("[YJCM Scripting] Loaded {} script file(s).", parsedScripts.size());
    }

    private Map<ResourceLocation, String> readFromResourcePacks() {
        final Map<ResourceLocation, String> result = new LinkedHashMap<>();
        final ResourceManager resourceManager = Scripting.getResourceManager();
        if (resourceManager == null) return result;

        try {
            final Map<ResourceLocation, Resource> resources = resourceManager
                    .listResources(SCRIPT_DIRECTORY, loc -> loc.getPath().endsWith(".js"));
            for (final Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try {
                    result.put(entry.getKey(), new String(entry.getValue().open().readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    scriptManager.getLogger().error("[YJCM Scripting] Failed to read script {}", entry.getKey(), e);
                }
            }
        } catch (Exception e) {
            scriptManager.getLogger().error("[YJCM Scripting] Failed to enumerate scripts from resource packs", e);
        }
        return result;
    }

    private Map<ResourceLocation, String> readFromCustomDirectory() {
        final Map<ResourceLocation, String> result = new LinkedHashMap<>();
        if (!CUSTOM_SCRIPT_DIRECTORY.isDirectory()) return result;

        final List<File> files = new ArrayList<>();
        collectFiles(CUSTOM_SCRIPT_DIRECTORY, files);
        for (final File file : files) {
            try {
                final String relative = CUSTOM_SCRIPT_DIRECTORY.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                final ResourceLocation location = new ResourceLocation(Scripting.MOD_ID, SCRIPT_DIRECTORY + "/" + relative);
                final String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Scripting.registerExternalScript(location, content);
                result.put(location, content);
            } catch (Exception e) {
                scriptManager.getLogger().error("[YJCM Scripting] Failed to read custom script {}", file, e);
            }
        }
        return result;
    }

    private static void collectFiles(File directory, List<File> output) {
        final File[] children = directory.listFiles();
        if (children == null) return;
        for (final File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, output);
            } else if (child.getName().endsWith(".js")) {
                output.add(child);
            }
        }
    }

    public ParsedScript getParsedScript(ResourceLocation location) {
        return parsedScripts.get(location);
    }

    public String getRawScript(ResourceLocation location) {
        return rawScripts.get(location);
    }

    public Map<ResourceLocation, ParsedScript> getParsedScripts() {
        return parsedScripts;
    }

    public void reset() {
        parsedScripts.clear();
        rawScripts.clear();
    }

    /** Ensure the default scripting language is registered exactly once. */
    public static void registerDefaultLanguage() {
        if (ScriptingLanguageRegistry.get(com.jsblock.scripting.api.ScriptingLanguage.RHINO_JS) == null) {
            ScriptingLanguageRegistry.add(new RhinoJSLanguage());
        }
    }
}
