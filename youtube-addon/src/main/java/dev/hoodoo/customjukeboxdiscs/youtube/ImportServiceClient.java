package dev.hoodoo.customjukeboxdiscs.youtube;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Objects;
import java.util.Base64;

public final class ImportServiceClient implements TrackImportClient {
    private final URI endpoint;
    private final String token;
    private final Duration timeout;
    private final HttpClient http;

    public ImportServiceClient(URI endpoint, String token, Duration timeout) {
        this(endpoint, token, timeout, HttpClient.newBuilder().connectTimeout(timeout).build());
    }

    ImportServiceClient(URI endpoint, String token, Duration timeout, HttpClient http) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.token = Objects.requireNonNull(token, "token");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.http = Objects.requireNonNull(http, "http");
    }

    @Override
    public UrlImportResult importTrack(String videoId, UrlImportRequest request) {
        if (token.isBlank()) return failure(UrlImportResult.Error.TOOLS_UNAVAILABLE);
        Path staged = Path.of(request.destination().toAbsolutePath() + ".mp3");
        try {
            if (request.cancelled().getAsBoolean()) return failure(UrlImportResult.Error.CANCELLED);
            request.progress().accept(UrlImportRequest.Progress.indeterminate(UrlImportRequest.Stage.RESOLVING));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(shorter(timeout, request.timeout()))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body(videoId, request)))
                    .build();
            HttpResponse<InputStream> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                try (InputStream input = response.body()) {
                    return failure(mapError(input.readNBytes(4096)));
                }
            }
            long declaredSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (declaredSize > request.maxBytes()) return failure(UrlImportResult.Error.TOO_LONG);
            String suggestedTitle = response.headers().firstValue("X-CJD-Track-Title-B64")
                    .map(ImportServiceClient::decodeTitle).orElse("");
            request.progress().accept(new UrlImportRequest.Progress(
                    UrlImportRequest.Stage.DOWNLOADING, 0, suggestedTitle));
            UrlImportResult copied = copyBounded(response.body(), staged, request, declaredSize, suggestedTitle);
            if (copied.error() != UrlImportResult.Error.NONE) return copied;
            Files.move(staged, request.destination(), StandardCopyOption.REPLACE_EXISTING);
            return UrlImportResult.success(suggestedTitle);
        } catch (HttpTimeoutException exception) {
            return failure(UrlImportResult.Error.TIMED_OUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(request.cancelled().getAsBoolean()
                    ? UrlImportResult.Error.CANCELLED
                    : UrlImportResult.Error.TIMED_OUT);
        } catch (IOException | RuntimeException exception) {
            return failure(UrlImportResult.Error.DOWNLOAD_FAILED);
        } finally {
            try {
                Files.deleteIfExists(staged);
            } catch (IOException ignored) {
            }
        }
    }

    private static UrlImportResult copyBounded(
            InputStream source, Path staged, UrlImportRequest request, long declaredSize, String suggestedTitle)
            throws IOException {
        try (source; var output = Files.newOutputStream(staged)) {
            byte[] buffer = new byte[16 * 1024];
            long total = 0L;
            int read;
            while ((read = source.read(buffer)) >= 0) {
                if (request.cancelled().getAsBoolean()) return failure(UrlImportResult.Error.CANCELLED);
                total += read;
                if (total > request.maxBytes()) return failure(UrlImportResult.Error.TOO_LONG);
                output.write(buffer, 0, read);
                int percent = declaredSize > 0 ? (int) Math.min(100L, total * 100L / declaredSize) : -1;
                request.progress().accept(new UrlImportRequest.Progress(
                        UrlImportRequest.Stage.DOWNLOADING, percent, suggestedTitle));
            }
        }
        return UrlImportResult.success();
    }

    private static String decodeTitle(String encoded) {
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private static String body(String videoId, UrlImportRequest request) {
        JsonObject json = new JsonObject();
        json.addProperty("videoId", videoId);
        json.addProperty("maxBytes", request.maxBytes());
        json.addProperty("maxDurationSeconds", request.maxDuration().toSeconds());
        return json.toString();
    }

    private static UrlImportResult.Error mapError(byte[] body) {
        try {
            String code = JsonParser.parseString(new String(body, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().get("error").getAsString();
            return switch (code) {
                case "too_long", "too_large" -> UrlImportResult.Error.TOO_LONG;
                case "busy" -> UrlImportResult.Error.QUEUE_FULL;
                default -> UrlImportResult.Error.DOWNLOAD_FAILED;
            };
        } catch (RuntimeException ignored) {
            return UrlImportResult.Error.DOWNLOAD_FAILED;
        }
    }

    private static Duration shorter(Duration first, Duration second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private static UrlImportResult failure(UrlImportResult.Error error) {
        return UrlImportResult.failure(error);
    }
}
