package dev.muhammedesmer.customjukeboxdiscs.server;

import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImporterRegistry;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class UrlImportDispatcher {
    private final UrlImporterRegistry registry;

    UrlImportDispatcher(UrlImporterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    CompletableFuture<UploadError> importTo(UrlImportRequest request, Supplier<UploadError> directDownload) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(directDownload, "directDownload");
        return registry.find(request.uri())
                .map(importer -> importer.importTrack(request).thenApply(UrlImportDispatcher::map))
                .orElseGet(() -> CompletableFuture.completedFuture(directDownload.get()))
                .exceptionally(ignored -> UploadError.URL_FETCH_FAILED);
    }

    private static UploadError map(UrlImportResult result) {
        return switch (result.error()) {
            case NONE -> UploadError.NONE;
            case UNSUPPORTED_MEDIA -> UploadError.URL_NOT_ALLOWED;
            case QUEUE_FULL -> UploadError.ANOTHER_UPLOAD_ACTIVE;
            case TOO_LONG -> UploadError.DURATION_LIMIT;
            case CANCELLED -> UploadError.INVALID_WRITER;
            case TOOLS_UNAVAILABLE, DOWNLOAD_FAILED, CONVERSION_FAILED -> UploadError.URL_FETCH_FAILED;
        };
    }
}
