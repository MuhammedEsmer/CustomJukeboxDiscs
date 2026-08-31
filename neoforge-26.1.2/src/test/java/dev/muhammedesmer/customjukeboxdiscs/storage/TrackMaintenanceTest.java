package dev.muhammedesmer.customjukeboxdiscs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class TrackMaintenanceTest {
    private static final UUID OWNER = UUID.fromString("12345678-1234-5678-9234-567812345678");

    @TempDir
    Path worldDirectory;

    private FileTrackStorage storage;
    private TrackCatalogData catalog;
    private TrackMaintenance maintenance;

    @BeforeEach
    void setUp() {
        storage = new FileTrackStorage(worldDirectory);
        catalog = new TrackCatalogData();
        maintenance = new TrackMaintenance(catalog, storage);
    }

    @Test
    void deleteRemovesTheCatalogEntryAndTheAudioFile() throws IOException {
        String hash = "a".repeat(64);
        Path file = writeTrackFile(hash);
        catalog.add(metadata(hash));

        assertTrue(maintenance.delete(hash));

        assertTrue(catalog.find(hash).isEmpty());
        assertFalse(Files.exists(file));
    }

    @Test
    void deleteReportsUnknownTracks() throws IOException {
        assertFalse(maintenance.delete("f".repeat(64)));
    }

    @Test
    void recoveryDeletesLeftOverTemporaryFiles() throws IOException {
        Path temporary = storage.createTemporary(UUID.randomUUID());

        maintenance.recover();

        assertFalse(Files.exists(temporary));
    }

    @Test
    void recoveryReportsCatalogEntriesWithoutAudio() throws IOException {
        String hash = "b".repeat(64);
        catalog.add(metadata(hash));

        TrackMaintenance.RecoveryReport report = maintenance.recover();

        assertEquals(List.of(hash), report.missingAudio());
        assertTrue(catalog.find(hash).isPresent(), "catalog entries must survive recovery");
    }

    @Test
    void recoveryReportsUnreferencedFilesWithoutDeletingThem() throws IOException {
        String hash = "c".repeat(64);
        Path orphan = writeTrackFile(hash);

        TrackMaintenance.RecoveryReport report = maintenance.recover();

        assertEquals(List.of(hash), report.unreferencedAudio());
        assertTrue(Files.exists(orphan), "validated audio must never be deleted automatically");
    }

    @Test
    void recoveryIsSilentWhenStorageAndCatalogAgree() throws IOException {
        String hash = "d".repeat(64);
        writeTrackFile(hash);
        catalog.add(metadata(hash));

        TrackMaintenance.RecoveryReport report = maintenance.recover();

        assertTrue(report.missingAudio().isEmpty());
        assertTrue(report.unreferencedAudio().isEmpty());
    }

    private Path writeTrackFile(String hash) throws IOException {
        Path directory = worldDirectory.resolve("customjukeboxdiscs/tracks").resolve(hash.substring(0, 2));
        Files.createDirectories(directory);
        Path file = directory.resolve(hash + ".ogg");
        Files.writeString(file, "audio");
        return file;
    }

    private static TrackMetadata metadata(String hash) {
        return new TrackMetadata(
                new TrackReference(hash, "Track", OWNER, "Player", 1_000, AudioFormat.OGG),
                100,
                Instant.parse("2026-08-14T00:00:00Z"));
    }
}
