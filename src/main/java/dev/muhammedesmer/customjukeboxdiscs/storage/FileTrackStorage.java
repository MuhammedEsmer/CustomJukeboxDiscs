package dev.muhammedesmer.customjukeboxdiscs.storage;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class FileTrackStorage implements TrackStorage {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final Path tracksDirectory;
    private final Path temporaryDirectory;

    public FileTrackStorage(Path worldDirectory) {
        Path storageRoot = Objects.requireNonNull(worldDirectory, "worldDirectory")
                .toAbsolutePath()
                .normalize()
                .resolve("customjukeboxdiscs");
        tracksDirectory = storageRoot.resolve("tracks");
        temporaryDirectory = storageRoot.resolve("tmp");
    }

    @Override
    public Path createTemporary(UUID sessionId) throws IOException {
        Files.createDirectories(temporaryDirectory);
        Path temporary = temporaryDirectory.resolve(Objects.requireNonNull(sessionId, "sessionId") + ".part");
        return Files.createFile(temporary);
    }

    @Override
    public Optional<Path> find(String sha256, AudioFormat format) {
        Path path = trackPath(sha256, format);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    @Override
    public Path commit(Path temporary, String sha256, AudioFormat format) throws IOException {
        Path normalizedTemporary = Objects.requireNonNull(temporary, "temporary").toAbsolutePath().normalize();
        if (!normalizedTemporary.startsWith(temporaryDirectory)) {
            throw new IllegalArgumentException("temporary path is outside storage");
        }

        Path target = trackPath(sha256, format);
        Files.createDirectories(target.getParent());
        if (Files.isRegularFile(target)) {
            Files.deleteIfExists(normalizedTemporary);
            return target;
        }
        return Files.move(normalizedTemporary, target, StandardCopyOption.ATOMIC_MOVE);
    }

    @Override
    public void delete(String sha256) throws IOException {
        Files.deleteIfExists(trackPath(sha256, AudioFormat.MP3));
        Files.deleteIfExists(trackPath(sha256, AudioFormat.OGG));
    }

    public void cleanupTemporaryFiles() throws IOException {
        if (!Files.isDirectory(temporaryDirectory)) return;
        try (var files = Files.list(temporaryDirectory)) {
            for (Path path : files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".part")).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private Path trackPath(String sha256, AudioFormat format) {
        requireHash(sha256);
        Objects.requireNonNull(format, "format");
        String extension = format.serializedName();
        return tracksDirectory.resolve(sha256.substring(0, 2)).resolve(sha256 + "." + extension);
    }

    private static void requireHash(String sha256) {
        if (sha256 == null || !SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("sha256 must contain exactly 64 lowercase hexadecimal characters");
        }
    }
}
