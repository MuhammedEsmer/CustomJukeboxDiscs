package dev.muhammedesmer.customjukeboxdiscs.storage;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public interface TrackStorage {
    Path createTemporary(UUID sessionId) throws IOException;

    Optional<Path> find(String sha256, AudioFormat format);

    Path commit(Path temporary, String sha256, AudioFormat format) throws IOException;

    void delete(String sha256) throws IOException;
}
