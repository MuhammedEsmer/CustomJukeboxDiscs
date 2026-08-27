package dev.hoodoo.customjukeboxdiscs.transfer;

import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import java.util.UUID;

public final class BeginUploadResult {
    private final UploadError error;
    private final UUID sessionId;
    private final int chunkBytes;
    private final TrackReference existingTrack;

    public BeginUploadResult(UploadError error, UUID sessionId, int chunkBytes, TrackReference existingTrack) {
        this.error = error != null ? error : UploadError.NONE;
        this.sessionId = sessionId;
        this.chunkBytes = chunkBytes;
        this.existingTrack = existingTrack;
    }

    public static BeginUploadResult accepted(UUID sessionId, int chunkBytes) {
        return new BeginUploadResult(UploadError.NONE, sessionId, chunkBytes, null);
    }

    public static BeginUploadResult existing(TrackReference track) {
        return new BeginUploadResult(UploadError.NONE, null, 0, track);
    }

    public static BeginUploadResult failed(UploadError error) {
        return new BeginUploadResult(error, null, 0, null);
    }

    public UploadError error() {
        return error;
    }

    public UploadError getError() {
        return error;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int chunkBytes() {
        return chunkBytes;
    }

    public int getChunkBytes() {
        return chunkBytes;
    }

    public TrackReference existingTrack() {
        return existingTrack;
    }

    public TrackReference getExistingTrack() {
        return existingTrack;
    }

    public boolean accepted() {
        return error == UploadError.NONE && sessionId != null;
    }

    public boolean alreadyPresent() {
        return error == UploadError.NONE && existingTrack != null;
    }
}
