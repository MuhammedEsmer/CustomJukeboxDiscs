package dev.hoodoo.customjukeboxdiscs.youtube;

import java.nio.file.Path;
import java.util.Objects;

public record PlatformTools(Path ytDlp, Path ffmpeg) {
    public PlatformTools {
        Objects.requireNonNull(ytDlp, "ytDlp");
        Objects.requireNonNull(ffmpeg, "ffmpeg");
    }
}
