package dev.muhammedesmer.customjukeboxdiscs.api.url;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public record UrlImportRequest(
        URI uri,
        Path destination,
        UUID playerId,
        Duration timeout,
        Duration maxDuration,
        long maxBytes,
        BooleanSupplier cancelled,
        Consumer<Progress> progress) {
    public enum Stage { QUEUED, RESOLVING, DOWNLOADING, WRITING }

    public record Progress(Stage stage, int percent, String suggestedTitle) {
        public Progress {
            Objects.requireNonNull(stage, "stage");
            suggestedTitle = suggestedTitle == null ? "" : suggestedTitle;
            if (percent < -1 || percent > 100) throw new IllegalArgumentException("percent must be -1-100");
        }

        public static Progress indeterminate(Stage stage) {
            return new Progress(stage, -1, "");
        }
    }

    public UrlImportRequest {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(maxDuration, "maxDuration");
        Objects.requireNonNull(cancelled, "cancelled");
        Objects.requireNonNull(progress, "progress");
        if (maxBytes < 1) throw new IllegalArgumentException("maxBytes must be positive");
    }
}
