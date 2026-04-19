package mc.toolkitmc.macroengine.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * File I/O utilities for MacroEngine generators.
 */
public final class FileUtil {

    private FileUtil() {}

    public static void mkdirs(Path dir) throws IOException {
        Files.createDirectories(dir);
    }

    /**
     * Write text content to a file (creates parent dirs automatically).
     * Skips writing if the file already exists to avoid overwriting user edits.
     */
    public static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) return; // don't overwrite existing files
        Files.writeString(file, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW);
    }

    /**
     * Write JSON content (alias for write — JSON is just text).
     */
    public static void writeJson(Path file, String json) throws IOException {
        write(file, json);
    }

    /**
     * Force-write even if file exists (used for regeneration).
     */
    public static void overwrite(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
