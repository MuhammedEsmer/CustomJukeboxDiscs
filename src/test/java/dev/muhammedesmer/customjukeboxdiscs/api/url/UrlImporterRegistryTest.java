package dev.muhammedesmer.customjukeboxdiscs.api.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class UrlImporterRegistryTest {
    @Test
    void findsTheOnlyImporterThatSupportsTheUri() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        UrlTrackImporter audio = importer("audio", "audio.example");
        UrlTrackImporter youtube = importer("youtube", "youtube.com");
        registry.register(audio);
        registry.register(youtube);

        assertEquals(youtube, registry.find(URI.create("https://youtube.com/watch?v=abc")).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoImporterSupportsTheUri() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        registry.register(importer("youtube", "youtube.com"));

        assertTrue(registry.find(URI.create("https://audio.example/song.mp3")).isEmpty());
    }

    @Test
    void rejectsDuplicateImporterIds() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        registry.register(importer("youtube", "youtube.com"));

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(importer("youtube", "youtu.be")));
    }

    @Test
    void rejectsAmbiguousImporterClaims() {
        UrlImporterRegistry registry = new UrlImporterRegistry();
        registry.register(importer("first", "youtube.com"));
        registry.register(importer("second", "youtube.com"));

        assertThrows(IllegalStateException.class,
                () -> registry.find(URI.create("https://youtube.com/watch?v=abc")));
    }

    private static UrlTrackImporter importer(String id, String host) {
        return new UrlTrackImporter() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public boolean supports(URI uri) {
                return host.equals(uri.getHost());
            }

            @Override
            public CompletableFuture<UrlImportResult> importTrack(UrlImportRequest request) {
                return CompletableFuture.completedFuture(UrlImportResult.success());
            }
        };
    }
}
