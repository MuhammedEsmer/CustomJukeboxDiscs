package dev.muhammedesmer.customjukeboxdiscs.content.writer;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;

public record DiscWriterTransaction(long inputFingerprint) {
    public boolean canComplete(long currentFingerprint, boolean hasBlankDisc, TrackReference track) {
        return track != null && hasBlankDisc && inputFingerprint == currentFingerprint;
    }
}
