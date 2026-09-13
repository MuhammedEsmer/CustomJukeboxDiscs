package dev.muhammedesmer.customjukeboxdiscs.youtubeaddon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.sun.net.httpserver.HttpServer;
import dev.hoodoo.customjukeboxdiscs.youtube.ImportServiceClient;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ImportServiceClientTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void downloadsAuthenticatedMp3Response() throws Exception {
        byte[] audio = "fake mp3".getBytes(StandardCharsets.UTF_8);
        try (TestServer server = server((exchange) -> {
            assertEquals("Bearer secret", exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("POST", exchange.getRequestMethod());
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
            exchange.sendResponseHeaders(200, audio.length);
            exchange.getResponseBody().write(audio);
        })) {
            Path destination = temporaryDirectory.resolve("track.part");

            UrlImportResult result = client(server, "secret").importTrack("xAWDqdpOlu8", request(destination));

            assertEquals(UrlImportResult.Error.NONE, result.error());
            assertEquals("fake mp3", Files.readString(destination));
        }
    }

    @Test
    void rejectsResponseLargerThanImportLimitAndRemovesPartialFile() throws Exception {
        byte[] audio = "audio larger than limit".getBytes(StandardCharsets.UTF_8);
        try (TestServer server = server((exchange) -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, audio.length);
            exchange.getResponseBody().write(audio);
        })) {
            Path destination = temporaryDirectory.resolve("track.part");

            UrlImportResult result = client(server, "secret")
                    .importTrack("xAWDqdpOlu8", request(destination, 4));

            assertEquals(UrlImportResult.Error.TOO_LONG, result.error());
            assertFalse(Files.exists(destination));
        }
    }

    @Test
    void mapsServiceDurationError() throws Exception {
        byte[] body = "{\"error\":\"too_long\"}".getBytes(StandardCharsets.UTF_8);
        try (TestServer server = server((exchange) -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(422, body.length);
            exchange.getResponseBody().write(body);
        })) {
            UrlImportResult result = client(server, "secret")
                    .importTrack("xAWDqdpOlu8", request(temporaryDirectory.resolve("track.part")));

            assertEquals(UrlImportResult.Error.TOO_LONG, result.error());
        }
    }

    private ImportServiceClient client(TestServer server, String token) {
        return new ImportServiceClient(server.uri(), token, Duration.ofSeconds(5));
    }

    private UrlImportRequest request(Path destination) {
        return request(destination, 1024);
    }

    private UrlImportRequest request(Path destination, long maxBytes) {
        return new UrlImportRequest(
                URI.create("https://www.youtube.com/watch?v=xAWDqdpOlu8"),
                destination,
                UUID.randomUUID(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(600),
                maxBytes,
                () -> false,
                ignored -> { });
    }

    private static TestServer server(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/import", handler);
        server.start();
        return new TestServer(server);
    }

    private record TestServer(HttpServer server) implements AutoCloseable {
        URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/import");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
