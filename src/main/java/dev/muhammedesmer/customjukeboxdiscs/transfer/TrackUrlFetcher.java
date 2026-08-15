package dev.muhammedesmer.customjukeboxdiscs.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Downloads a track the operator allowed, straight into the upload staging area.
 *
 * <p>Redirects are refused rather than followed: a redirect is the easy way to send an allowed host's
 * request somewhere else entirely.
 */
public final class TrackUrlFetcher {
    private static final int BUFFER_BYTES = 64 * 1024;

    private final HttpClient client;
    private final TrackUrlPolicy policy;
    private final Duration timeout;

    public TrackUrlFetcher(TrackUrlPolicy policy, Duration timeout) {
        this.policy = policy;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(timeout)
                .build();
    }

    /** {@return {@link UploadError#NONE} when the file was written to {@code destination}} */
    public UploadError download(String url, Path destination, long maxBytes) {
        UploadError allowed = policy.check(url);
        if (allowed != UploadError.NONE) {
            return allowed;
        }
        URI uri = URI.create(url.strip());
        if (refusedByAddress(uri.getHost())) {
            return UploadError.URL_NOT_ALLOWED;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "audio/mpeg, audio/ogg, application/octet-stream")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    return UploadError.URL_FETCH_FAILED;
                }
                long declared = response.headers().firstValueAsLong("content-length").orElse(-1L);
                if (declared > maxBytes) {
                    return UploadError.SIZE_LIMIT;
                }
                return copyBounded(body, destination, maxBytes);
            }
        } catch (IOException exception) {
            return UploadError.URL_FETCH_FAILED;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return UploadError.URL_FETCH_FAILED;
        }
    }

    /** Package private so the size cap can be exercised without standing up a web server. */
    static UploadError copyBounded(InputStream body, Path destination, long maxBytes) throws IOException {
        byte[] buffer = new byte[BUFFER_BYTES];
        long written = 0;
        try (OutputStream output = Files.newOutputStream(destination)) {
            int read;
            while ((read = body.read(buffer)) >= 0) {
                written += read;
                if (written > maxBytes) {
                    return UploadError.SIZE_LIMIT;
                }
                output.write(buffer, 0, read);
            }
        }
        return written == 0 ? UploadError.URL_FETCH_FAILED : UploadError.NONE;
    }

    private boolean refusedByAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (policy.refusesAddress(address)) {
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException exception) {
            return true;
        }
    }
}
