package dev.muhammedesmer.customjukeboxdiscs.transfer;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;

public record FinishUploadResult(UploadError error, TrackReference track) {
    public static FinishUploadResult success(TrackReference track) {
        return new FinishUploadResult(UploadError.NONE, track);
    }

    public static FinishUploadResult failed(UploadError error) {
        return new FinishUploadResult(error, null);
    }
}
