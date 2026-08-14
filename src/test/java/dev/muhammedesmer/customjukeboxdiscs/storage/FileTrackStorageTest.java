package dev.muhammedesmer.customjukeboxdiscs.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileTrackStorageTest {
    private static final String HASH = "ab" + "0".repeat(62);

    @TempDir
    Path worldDirectory;

    @Test
    void temporaryAndCommittedPathsStayUnderWorldStorage() throws IOException {
        FileTrackStorage storage = new FileTrackStorage(worldDirectory);
        Path temporary = storage.createTemporary(UUID.randomUUID());
        Files.write(temporary, new byte[] {1, 2, 3});

        Path committed = storage.commit(temporary, HASH, AudioFormat.MP3);

        assertEquals(worldDirectory.resolve("customjukeboxdiscs/tracks/ab/" + HASH + ".mp3"), committed);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(committed));
        assertEquals(committed, storage.find(HASH, AudioFormat.MP3).orElseThrow());
    }

    @Test
    void duplicateCommitKeepsExistingValidatedFile() throws IOException {
        FileTrackStorage storage = new FileTrackStorage(worldDirectory);
        Path first = storage.createTemporary(UUID.randomUUID());
        Files.write(first, new byte[] {1});
        Path committed = storage.commit(first, HASH, AudioFormat.OGG);
        Path duplicate = storage.createTemporary(UUID.randomUUID());
        Files.write(duplicate, new byte[] {2});

        assertEquals(committed, storage.commit(duplicate, HASH, AudioFormat.OGG));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(committed));
        assertFalse(Files.exists(duplicate));
    }

    @Test
    void invalidHashCannotEscapeStorageRoot() {
        FileTrackStorage storage = new FileTrackStorage(worldDirectory);

        assertThrows(IllegalArgumentException.class,
                () -> storage.find("../../outside", AudioFormat.MP3));
    }

    @Test
    void startupDoesNotDeleteUnreferencedValidatedFiles() throws IOException {
        Path orphan = worldDirectory.resolve("customjukeboxdiscs/tracks/ab/" + HASH + ".ogg");
        Files.createDirectories(orphan.getParent());
        Files.write(orphan, new byte[] {1});

        new FileTrackStorage(worldDirectory);

        assertTrue(Files.exists(orphan));
    }
}
