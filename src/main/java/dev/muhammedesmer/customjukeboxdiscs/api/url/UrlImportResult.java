package dev.muhammedesmer.customjukeboxdiscs.api.url;

import java.util.Objects;

public record UrlImportResult(Error error, String suggestedTitle) {
    public enum Error {
        NONE,
        UNSUPPORTED_MEDIA,
        TOOLS_UNAVAILABLE,
        QUEUE_FULL,
        TOO_LONG,
        DOWNLOAD_FAILED,
        CONVERSION_FAILED,
        CANCELLED,
        TIMED_OUT
    }

    public UrlImportResult {
        Objects.requireNonNull(error, "error");
        suggestedTitle = suggestedTitle == null ? "" : suggestedTitle;
    }

    public static UrlImportResult success() {
        return success("");
    }

    public static UrlImportResult success(String suggestedTitle) {
        return new UrlImportResult(Error.NONE, suggestedTitle);
    }

    public static UrlImportResult failure(Error error) {
        if (error == Error.NONE) throw new IllegalArgumentException("failure requires an error");
        return new UrlImportResult(error, "");
    }

    public boolean successful() {
        return error == Error.NONE;
    }
}
