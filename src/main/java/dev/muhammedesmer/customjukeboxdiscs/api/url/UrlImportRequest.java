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
        long maxBytes,
        BooleanSupplier cancelled,
        Consumer<Stage> progress) {
    public enum Stage { QUEUED, DOWNLOADING, CONVERTING }

    public UrlImportRequest {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(cancelled, "cancelled");
        Objects.requireNonNull(progress, "progress");
        if (maxBytes < 1) throw new IllegalArgumentException("maxBytes must be positive");
    }
}
