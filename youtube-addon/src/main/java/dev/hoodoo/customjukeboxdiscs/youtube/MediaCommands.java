package dev.hoodoo.customjukeboxdiscs.youtube;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public final class MediaCommands {
    private MediaCommands() {
    }

    public static List<String> metadata(Path ytDlp, URI uri) {
        return List.of(
                ytDlp.toString(), "--ignore-config", "--dump-single-json", "--no-playlist", "--no-warnings",
                "--no-remote-components", "--", uri.toString());
    }

    public static List<String> download(Path ytDlp, Path ffmpeg, URI uri, Path destination, long maxBytes) {
        return List.of(
                ytDlp.toString(),
                "--ignore-config",
                "--no-playlist",
                "--no-warnings",
                "--no-progress",
                "--no-remote-components",
                "--max-filesize",
                Long.toString(maxBytes),
                "--extract-audio",
                "--audio-format",
                "mp3",
                "--audio-quality",
                "0",
                "--ffmpeg-location",
                ffmpeg.toAbsolutePath().toString(),
                "--output",
                destination.toAbsolutePath() + ".%(ext)s",
                "--",
                uri.toString());
    }
}
