package dev.hoodoo.customjukeboxdiscs.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class TrackUrlFetcher {
    private static final int BUFFER_BYTES = 64 * 1024;

    private final TrackUrlPolicy policy;
    private final Duration timeout;

    public TrackUrlFetcher(TrackUrlPolicy policy, Duration timeout) {
        this.policy = policy;
        this.timeout = timeout;
    }

    public UploadError download(String urlStr, Path destination, long maxBytes) {
        UploadError allowed = policy.check(urlStr);
        if (allowed != UploadError.NONE) {
            return allowed;
        }
        URI uri = URI.create(urlStr.trim());
        if (refusedByAddress(uri.getHost())) {
            return UploadError.URL_NOT_ALLOWED;
        }
        HttpURLConnection connection = null;
        try {
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout((int) timeout.toMillis());
            connection.setReadTimeout((int) timeout.toMillis());
            connection.setRequestProperty("Accept", "audio/mpeg, audio/ogg, application/octet-stream");
            connection.setRequestMethod("GET");
            connection.connect();

            int code = connection.getResponseCode();
            if (code != 200) {
                return UploadError.URL_FETCH_FAILED;
            }

            long contentLength = connection.getContentLengthLong();
            if (contentLength > maxBytes) {
                return UploadError.SIZE_LIMIT;
            }

            try (InputStream input = connection.getInputStream()) {
                return copyBounded(input, destination, maxBytes);
            }
        } catch (IOException exception) {
            return UploadError.URL_FETCH_FAILED;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

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
