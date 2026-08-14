package dev.muhammedesmer.customjukeboxdiscs.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.muhammedesmer.customjukeboxdiscs.config.ServerConfig;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessMode;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessPolicyData;
import dev.muhammedesmer.customjukeboxdiscs.permission.DefaultAccessService;
import dev.muhammedesmer.customjukeboxdiscs.storage.BoundedAudioInspector;
import dev.muhammedesmer.customjukeboxdiscs.storage.FileTrackStorage;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackCatalogData;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackMetadata;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class UploadManagerTest {
    private static final UUID PLAYER = UUID.fromString("12345678-1234-5678-9234-567812345678");

    @TempDir
    Path worldDirectory;

    private AccessPolicyData policy;
    private AtomicLong nowNanos;
    private TrackCatalogData catalog;
    private UploadManager manager;

    @BeforeEach
    void setUp() {
        policy = new AccessPolicyData();
        policy.setMode(AccessMode.EVERYONE);
        nowNanos = new AtomicLong();
        catalog = new TrackCatalogData();
        manager = new UploadManager(
                new DefaultAccessService(policy),
                new FileTrackStorage(worldDirectory),
                catalog,
                new BoundedAudioInspector(),
                ServerConfig.Limits.defaults(),
                nowNanos::get,
                Runnable::run,
                Runnable::run);
    }

    @Test
    void rejectsUnauthorizedOversizedAndConcurrentBegins() throws IOException {
        policy.setMode(AccessMode.OPS);
        assertEquals(UploadError.PERMISSION_DENIED,
                begin(100, "a".repeat(64), 0).error());

        policy.setMode(AccessMode.EVERYONE);
        assertEquals(UploadError.SIZE_LIMIT,
                begin(ServerConfig.Limits.defaults().maxSourceBytes() + 1, "a".repeat(64), 0).error());

        BeginUploadResult accepted = begin(100, "a".repeat(64), 0);
        assertTrue(accepted.accepted());
        assertEquals(UploadError.ACTIVE_SESSION_LIMIT,
                begin(100, "b".repeat(64), 0).error());
    }

    @Test
    void enforcesChunkBoundsAndExactOffsets() throws IOException {
        BeginUploadResult begin = begin(64_000, "a".repeat(64), 0);

        assertEquals(UploadError.CHUNK_TOO_LARGE,
                manager.append(PLAYER, begin.sessionId(), 0, new byte[32 * 1024 + 1]));
        assertEquals(UploadError.WRONG_OFFSET,
                manager.append(PLAYER, begin.sessionId(), 1, new byte[] {1}));
        assertEquals(UploadError.NONE,
                manager.append(PLAYER, begin.sessionId(), 0, new byte[] {1, 2}));
        assertEquals(UploadError.WRONG_OFFSET,
                manager.append(PLAYER, begin.sessionId(), 0, new byte[] {1, 2}));
    }

    @Test
    void timeoutAndLogoutRemoveTemporaryFiles() throws IOException {
        BeginUploadResult timedOut = begin(100, "a".repeat(64), 0);
        Path firstTemporary = manager.temporaryPath(timedOut.sessionId()).orElseThrow();
        nowNanos.set(ServerConfig.Limits.defaults().uploadTimeoutMillis() * 1_000_000L + 1);

        manager.expireTimedOut();

        assertFalse(Files.exists(firstTemporary));
        BeginUploadResult logout = begin(100, "b".repeat(64), 0);
        Path secondTemporary = manager.temporaryPath(logout.sessionId()).orElseThrow();
        manager.cancelAll(PLAYER);
        assertFalse(Files.exists(secondTemporary));
    }

    @Test
    void hashDisagreementFailsAndCleansTemporaryFile() throws Exception {
        byte[] audio = fixtureBytes("one_second.mp3");
        BeginUploadResult begin = begin(audio.length, "a".repeat(64), 0);
        Path temporary = manager.temporaryPath(begin.sessionId()).orElseThrow();
        appendAll(begin.sessionId(), audio);

        FinishUploadResult result = manager.finish(PLAYER, begin.sessionId(), "a".repeat(64)).join();

        assertEquals(UploadError.HASH_MISMATCH, result.error());
        assertFalse(Files.exists(temporary));
        assertTrue(catalog.tracks().isEmpty());
    }

    @Test
    void validatesCommitsAndCatalogsCompletedUpload() throws Exception {
        byte[] audio = fixtureBytes("one_second.mp3");
        String hash = sha256(audio);
        BeginUploadResult begin = begin(audio.length, hash, 0);
        appendAll(begin.sessionId(), audio);

        FinishUploadResult result = manager.finish(PLAYER, begin.sessionId(), hash).join();

        assertEquals(UploadError.NONE, result.error());
        assertNotNull(result.track());
        assertEquals(hash, result.track().sha256());
        assertEquals(AudioFormat.MP3, result.track().format());
        assertTrue(catalog.find(hash).isPresent());
    }

    @Test
    void changedWriterRejectsBeforePermanentCommit() throws Exception {
        byte[] audio = fixtureBytes("one_second.mp3");
        String hash = sha256(audio);
        BeginUploadResult begin = begin(audio.length, hash, 0);
        appendAll(begin.sessionId(), audio);

        FinishUploadResult result = manager.finish(PLAYER, begin.sessionId(), hash, () -> false).join();

        assertEquals(UploadError.INVALID_WRITER, result.error());
        assertTrue(catalog.tracks().isEmpty());
        assertFalse(Files.exists(worldDirectory.resolve("customjukeboxdiscs/tracks")
                .resolve(hash.substring(0, 2)).resolve(hash + ".mp3")));
    }

    @Test
    void existingServerHashSkipsUpload() throws Exception {
        byte[] audio = fixtureBytes("one_second.mp3");
        String hash = sha256(audio);
        BeginUploadResult first = begin(audio.length, hash, 0);
        appendAll(first.sessionId(), audio);
        manager.finish(PLAYER, first.sessionId(), hash).join();

        BeginUploadResult duplicate = begin(audio.length, hash, 0);

        assertTrue(duplicate.alreadyPresent());
        assertEquals(hash, duplicate.existingTrack().sha256());
    }

    @Test
    void excessiveInitialBurstCancelsSession() throws IOException {
        ServerConfig.Limits limits = new ServerConfig.Limits(
                100, 1_000, 20, 100, 100, 1, 100, 10, 30_000, true, true);
        manager = manager(limits, new FileTrackStorage(worldDirectory));
        BeginUploadResult begin = begin(20, "a".repeat(64), 0);
        Path temporary = manager.temporaryPath(begin.sessionId()).orElseThrow();

        assertEquals(UploadError.RATE_LIMIT,
                manager.append(PLAYER, begin.sessionId(), 0, new byte[11]));
        assertFalse(Files.exists(temporary));
    }

    @Test
    void declaredUploadIsRejectedByPlayerAndServerQuotas() {
        UUID otherPlayer = UUID.fromString("87654321-4321-6789-9234-567812345678");
        catalog.add(metadata("c".repeat(64), PLAYER, 60));
        ServerConfig.Limits playerLimits = new ServerConfig.Limits(
                100, 1_000, 20, 100, 200, 1, 100, 100, 30_000, true, true);
        manager = manager(playerLimits, new FileTrackStorage(worldDirectory));
        assertEquals(UploadError.PLAYER_QUOTA, begin(50, "a".repeat(64), 0).error());

        catalog = new TrackCatalogData();
        catalog.add(metadata("d".repeat(64), otherPlayer, 120));
        ServerConfig.Limits serverLimits = new ServerConfig.Limits(
                100, 1_000, 20, 150, 150, 1, 100, 100, 30_000, true, true);
        manager = manager(serverLimits, new FileTrackStorage(worldDirectory));
        assertEquals(UploadError.SERVER_QUOTA, begin(40, "b".repeat(64), 0).error());
    }

    @Test
    void storageFailureProducesStableError() {
        TrackStorage failing = new TrackStorage() {
            @Override
            public Path createTemporary(UUID sessionId) throws IOException {
                throw new IOException("simulated");
            }

            @Override
            public java.util.Optional<Path> find(String sha256, AudioFormat format) {
                return java.util.Optional.empty();
            }

            @Override
            public Path commit(Path temporary, String sha256, AudioFormat format) throws IOException {
                throw new IOException("simulated");
            }

            @Override
            public void delete(String sha256) {
            }
        };
        manager = manager(ServerConfig.Limits.defaults(), failing);

        assertEquals(UploadError.STORAGE_FAILURE, begin(100, "a".repeat(64), 0).error());
    }

    private BeginUploadResult begin(long bytes, String hash, int permissionLevel) {
        return manager.begin(PLAYER, permissionLevel,
                new BeginUpload(hash, bytes, AudioFormat.MP3, "Track", "Player"));
    }

    private UploadManager manager(ServerConfig.Limits limits, TrackStorage storage) {
        return new UploadManager(
                new DefaultAccessService(policy), storage, catalog, new BoundedAudioInspector(), limits,
                nowNanos::get, Runnable::run, Runnable::run);
    }

    private static TrackMetadata metadata(String hash, UUID owner, long bytes) {
        return new TrackMetadata(
                new dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference(
                        hash, "Track", owner, "Player", 1_000, AudioFormat.MP3),
                bytes,
                Instant.parse("2026-08-14T00:00:00Z"));
    }

    private void appendAll(UUID sessionId, byte[] bytes) {
        int offset = 0;
        while (offset < bytes.length) {
            int length = Math.min(32 * 1024, bytes.length - offset);
            byte[] chunk = java.util.Arrays.copyOfRange(bytes, offset, offset + length);
            assertEquals(UploadError.NONE, manager.append(PLAYER, sessionId, offset, chunk));
            offset += length;
        }
    }

    private static byte[] fixtureBytes(String name) throws Exception {
        try (var input = UploadManagerTest.class.getResourceAsStream("/audio/" + name)) {
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
