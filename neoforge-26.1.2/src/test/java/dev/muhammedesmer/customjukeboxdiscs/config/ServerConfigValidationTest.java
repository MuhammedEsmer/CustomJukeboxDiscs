package dev.muhammedesmer.customjukeboxdiscs.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ServerConfigValidationTest {
    @Test
    void approvedDefaultsAreStable() {
        ServerConfig.Limits server = ServerConfig.Limits.defaults();
        ClientConfig.Limits client = ClientConfig.Limits.defaults();

        assertEquals(10L * 1024 * 1024, server.maxSourceBytes());
        assertEquals(600_000L, server.maxDurationMillis());
        assertEquals(20, server.maxTracksPerPlayer());
        assertEquals(100L * 1024 * 1024, server.maxBytesPerPlayer());
        assertEquals(2L * 1024 * 1024 * 1024, server.maxServerBytes());
        assertEquals(1, server.maxSessionsPerPlayer());
        assertEquals(32 * 1024, server.chunkBytes());
        assertEquals(512 * 1024, server.uploadBytesPerSecond());
        assertEquals(30_000L, server.uploadTimeoutMillis());
        assertEquals(512L * 1024 * 1024, client.maxCacheBytes());
        assertEquals(1_000, client.maxUploadScanFiles());
    }

    @Test
    void validBoundaryValuesAreAccepted() {
        assertDoesNotThrow(() -> new ServerConfig.Limits(1, 1, 1, 1, 1, 1, 1, 1, 1, true, false));
        assertDoesNotThrow(() -> new ClientConfig.Limits(1, 1));
    }

    @Test
    void nonPositiveNumericLimitsAreRejected() {
        ServerConfig.Limits defaults = ServerConfig.Limits.defaults();

        assertThrows(IllegalArgumentException.class, () -> defaults.withMaxSourceBytes(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withMaxDurationMillis(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withMaxTracksPerPlayer(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withMaxBytesPerPlayer(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withMaxServerBytes(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withMaxSessionsPerPlayer(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withChunkBytes(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withUploadBytesPerSecond(0));
        assertThrows(IllegalArgumentException.class, () -> defaults.withUploadTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> new ClientConfig.Limits(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ClientConfig.Limits(1, 0));
    }

    @Test
    void relatedLimitsMustRemainCoherent() {
        ServerConfig.Limits defaults = ServerConfig.Limits.defaults();

        assertThrows(IllegalArgumentException.class,
                () -> defaults.withChunkBytes((int) defaults.maxSourceBytes() + 1));
        assertThrows(IllegalArgumentException.class,
                () -> defaults.withMaxBytesPerPlayer(defaults.maxServerBytes() + 1));
        assertThrows(IllegalArgumentException.class,
                () -> defaults.withMaxSourceBytes(defaults.maxBytesPerPlayer() + 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig.Limits(1, 1, 1, 1, 1, 1, 1, 1, 1, false, false));
    }
}
