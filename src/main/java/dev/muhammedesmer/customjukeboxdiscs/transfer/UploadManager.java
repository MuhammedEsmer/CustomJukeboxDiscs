package dev.muhammedesmer.customjukeboxdiscs.transfer;

import dev.muhammedesmer.customjukeboxdiscs.config.ServerConfig;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessDecision;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessService;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessSubject;
import dev.muhammedesmer.customjukeboxdiscs.storage.AudioInspector;
import dev.muhammedesmer.customjukeboxdiscs.storage.AudioValidationException;
import dev.muhammedesmer.customjukeboxdiscs.storage.InspectionResult;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackCatalogData;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackMetadata;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

public final class UploadManager {
    public static final int HARD_MAX_CHUNK_BYTES = 31 * 1024;

    private final AccessService accessService;
    private final TrackStorage storage;
    private final TrackCatalogData catalog;
    private final AudioInspector inspector;
    private final ServerConfig.Limits limits;
    private final LongSupplier nanoTime;
    private final Executor ioExecutor;
    private final Executor serverExecutor;
    private final Map<UUID, UploadSession> sessions = new HashMap<>();

    public UploadManager(
            AccessService accessService,
            TrackStorage storage,
            TrackCatalogData catalog,
            AudioInspector inspector,
            ServerConfig.Limits limits,
            LongSupplier nanoTime,
            Executor ioExecutor,
            Executor serverExecutor) {
        this.accessService = accessService;
        this.storage = storage;
        this.catalog = catalog;
        this.inspector = inspector;
        this.limits = limits;
        this.nanoTime = nanoTime;
        this.ioExecutor = ioExecutor;
        this.serverExecutor = serverExecutor;
    }

    public synchronized BeginUploadResult begin(UUID playerId, int permissionLevel, BeginUpload request) {
        AccessDecision access = accessService.mayUpload(AccessSubject.player(playerId, permissionLevel));
        if (!access.allowed()) {
            return BeginUploadResult.failed(access.reason() == AccessDecision.Reason.DENIED
                    ? UploadError.DENIED_PLAYER
                    : UploadError.PERMISSION_DENIED);
        }
        if (request.declaredBytes() > limits.maxSourceBytes()) {
            return BeginUploadResult.failed(UploadError.SIZE_LIMIT);
        }
        if (!formatEnabled(request.formatHint())) {
            return BeginUploadResult.failed(UploadError.UNSUPPORTED_FORMAT);
        }

        Optional<TrackMetadata> existing = catalog.find(request.clientHash());
        if (existing.isPresent()) {
            return BeginUploadResult.existing(existing.get().reference());
        }
        long activeSessions = sessions.values().stream().filter(session -> session.owner.equals(playerId)).count();
        if (activeSessions >= limits.maxSessionsPerPlayer()) {
            return BeginUploadResult.failed(UploadError.ACTIVE_SESSION_LIMIT);
        }
        UploadError quotaError = quotaError(playerId, request.declaredBytes());
        if (quotaError != UploadError.NONE) {
            return BeginUploadResult.failed(quotaError);
        }

        UUID sessionId = UUID.randomUUID();
        Path temporary = null;
        try {
            temporary = storage.createTemporary(sessionId);
            UploadSession session = new UploadSession(
                    sessionId,
                    playerId,
                    request,
                    temporary,
                    Files.newOutputStream(temporary, StandardOpenOption.WRITE),
                    MessageDigest.getInstance("SHA-256"),
                    nanoTime.getAsLong());
            sessions.put(sessionId, session);
            return BeginUploadResult.accepted(sessionId, Math.min(limits.chunkBytes(), HARD_MAX_CHUNK_BYTES));
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteQuietly(temporary);
            return BeginUploadResult.failed(UploadError.STORAGE_FAILURE);
        }
    }

    public synchronized UploadError append(UUID playerId, UUID sessionId, long offset, byte[] bytes) {
        UploadSession session = sessions.get(sessionId);
        if (session == null || !session.owner.equals(playerId)) {
            return UploadError.SESSION_NOT_FOUND;
        }
        long now = nanoTime.getAsLong();
        if (timedOut(session, now)) {
            cancelSession(session);
            return UploadError.TIMEOUT;
        }
        if (bytes == null || bytes.length == 0 || bytes.length > Math.min(limits.chunkBytes(), HARD_MAX_CHUNK_BYTES)) {
            return UploadError.CHUNK_TOO_LARGE;
        }
        if (offset != session.receivedBytes || session.receivedBytes + bytes.length > session.request.declaredBytes()) {
            return UploadError.WRONG_OFFSET;
        }
        long elapsedNanos = Math.max(0, now - session.startedNanos);
        long allowedBytes = limits.uploadBytesPerSecond()
                + elapsedNanos * limits.uploadBytesPerSecond() / 1_000_000_000L;
        if (session.receivedBytes + bytes.length > allowedBytes) {
            cancelSession(session);
            return UploadError.RATE_LIMIT;
        }

        try {
            session.append(bytes, now);
            return UploadError.NONE;
        } catch (IOException exception) {
            cancelSession(session);
            return UploadError.STORAGE_FAILURE;
        }
    }

