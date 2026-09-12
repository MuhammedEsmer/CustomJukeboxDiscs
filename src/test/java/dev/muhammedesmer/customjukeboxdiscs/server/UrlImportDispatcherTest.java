package dev.muhammedesmer.customjukeboxdiscs.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImporterRegistry;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlTrackImporter;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class UrlImportDispatcherTest {
    @Test
    void supportedUriUsesTheRegisteredImporter() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        AtomicBoolean imported = new AtomicBoolean();
        registry.register(importer(imported, UrlImportResult.success()));
        AtomicBoolean fetchedDirectly = new AtomicBoolean();

        UploadError result = new UrlImportDispatcher(registry)
                .importTo(request(), () -> {
                    fetchedDirectly.set(true);
                    return UploadError.NONE;
                }).join();

        assertEquals(UploadError.NONE, result);
        assertEquals(true, imported.get());
        assertEquals(false, fetchedDirectly.get());
    }

    @Test
    void unsupportedUriFallsBackToTheDirectDownloader() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        AtomicBoolean fetchedDirectly = new AtomicBoolean();

        UploadError result = new UrlImportDispatcher(registry)
                .importTo(request(), () -> {
                    fetchedDirectly.set(true);
                    return UploadError.NONE;
                }).join();

        assertEquals(UploadError.NONE, result);
        assertEquals(true, fetchedDirectly.get());
    }

    @Test
    void mapsImporterFailureToUploadError() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        registry.register(importer(new AtomicBoolean(),
                UrlImportResult.failure(UrlImportResult.Error.TOO_LONG)));

        UploadError result = new UrlImportDispatcher(registry)
                .importTo(request(), () -> UploadError.NONE).join();

        assertEquals(UploadError.DURATION_LIMIT, result);
    }

    private static UrlTrackImporter importer(AtomicBoolean imported, UrlImportResult result) {
        return new UrlTrackImporter() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public boolean supports(URI uri) {
                return "youtube.com".equals(uri.getHost());
            }

            @Override
            public CompletableFuture<UrlImportResult> importTrack(UrlImportRequest request) {
                imported.set(true);
                return CompletableFuture.completedFuture(result);
            }
        };
    }

    private static UrlImportRequest request() {
        return new UrlImportRequest(
                URI.create("https://youtube.com/watch?v=abc"), Path.of("track.part"), UUID.randomUUID(),
                Duration.ofMinutes(5), 1024, () -> false, ignored -> { });
    }
}
