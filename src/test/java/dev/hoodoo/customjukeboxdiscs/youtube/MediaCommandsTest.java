package dev.muhammedesmer.customjukeboxdiscs.youtubeaddon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.hoodoo.customjukeboxdiscs.youtube.MediaCommands;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MediaCommandsTest {
    @Test
    void metadataCommandTreatsTheUrlAsOneArgument() {
        List<String> command = MediaCommands.metadata(
                Path.of("yt-dlp"), URI.create("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));

        assertEquals(List.of(
                "yt-dlp", "--ignore-config", "--dump-single-json", "--no-playlist", "--no-warnings",
                "--no-remote-components", "--",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"), command);
        assertFalse(command.stream().anyMatch(value -> value.contains("cmd /c") || value.contains("sh -c")));
    }

    @Test
    void downloadCommandUsesPinnedOutputAndFfmpegLocation() {
        List<String> command = MediaCommands.download(
                Path.of("yt-dlp"), Path.of("tools/ffmpeg"),
                URI.create("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), Path.of("track.mp3"), 10485760L);

        assertEquals(List.of(
                "yt-dlp", "--ignore-config", "--no-playlist", "--no-warnings", "--no-progress",
                "--no-remote-components", "--max-filesize", "10485760", "--extract-audio", "--audio-format", "mp3",
                "--audio-quality", "0", "--ffmpeg-location", Path.of("tools/ffmpeg").toAbsolutePath().toString(),
                "--output", Path.of("track.mp3").toAbsolutePath() + ".%(ext)s", "--",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"), command);
    }
}
