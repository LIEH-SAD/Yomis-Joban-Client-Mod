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
        ResourceLocation identifier;
        if (pathOrIdentifier instanceof ResourceLocation) {
            identifier = Scripting.resolveScriptFile(Scripting.caseFold((ResourceLocation) pathOrIdentifier));
        } else {
            identifier = resolveExisting(pathOrIdentifier.toString());
            /* JCM 2.x resolves includes absolutely; scripts written for older builds of this mod may
               use a path relative to the including file, so fall back to that spelling before giving up. */
            if (!exist(identifier) && !scriptLocationStack.empty()) {
                final ResourceLocation relative = idr(pathOrIdentifier.toString());
                if (exist(relative)) identifier = relative;
            }
            /* A pack may write a folder that does not exist ("pids/scripts" for "pids/script");
               the file name is enough to find it when the namespace holds exactly one of them. */
            identifier = Scripting.resolveScriptFile(identifier);
        }
        /* Including the same file twice in one script would execute its top-level declarations twice,
           which Rhino rejects as a redeclaration. Upstream scripts never rely on that, so skip it.
           The stack check catches a file that includes itself indirectly, which would recurse forever. */
        if (scriptLocationStack.contains(identifier)) return;
        if (!includedScripts.add(identifier.toString())) return;
        final DataReaderJS scriptFile = read(identifier);
        if (scriptFile == null) throw new IllegalStateException(String.format("Failed to include script %s:%s as it is not found.", identifier.getNamespace(), identifier.getPath()));
        executeScript(activeParsingScript, identifier, scriptFile.asString());
    }

    /** {@code print(...)} */
    public static void print(Object... objs) {
        ConsoleJS.logRaw(objs);
    }

    /**
     * Build a resource location the lenient way scripts expect: capitals are folded and characters a
     * {@link ResourceLocation} cannot hold are replaced, so {@code "jsblock:Texture/Gui.png"} resolves
     * instead of throwing. JCM 2.x's {@code ScriptPaths} does the same.
     */
    public static ResourceLocation lenientLocation(String textForm) {
        String text = textForm == null ? "" : textForm.trim().replace('\\', '/');
        String namespace = "jsblock";
        final int colon = text.indexOf(':');
        if (colon >= 0) {
            namespace = text.substring(0, colon);
            text = text.substring(colon + 1);
        }
        return new ResourceLocation(sanitize(namespace, false), sanitize(text, true));
    }

    /** @return the location of an existing resource, tolerating the capitalisation a pack used on disk */
    public static ResourceLocation resolveExisting(String textForm) {
        return Scripting.caseFold(lenientLocation(textForm));
    }

    public static ResourceLocation id(String textForm) {
        return lenientLocation(textForm);
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
        return read(resolveExisting(id));
    }

    public static DataReaderJS read(ResourceLocation id) {
        final ResourceLocation resolved = Scripting.caseFold(id);
        final String idStr = resolved.getNamespace() + ":" + resolved.getPath();
        return cacheOrGet(idStr, () -> {
            final byte[][] fileBytes = new byte[][]{null};
            try (final InputStream stream = Scripting.readResource(resolved)) {
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
        return exist(resolveExisting(id));
    }

    public static boolean exist(ResourceLocation id) {
        return Scripting.resourceExists(Scripting.caseFold(id));
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
        final String text = relative == null ? "" : relative.trim().replace('\\', '/');
        if (text.contains(":")) return lenientLocation(text);
        final String parent = FileSystems.getDefault().getPath(baseFile.getPath()).getParent() == null ? "" : FileSystems.getDefault().getPath(baseFile.getPath()).getParent().toString();
        final String combined = FileSystems.getDefault().getPath(parent).resolve(text.toLowerCase(Locale.ROOT)).normalize().toString().replace('\\', '/');
        return Scripting.caseFold(new ResourceLocation(baseFile.getNamespace(), sanitize(combined, true)));
    }

    /** Replaces the characters a {@link ResourceLocation} cannot hold, so lenient input still parses. */
    private static String sanitize(String text, boolean allowSlash) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = Character.toLowerCase(text.charAt(i));
            final boolean valid = c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '.' || c == '_' || c == '-' || allowSlash && c == '/';
            out.append(valid ? c : '_');
        }
        return out.toString();
    }
}
