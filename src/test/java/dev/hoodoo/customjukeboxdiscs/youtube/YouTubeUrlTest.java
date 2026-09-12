package dev.muhammedesmer.customjukeboxdiscs.youtubeaddon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hoodoo.customjukeboxdiscs.youtube.YouTubeUrl;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

final class YouTubeUrlTest {
    @ParameterizedTest
    @CsvSource({
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ, https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ, https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ, https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ, https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ, https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    })
    void canonicalizesSingleVideoLinks(String input, String expected) {
        assertEquals(URI.create(expected), YouTubeUrl.canonicalize(URI.create(input)).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/playlist?list=PL123",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL123",
            "https://www.youtube.com/live/dQw4w9WgXcQ",
            "https://www.youtube.com/channel/UC123",
            "https://youtu.be/not-valid"
    })
    void rejectsUnsupportedOrAmbiguousLinks(String input) {
        assertTrue(YouTubeUrl.canonicalize(URI.create(input)).isEmpty());
    }
}
