package dev.muhammedesmer.customjukeboxdiscs.client.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientTrackCacheTest {
    @TempDir Path directory;

    @Test
    void acceptsExactOrderedHashVerifiedDownload() throws Exception {
        byte[] bytes = "audio".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        ClientTrackCache cache = new ClientTrackCache(directory, "example.org:25565", 100);
        cache.begin(hash, bytes.length, AudioFormat.OGG, 100);

        Path stored = cache.append(hash, 0, bytes, true).orElseThrow();

        assertTrue(stored.startsWith(directory.toAbsolutePath().normalize()));
        assertArrayEquals(bytes, Files.readAllBytes(stored));
        assertTrue(cache.find(hash, AudioFormat.OGG).isPresent());
    }

    @Test
    void rejectsWrongOffsetAndDeletesOnCancel() throws Exception {
        String hash = "a".repeat(64);
        ClientTrackCache cache = new ClientTrackCache(directory, "server", 100);
        cache.begin(hash, 2, AudioFormat.MP3, 100);

        assertThrows(java.io.IOException.class, () -> cache.append(hash, 1, new byte[] {1}, false));
        cache.cancel(hash);
        assertTrue(Files.walk(directory).noneMatch(path -> path.getFileName().toString().endsWith(".part")));
    }
}
