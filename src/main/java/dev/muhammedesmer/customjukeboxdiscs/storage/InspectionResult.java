package dev.muhammedesmer.customjukeboxdiscs.storage;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;

public record InspectionResult(AudioFormat format, String sha256, long byteCount, long durationMillis) {
    public InspectionResult {
        if (byteCount <= 0) {
            throw new IllegalArgumentException("byteCount must be positive");
        }
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
    }
}
