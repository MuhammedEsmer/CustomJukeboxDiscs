package dev.muhammedesmer.customjukeboxdiscs.api.url;

import java.util.Objects;

public record UrlImportResult(Error error) {
    public enum Error {
        NONE,
        UNSUPPORTED_MEDIA,
        TOOLS_UNAVAILABLE,
        QUEUE_FULL,
        TOO_LONG,
        DOWNLOAD_FAILED,
        CONVERSION_FAILED,
        CANCELLED
    }

    public UrlImportResult {
        Objects.requireNonNull(error, "error");
    }

    public static UrlImportResult success() {
        return new UrlImportResult(Error.NONE);
    }

    public static UrlImportResult failure(Error error) {
        if (error == Error.NONE) throw new IllegalArgumentException("failure requires an error");
        return new UrlImportResult(error);
    }

    public boolean successful() {
        return error == Error.NONE;
    }
}
