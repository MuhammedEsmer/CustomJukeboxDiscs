package dev.muhammedesmer.customjukeboxdiscs.youtubeaddon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hoodoo.customjukeboxdiscs.youtube.TrackImportClient;
import dev.hoodoo.customjukeboxdiscs.youtube.YouTubeImporter;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class YouTubeImporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsVideosLongerThanTheRequestLimit() {
        YouTubeImporter importer = importer((videoId, request) ->
                UrlImportResult.failure(UrlImportResult.Error.TOO_LONG));

        UrlImportResult result = importer.importTrack(request()).join();

        assertEquals(UrlImportResult.Error.TOO_LONG, result.error());
    }

    @Test
    void delegatesCanonicalVideoIdToServiceClient() {
        String[] received = new String[1];
        YouTubeImporter importer = importer((videoId, request) -> {
            received[0] = videoId;
            return UrlImportResult.success();
        });

        UrlImportResult result = importer.importTrack(request()).join();

        assertEquals(UrlImportResult.Error.NONE, result.error());
        assertEquals("dQw4w9WgXcQ", received[0]);
    }

    @Test
    void preservesServiceFailure() {
        UrlImportResult result = importer((videoId, request) ->
                UrlImportResult.failure(UrlImportResult.Error.DOWNLOAD_FAILED)).importTrack(request()).join();

        assertEquals(UrlImportResult.Error.DOWNLOAD_FAILED, result.error());
    }

    private YouTubeImporter importer(TrackImportClient client) {
        return new YouTubeImporter(() -> client, 8);
    }

    private UrlImportRequest request() {
        return new UrlImportRequest(
                URI.create("https://youtu.be/dQw4w9WgXcQ"),
                temporaryDirectory.resolve("track.part"),
                UUID.randomUUID(),
                Duration.ofSeconds(30),
                Duration.ofSeconds(600),
                1024,
                () -> false,
                ignored -> { });
    }
}
