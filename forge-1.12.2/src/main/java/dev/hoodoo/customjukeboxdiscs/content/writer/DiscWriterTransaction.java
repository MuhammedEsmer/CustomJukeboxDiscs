package dev.hoodoo.customjukeboxdiscs.content.writer;

import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;

public final class DiscWriterTransaction {
    private final long inputFingerprint;

    public DiscWriterTransaction(long inputFingerprint) {
        this.inputFingerprint = inputFingerprint;
    }

    public long getInputFingerprint() {
        return inputFingerprint;
    }

    public boolean canComplete(long currentFingerprint, boolean hasBlankDisc, TrackReference track) {
        return track != null && hasBlankDisc && inputFingerprint == currentFingerprint;
    }
}
