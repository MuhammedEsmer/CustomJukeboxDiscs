package dev.muhammedesmer.customjukeboxdiscs.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class TrackUrlPolicyTest {
    private final TrackUrlPolicy policy = new TrackUrlPolicy(
            List.of("example.com", "cdn.example.org", "10.0.0.5", "127.0.0.1", "169.254.169.254",
                    "172.16.4.4", "192.168.1.20", "localhost", "[::1]"));

    @Test
    void acceptsAnAllowedHostServingAnAudioFile() {
        assertEquals(UploadError.NONE, policy.check("https://example.com/music/song.mp3"));
        assertEquals(UploadError.NONE, policy.check("https://cdn.example.org/a/b/song.ogg"));
    }

    @Test
    void acceptsASubdomainOfAnAllowedHost() {
        assertEquals(UploadError.NONE, policy.check("https://files.example.com/song.mp3"));
    }

    @Test
    void rejectsAHostThatMerelyEndsWithAnAllowedName() {
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check("https://notexample.com/song.mp3"));
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check("https://example.com.evil.net/song.mp3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://example.com/song.mp3",
        "ftp://example.com/song.mp3",
        "file:///etc/passwd",
        "jar:https://example.com/a.jar!/song.mp3",
    })
    void requiresHttps(String url) {
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://localhost/song.mp3",
        "https://127.0.0.1/song.mp3",
        "https://[::1]/song.mp3",
        "https://10.0.0.5/song.mp3",
        "https://192.168.1.20/song.mp3",
        "https://172.16.4.4/song.mp3",
        "https://169.254.169.254/song.mp3",
    })
    void rejectsAddressesInsideTheServerNetwork(String url) {
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check(url));
    }

    @Test
    void acceptsALinkWithoutAFileExtension() {
        // Plenty of download links carry no suffix; the audio inspector decides what the bytes are.
        assertEquals(UploadError.NONE, policy.check("https://example.com/download?id=42"));
    }

    @Test
    void reportsNoFormatHintForALinkWithoutAKnownExtension() {
        assertEquals(java.util.Optional.empty(), policy.formatOf("https://example.com/track.wav"));
    }

    @Test
    void screensResolvedAddressesThatBelongToTheServerNetwork() throws java.net.UnknownHostException {
        assertEquals(true, TrackUrlPolicy.isInternalAddress(java.net.InetAddress.getByName("127.0.0.1")));
        assertEquals(true, TrackUrlPolicy.isInternalAddress(java.net.InetAddress.getByName("10.1.2.3")));
        assertEquals(true, TrackUrlPolicy.isInternalAddress(java.net.InetAddress.getByName("169.254.7.7")));
        assertEquals(false, TrackUrlPolicy.isInternalAddress(java.net.InetAddress.getByName("93.184.216.34")));
    }

    @Test
    void rejectsRubbish() {
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check("not a url"));
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check(""));
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check(null));
    }

    @Test
    void anEmptyAllowlistBlocksEverything() {
        assertEquals(UploadError.URL_NOT_ALLOWED,
                new TrackUrlPolicy(List.of()).check("https://example.com/song.mp3"));
    }

    @Test
    void derivesTheFormatFromTheFileExtension() {
        assertEquals(dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat.MP3,
                policy.formatOf("https://example.com/a/song.MP3").orElseThrow());
        assertEquals(dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat.OGG,
                policy.formatOf("https://example.com/a/song.ogg").orElseThrow());
    }
}
