package com.jsblock.scripting.core.util;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * File access for scripts, exposed as the {@code Files} global.<br>
 * {@code read(a, b, ...)} resolves against the game directory, while
 * {@code readData/saveData/deleteData/hasData} use JCM 2.x's private area,
 * {@code <game directory>/data/mtrscripting/}. Every path is confined to its root: unlike the
 * JCM 2.3 implementation, {@code ../../options.txt} is refused instead of resolved.
 * @author LX86 (JCM)
 */
public final class FilesJS {
    private static final String DATA_DIRECTORY = "data";
    private static final String DATA_NAMESPACE = "mtrscripting";

    private FilesJS() {
    }

    /** {@code Files.read(a, b, ...)} — a UTF-8 text file relative to the game directory. */
    public static String read(String... path) throws IOException {
        return readText(gameDirectory(), path);
    }

    /** {@code Files.readData(a, b, ...)} — a UTF-8 text file from this mod's data area. */
    public static String readData(String... path) throws IOException {
        return readText(dataDirectory(), path);
    }

    /** {@code Files.saveData(text, a, b, ...)} — writes UTF-8 text into this mod's data area. */
    public static void saveData(String content, String... path) throws IOException {
        final Path file = resolve(dataDirectory(), path);
        final Path parent = file.getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }
        final Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        java.nio.file.Files.writeString(temporary, content == null ? "" : content, StandardCharsets.UTF_8);
        try {
            java.nio.file.Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            java.nio.file.Files.deleteIfExists(temporary);
        }
    }

    public static void deleteData(String... path) throws IOException {
        java.nio.file.Files.deleteIfExists(resolve(dataDirectory(), path));
    }

    public static boolean hasData(String... path) throws IOException {
        return java.nio.file.Files.exists(resolve(dataDirectory(), path));
    }

    private static String readText(Path root, String... path) throws IOException {
        final Path file = resolve(root, path);
        if (!java.nio.file.Files.isRegularFile(file)) return null;
        return java.nio.file.Files.readString(file, StandardCharsets.UTF_8);
    }

    private static Path gameDirectory() throws IOException {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) {
            throw new IOException("The game directory is not available yet");
        }
        return minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
    }

    private static Path dataDirectory() throws IOException {
        return gameDirectory().resolve(DATA_DIRECTORY).resolve(DATA_NAMESPACE);
    }

    private static Path resolve(Path root, String... path) throws IOException {
        if (path == null || path.length == 0) throw new IOException("A file path is required");
        final StringBuilder joined = new StringBuilder();
        for (final String part : path) {
            if (part == null) throw new IOException("A file path component is null");
            if (joined.length() > 0) joined.append('/');
            joined.append(part);
        }

        final String reference = joined.toString();
        if (reference.isEmpty()) throw new IOException("A file path is required");
        if (reference.indexOf('\\') >= 0) throw new IOException("Backslashes are not allowed in script paths: " + reference);
        if (reference.startsWith("/")) throw new IOException("Absolute paths are not allowed: " + reference);
        for (final String segment : reference.split("/")) {
            if ("..".equals(segment)) throw new IOException("Parent directory references are not allowed: " + reference);
        }

        final Path resolved = root.resolve(reference).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Path escapes the game directory: " + reference);
        return resolved;
    }
}
