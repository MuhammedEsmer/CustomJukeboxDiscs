package dev.hoodoo.customjukeboxdiscs.transfer;

import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;

public final class FinishUploadResult {
    private final UploadError error;
    private final TrackReference track;

    public FinishUploadResult(UploadError error, TrackReference track) {
        this.error = error != null ? error : UploadError.NONE;
        this.track = track;
    }

    public static FinishUploadResult success(TrackReference track) {
        return new FinishUploadResult(UploadError.NONE, track);
    }

    public static FinishUploadResult failed(UploadError error) {
        return new FinishUploadResult(error, null);
    }

    public UploadError error() { return error; }
    public UploadError getError() { return error; }
    public TrackReference track() { return track; }
    public TrackReference getTrack() { return track; }
}
