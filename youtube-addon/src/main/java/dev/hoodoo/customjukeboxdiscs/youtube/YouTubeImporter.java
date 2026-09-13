package dev.hoodoo.customjukeboxdiscs.youtube;

import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlTrackImporter;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class YouTubeImporter implements UrlTrackImporter, AutoCloseable {
    private final Supplier<? extends TrackImportClient> client;
    private final ThreadPoolExecutor executor;
    private final BooleanSupplier enabled;

    public YouTubeImporter(Supplier<? extends TrackImportClient> client, int maxQueued) {
        this(client, maxQueued, () -> true);
    }

    public YouTubeImporter(
            Supplier<? extends TrackImportClient> client, int maxQueued, BooleanSupplier enabled) {
        this.client = Objects.requireNonNull(client, "client");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        if (maxQueued < 1) throw new IllegalArgumentException("maxQueued must be positive");
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueued), runnable -> {
                    Thread thread = new Thread(runnable, "CustomJukeboxDiscs-YouTube");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public String id() {
        return "customjukeboxdiscsyoutube:youtube";
    }

    @Override
    public boolean supports(URI uri) {
        return enabled.getAsBoolean() && YouTubeUrl.canonicalize(uri).isPresent();
    }

    @Override
    public CompletableFuture<UrlImportResult> importTrack(UrlImportRequest request) {
        CompletableFuture<UrlImportResult> result = new CompletableFuture<>();
        try {
            executor.execute(() -> result.complete(importNow(request)));
        } catch (RejectedExecutionException exception) {
            result.complete(UrlImportResult.failure(UrlImportResult.Error.QUEUE_FULL));
        }
        return result;
    }

    private UrlImportResult importNow(UrlImportRequest request) {
        URI uri = YouTubeUrl.canonicalize(request.uri()).orElse(null);
        if (uri == null) return UrlImportResult.failure(UrlImportResult.Error.UNSUPPORTED_MEDIA);
        try {
            return client.get().importTrack(uri.getRawQuery().substring(2), request);
        } catch (RuntimeException exception) {
            return failed(UrlImportResult.Error.TOOLS_UNAVAILABLE);
        }
    }

    private static UrlImportResult failed(UrlImportResult.Error error) {
        return UrlImportResult.failure(error);
    }


    @Override
    public void close() {
        executor.shutdownNow();
    }
}
