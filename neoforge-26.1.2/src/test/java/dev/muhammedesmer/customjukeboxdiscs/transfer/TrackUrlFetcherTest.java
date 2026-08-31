package dev.muhammedesmer.customjukeboxdiscs.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the byte handling of a link download. The HTTP conversation itself is not exercised here;
 * it needs a real https endpoint and is verified by using the feature against a live server.
 */
final class TrackUrlFetcherTest {
    @TempDir
    Path directory;

    @Test
    void writesABodyThatFitsTheCap() throws IOException {
        Path target = directory.resolve("track.mp3");

        UploadError result = TrackUrlFetcher.copyBounded(new ByteArrayInputStream(new byte[500]), target, 1_000);

        assertEquals(UploadError.NONE, result);
        assertEquals(500, Files.size(target));
    }

    @Test
    void acceptsABodyOfExactlyTheCap() throws IOException {
        Path target = directory.resolve("track.mp3");

        assertEquals(UploadError.NONE,
                TrackUrlFetcher.copyBounded(new ByteArrayInputStream(new byte[1_000]), target, 1_000));
    }

    @Test
    void stopsAsSoonAsTheBodyPassesTheCap() throws IOException {
        Path target = directory.resolve("track.mp3");

        UploadError result = TrackUrlFetcher.copyBounded(
                new ByteArrayInputStream(new byte[2_000_000]), target, 1_000);

        assertEquals(UploadError.SIZE_LIMIT, result);
        assertTrue(Files.size(target) <= 1_000 + 64 * 1024, "the download must not run past the cap");
    }

    @Test
    void treatsAnEmptyBodyAsAFailedDownload() throws IOException {
        Path target = directory.resolve("track.mp3");

        assertEquals(UploadError.URL_FETCH_FAILED,
                TrackUrlFetcher.copyBounded(new ByteArrayInputStream(new byte[0]), target, 1_000));
    }

    @Test
    void refusesALinkTheServerNetworkOwnsUnlessTheOperatorOptedIn() {
        TrackUrlPolicy guarded = new TrackUrlPolicy(java.util.List.of("192.168.1.20"));
        TrackUrlPolicy opened = new TrackUrlPolicy(java.util.List.of("192.168.1.20"), true);

        assertEquals(UploadError.URL_NOT_ALLOWED, guarded.check("https://192.168.1.20/song.mp3"));
        assertEquals(UploadError.NONE, opened.check("https://192.168.1.20/song.mp3"));
    }

    @Test
    void theOptInAlsoGovernsResolvedAddresses() throws java.net.UnknownHostException {
        java.net.InetAddress local = java.net.InetAddress.getByName("10.1.2.3");

        assertTrue(new TrackUrlPolicy(java.util.List.of("host")).refusesAddress(local));
        assertFalse(new TrackUrlPolicy(java.util.List.of("host"), true).refusesAddress(local));
    }

    @Test
    void theOptInNeverWidensTheHostAllowlist() {
        TrackUrlPolicy opened = new TrackUrlPolicy(java.util.List.of("example.com"), true);

        assertEquals(UploadError.URL_NOT_ALLOWED, opened.check("https://192.168.1.20/song.mp3"));
    }
}
