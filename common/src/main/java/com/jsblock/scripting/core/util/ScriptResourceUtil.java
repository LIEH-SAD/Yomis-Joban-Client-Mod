package com.jsblock.scripting.core.util;

import com.jsblock.scripting.Scripting;
import com.jsblock.scripting.primitive.ParsedScript;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resource helpers exposed to scripts as {@code Resources} (plus the {@code include} and {@code print} globals).
 * @author Zbx1425 / LX86 (JCM)
 */
public class ScriptResourceUtil {
    private static final Stack<ResourceLocation> scriptLocationStack = new Stack<>();
    private static final Set<String> includedScripts = new HashSet<>();
    private static ParsedScript activeParsingScript;
    private static final Map<String, DataReaderJS> dataCache = new ConcurrentHashMap<>();

    public static void beginParseScript(ParsedScript parsedScript) {
        if (activeParsingScript != null) throw new IllegalStateException("Must call finishParseScript() before beginParseScript()!");
        activeParsingScript = parsedScript;
        includedScripts.clear();
    }

    public static void finishParseScript() {
        activeParsingScript = null;
        includedScripts.clear();
    }

    public static void executeScript(ParsedScript script, ResourceLocation scriptLocation, String scriptContent) {
        scriptLocationStack.push(scriptLocation);
        script.parseScriptContent(scriptContent, scriptLocation.getNamespace() + ":" + scriptLocation.getPath());
        scriptLocationStack.pop();
    }

    /** {@code include("namespace:path/to/script.js")} */
    public static void includeScript(Object pathOrIdentifier) {
        if (activeParsingScript == null) {
            throw new RuntimeException("Cannot use include() in the runtime stage, all scripts have already been parsed at this point!");
        }
        final ResourceLocation identifier = pathOrIdentifier instanceof ResourceLocation ? (ResourceLocation) pathOrIdentifier : idr(pathOrIdentifier.toString());
        /* Including the same file twice in one script would execute its top-level declarations twice,
           which Rhino rejects as a redeclaration. Upstream scripts never rely on that, so skip it. */
        if (!includedScripts.add(identifier.toString())) return;
        final DataReaderJS scriptFile = read(identifier);
        if (scriptFile == null) throw new IllegalStateException(String.format("Failed to include script %s:%s as it is not found.", identifier.getNamespace(), identifier.getPath()));
        executeScript(activeParsingScript, identifier, scriptFile.asString());
    }

    /** {@code print(...)} */
    public static void print(Object... objs) {
        ConsoleJS.logRaw(objs);
    }

    public static ResourceLocation id(String textForm) {
        return textForm.contains(":") ? new ResourceLocation(textForm) : new ResourceLocation("jsblock", textForm);
    }

    public static ResourceLocation id(ResourceLocation id, String suffix) {
        return new ResourceLocation(id.getNamespace(), id.getPath() + suffix);
    }

    /** Resolve a path relative to the script currently being parsed. */
    public static ResourceLocation idr(String textForm) {
        if (scriptLocationStack.empty()) throw new RuntimeException("Cannot use idr()/idRelative() from inside a function, only while the script is being parsed.");
        return resolveRelativePath(scriptLocationStack.peek(), textForm);
    }

    public static DataReaderJS read(String id) {
        return read(id(id));
    }

    public static DataReaderJS read(ResourceLocation id) {
        final String idStr = id.getNamespace() + ":" + id.getPath();
        return cacheOrGet(idStr, () -> {
            final byte[][] fileBytes = new byte[][]{null};
            try (final InputStream stream = Scripting.readResource(id)) {
                if (stream != null) {
                    fileBytes[0] = stream.readAllBytes();
                }
            } catch (Exception e) {
                ConsoleJS.errorRaw("Error while reading data " + idStr);
            }
            return fileBytes[0] != null ? new DataReaderJS(() -> new ByteArrayInputStream(fileBytes[0])) : null;
        });
    }

    public static boolean exist(String id) {
        return exist(id(id));
    }

    public static boolean exist(ResourceLocation id) {
        return Scripting.resourceExists(id);
    }

    public static String readString(ResourceLocation id) {
        final DataReaderJS reader = read(id);
        return reader == null ? null : reader.asString();
    }

    public static void resetCache() {
        dataCache.clear();
    }

    private static DataReaderJS cacheOrGet(String key, Supplier<DataReaderJS> supplier) {
        return dataCache.computeIfAbsent(key, k -> supplier.get());
    }

    private static ResourceLocation resolveRelativePath(ResourceLocation baseFile, String relative) {
        String result = relative.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (result.contains(":")) {
            result = result.replaceAll("[^a-z0-9/.:_-]", "_");
            return new ResourceLocation(result);
        }
        result = result.replaceAll("[^a-z0-9/._-]", "_");
        final String parent = FileSystems.getDefault().getPath(baseFile.getPath()).getParent() == null ? "" : FileSystems.getDefault().getPath(baseFile.getPath()).getParent().toString();
        return new ResourceLocation(baseFile.getNamespace(), FileSystems.getDefault().getPath(parent).resolve(result).normalize().toString().replace('\\', '/'));
    }
}
