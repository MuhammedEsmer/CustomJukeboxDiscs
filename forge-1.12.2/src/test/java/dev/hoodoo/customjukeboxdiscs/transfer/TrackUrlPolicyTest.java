package dev.hoodoo.customjukeboxdiscs.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

final class TrackUrlPolicyTest {
    @Test
    void allowsWhitelistedDomains() {
        TrackUrlPolicy policy = new TrackUrlPolicy(Arrays.asList("archive.org", "github.io"), false);

        assertEquals(UploadError.NONE, policy.check("https://archive.org/track.mp3"));
        assertEquals(UploadError.NONE, policy.check("https://subdomain.archive.org/audio.ogg"));
        assertEquals(UploadError.NONE, policy.check("https://user.github.io/music.mp3"));

        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check("https://malicious.com/audio.mp3"));
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check("http://archive.org/track.mp3")); // Not HTTPS
        assertEquals(UploadError.URL_NOT_ALLOWED, policy.check("https://127.0.0.1/test.mp3"));
    }

    @Test
    void extractsFormatHintFromUrl() {
        TrackUrlPolicy policy = new TrackUrlPolicy(Collections.singletonList("archive.org"), false);

        assertEquals(AudioFormat.MP3, policy.formatOf("https://archive.org/track.mp3").orElse(null));
        assertEquals(AudioFormat.OGG, policy.formatOf("https://archive.org/track.ogg").orElse(null));
        assertEquals(false, policy.formatOf("https://archive.org/track.wav").isPresent());
    }
}
