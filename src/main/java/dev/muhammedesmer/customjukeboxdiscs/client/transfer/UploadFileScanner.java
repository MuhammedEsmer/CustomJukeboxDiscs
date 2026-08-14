package dev.muhammedesmer.customjukeboxdiscs.client.transfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class UploadFileScanner {
    private UploadFileScanner() {
    }

    public static List<Path> scan(Path uploadDirectory, int limit) throws IOException {
        if (limit < 1) {
            return List.of();
        }
        Files.createDirectories(uploadDirectory);
        try (var files = Files.list(uploadDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(UploadFileScanner::isSupported)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .limit(limit)
                    .toList();
        }
    }

    public static Path resolveSelection(Path uploadDirectory, String fileName) {
        Path root = uploadDirectory.toAbsolutePath().normalize();
        Path selected = root.resolve(fileName).normalize();
        if (!selected.getParent().equals(root) || !isSupported(selected)) {
            throw new IllegalArgumentException("Selection is outside the upload directory or unsupported");
        }
        return selected;
    }

    public static String titleFromFile(String fileName) {
        String cleanName = fileName.strip();
        int extension = cleanName.lastIndexOf('.');
        return sanitizeTitle(extension > 0 ? cleanName.substring(0, extension) : cleanName);
    }

    public static String sanitizeTitle(String title) {
        String compact = title.strip().replaceAll("\\s+", " ");
        int end = compact.offsetByCodePoints(0, Math.min(64, compact.codePointCount(0, compact.length())));
        return compact.substring(0, end);
    }

    private static boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".ogg");
    }
}