    public CompletableFuture<FinishUploadResult> finish(UUID playerId, UUID sessionId, String clientHash) {
        UploadSession session;
        synchronized (this) {
            session = sessions.remove(sessionId);
        }
        if (session == null || !session.owner.equals(playerId)) {
            return CompletableFuture.completedFuture(FinishUploadResult.failed(UploadError.SESSION_NOT_FOUND));
        }
        if (timedOut(session, nanoTime.getAsLong())) {
            cleanup(session);
            return CompletableFuture.completedFuture(FinishUploadResult.failed(UploadError.TIMEOUT));
        }
        if (session.receivedBytes != session.request.declaredBytes()) {
            cleanup(session);
            return CompletableFuture.completedFuture(FinishUploadResult.failed(UploadError.INCOMPLETE_UPLOAD));
        }
        try {
            session.close();
        } catch (IOException exception) {
            cleanup(session);
            return CompletableFuture.completedFuture(FinishUploadResult.failed(UploadError.STORAGE_FAILURE));
        }

        String calculatedHash = HexFormat.of().formatHex(session.digest.digest());
        if (!calculatedHash.equals(clientHash) || !calculatedHash.equals(session.request.clientHash())) {
            cleanup(session);
            return CompletableFuture.completedFuture(FinishUploadResult.failed(UploadError.HASH_MISMATCH));
        }

        return CompletableFuture.supplyAsync(() -> inspect(session), ioExecutor)
                .thenApplyAsync(prepared -> finalizeUpload(session, prepared), serverExecutor)
                .exceptionally(exception -> {
                    cleanup(session);
                    return FinishUploadResult.failed(UploadError.STORAGE_FAILURE);
                });
    }

    public synchronized void cancelAll(UUID playerId) {
        sessions.values().stream()
                .filter(session -> session.owner.equals(playerId))
                .toList()
                .forEach(this::cancelSession);
    }

    public synchronized void expireTimedOut() {
        long now = nanoTime.getAsLong();
        sessions.values().stream().filter(session -> timedOut(session, now)).toList().forEach(this::cancelSession);
    }

    synchronized Optional<Path> temporaryPath(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId)).map(session -> session.temporaryPath);
    }

    private PreparedUpload inspect(UploadSession session) {
        try {
            InspectionResult inspection = inspector.inspect(
                    session.temporaryPath,
                    limits.maxSourceBytes(),
                    Duration.ofMillis(limits.maxDurationMillis()));
            if (!formatEnabled(inspection.format())) {
                return PreparedUpload.failed(UploadError.UNSUPPORTED_FORMAT);
            }
            if (!inspection.sha256().equals(session.request.clientHash())) {
                return PreparedUpload.failed(UploadError.HASH_MISMATCH);
            }
            TrackReference reference = new TrackReference(
                    inspection.sha256(),
                    session.request.title(),
                    session.owner,
                    session.request.uploaderName(),
                    inspection.durationMillis(),
                    inspection.format());
            return PreparedUpload.success(reference, inspection.byteCount());
        } catch (AudioValidationException exception) {
            return PreparedUpload.failed(switch (exception.reason()) {
                case SIZE_LIMIT -> UploadError.SIZE_LIMIT;
                case DURATION_LIMIT -> UploadError.DURATION_LIMIT;
                case UNSUPPORTED_FORMAT -> UploadError.UNSUPPORTED_FORMAT;
                case MALFORMED -> UploadError.MALFORMED_AUDIO;
            });
        } catch (IOException | RuntimeException exception) {
            return PreparedUpload.failed(UploadError.STORAGE_FAILURE);
        }
    }

    private FinishUploadResult finalizeUpload(UploadSession session, PreparedUpload prepared) {
        if (prepared.error != UploadError.NONE) {
            cleanup(session);
            return FinishUploadResult.failed(prepared.error);
        }
        UploadError quotaError = quotaError(session.owner, prepared.byteCount);
        if (quotaError != UploadError.NONE) {
            cleanup(session);
            return FinishUploadResult.failed(quotaError);
        }
        try {
            storage.commit(session.temporaryPath, prepared.reference.sha256(), prepared.reference.format());
            catalog.add(new TrackMetadata(prepared.reference, prepared.byteCount, Instant.now()));
            return FinishUploadResult.success(prepared.reference);
        } catch (IOException | RuntimeException exception) {
            cleanup(session);
            return FinishUploadResult.failed(UploadError.STORAGE_FAILURE);
        }
    }

    private UploadError quotaError(UUID playerId, long incomingBytes) {
        if (catalog.trackCount(playerId) >= limits.maxTracksPerPlayer()
                || catalog.byteCount(playerId) + incomingBytes > limits.maxBytesPerPlayer()) {
            return UploadError.PLAYER_QUOTA;
        }
        if (catalog.totalByteCount() + incomingBytes > limits.maxServerBytes()) {
            return UploadError.SERVER_QUOTA;
        }
        return UploadError.NONE;
    }

    private boolean formatEnabled(AudioFormat format) {
        return switch (format) {
            case MP3 -> limits.mp3Enabled();
            case OGG -> limits.oggEnabled();
        };
    }

    private boolean timedOut(UploadSession session, long now) {
        return now - session.lastActivityNanos > limits.uploadTimeoutMillis() * 1_000_000L;
    }

    private synchronized void cancelSession(UploadSession session) {
        sessions.remove(session.id);
        cleanup(session);
    }

    private static void cleanup(UploadSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
        deleteQuietly(session.temporaryPath);
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record PreparedUpload(UploadError error, TrackReference reference, long byteCount) {
        private static PreparedUpload success(TrackReference reference, long byteCount) {
            return new PreparedUpload(UploadError.NONE, reference, byteCount);
        }

        private static PreparedUpload failed(UploadError error) {
            return new PreparedUpload(error, null, 0);
        }
    }
}
