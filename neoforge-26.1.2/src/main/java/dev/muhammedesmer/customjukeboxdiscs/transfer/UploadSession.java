package dev.muhammedesmer.customjukeboxdiscs.transfer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;

final class UploadSession {
    final UUID id;
    final UUID owner;
    final BeginUpload request;
    final Path temporaryPath;
    final OutputStream output;
    final MessageDigest digest;
    final long startedNanos;
    long lastActivityNanos;
    long receivedBytes;

    UploadSession(
            UUID id,
            UUID owner,
            BeginUpload request,
            Path temporaryPath,
            OutputStream output,
            MessageDigest digest,
            long nowNanos) {
        this.id = id;
        this.owner = owner;
        this.request = request;
        this.temporaryPath = temporaryPath;
        this.output = output;
        this.digest = digest;
        this.startedNanos = nowNanos;
        this.lastActivityNanos = nowNanos;
    }

    void append(byte[] bytes, long nowNanos) throws IOException {
        output.write(bytes);
        digest.update(bytes);
        receivedBytes += bytes.length;
        lastActivityNanos = nowNanos;
    }

    void close() throws IOException {
        output.close();
    }
}
