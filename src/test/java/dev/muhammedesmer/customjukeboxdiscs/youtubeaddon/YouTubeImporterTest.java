package dev.muhammedesmer.customjukeboxdiscs.youtubeaddon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hoodoo.customjukeboxdiscs.youtube.CommandRunner;
import dev.hoodoo.customjukeboxdiscs.youtube.PlatformTools;
import dev.hoodoo.customjukeboxdiscs.youtube.YouTubeImporter;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class YouTubeImporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsVideosLongerThanTheRequestLimit() {
        YouTubeImporter importer = importer((command, timeout, cancelled) ->
                new CommandRunner.Result(0, "{\"duration\":601,\"is_live\":false}"));

        UrlImportResult result = importer.importTrack(request()).join();

        assertEquals(UrlImportResult.Error.TOO_LONG, result.error());
    }

    @Test
    void downloadsValidatedVideoAudioToTheRequestedDestination() throws Exception {
        CommandRunner runner = (command, timeout, cancelled) -> {
            if (command.contains("--dump-single-json")) {
                return new CommandRunner.Result(0, "{\"duration\":120,\"is_live\":false}");
            }
            int output = command.indexOf("--output") + 1;
            Files.writeString(Path.of(command.get(output).replace("%(ext)s", "mp3")), "fake mp3");
            return new CommandRunner.Result(0, "");
        };
        YouTubeImporter importer = importer(runner);

        UrlImportResult result = importer.importTrack(request()).join();

        assertEquals(UrlImportResult.Error.NONE, result.error());
        assertEquals("fake mp3", Files.readString(temporaryDirectory.resolve("track.part")));
    }

    @Test
    void failedConversionRemovesPartialDownloadFiles() throws Exception {
        CommandRunner runner = (command, timeout, cancelled) -> {
            if (command.contains("--dump-single-json")) {
                return new CommandRunner.Result(0, "{\"duration\":120,\"is_live\":false}");
            }
            int output = command.indexOf("--output") + 1;
            Files.writeString(Path.of(command.get(output).replace("%(ext)s", "webm")), "partial");
            return new CommandRunner.Result(1, "conversion failed");
        };

        UrlImportResult result = importer(runner).importTrack(request()).join();

        assertEquals(UrlImportResult.Error.CONVERSION_FAILED, result.error());
        assertEquals(0L, Files.list(temporaryDirectory).count());
    }

    @Test
    void reportsCommandTimeoutSeparatelyFromDownloadFailure() {
        YouTubeImporter importer = importer((command, timeout, cancelled) ->
                new CommandRunner.Result(-1, ""));

        UrlImportResult result = importer.importTrack(request()).join();

        assertEquals(UrlImportResult.Error.TIMED_OUT, result.error());
    }

    private YouTubeImporter importer(CommandRunner runner) {
        return new YouTubeImporter(
                () -> new PlatformTools(Path.of("yt-dlp"), Path.of("ffmpeg")), runner, 8);
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
