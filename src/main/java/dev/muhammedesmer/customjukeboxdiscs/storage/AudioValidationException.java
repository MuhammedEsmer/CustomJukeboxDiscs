package dev.muhammedesmer.customjukeboxdiscs.storage;

import java.io.IOException;

public final class AudioValidationException extends IOException {
    private final Reason reason;

    public AudioValidationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AudioValidationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        SIZE_LIMIT,
        DURATION_LIMIT,
        UNSUPPORTED_FORMAT,
        MALFORMED
    }
}
