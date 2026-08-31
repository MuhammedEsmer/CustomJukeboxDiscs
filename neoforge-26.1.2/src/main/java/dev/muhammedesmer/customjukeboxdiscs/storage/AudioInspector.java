package dev.muhammedesmer.customjukeboxdiscs.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

public interface AudioInspector {
    InspectionResult inspect(Path path, long maxBytes, Duration maxDuration) throws IOException;
}
