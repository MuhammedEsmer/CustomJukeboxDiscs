package dev.muhammedesmer.customjukeboxdiscs.transfer;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.util.UUID;

public record BeginUploadResult(
        UploadError error,
        UUID sessionId,
        int chunkBytes,
        TrackReference existingTrack) {
    public static BeginUploadResult accepted(UUID sessionId, int chunkBytes) {
        return new BeginUploadResult(UploadError.NONE, sessionId, chunkBytes, null);
    }

    public static BeginUploadResult existing(TrackReference track) {
        return new BeginUploadResult(UploadError.NONE, null, 0, track);
    }

    public static BeginUploadResult failed(UploadError error) {
        return new BeginUploadResult(error, null, 0, null);
    }

    public boolean accepted() {
        return error == UploadError.NONE && sessionId != null;
    }

    public boolean alreadyPresent() {
        return error == UploadError.NONE && existingTrack != null;
    }
}
