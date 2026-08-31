package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class ControlPayloadCodecTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void playbackPreferenceRoundTrips(boolean enabled) {
        PlaybackPreference expected = new PlaybackPreference(enabled);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        PlaybackPreference.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, PlaybackPreference.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void urlRequestRoundTrips() {
        UrlUploadRequest expected = new UrlUploadRequest("https://example.com/song.mp3", "A track", 99L);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        UrlUploadRequest.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, UrlUploadRequest.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void urlRequestRejectsAnEmptyOrOversizedLink() {
        assertThrows(IllegalArgumentException.class, () -> new UrlUploadRequest("", "A track", 0L));
        assertThrows(IllegalArgumentException.class, () -> new UrlUploadRequest("   ", "A track", 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new UrlUploadRequest("h".repeat(UrlUploadRequest.MAX_URL_LENGTH + 1), "A track", 0L));
    }

    @Test
    void urlRequestRejectsATitleOutsideTheAllowedLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new UrlUploadRequest("https://example.com/song.mp3", "", 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new UrlUploadRequest("https://example.com/song.mp3", "x".repeat(65), 0L));
    }

    @Test
    void urlRequestAcceptsALinkOfExactlyTheMaximumLength() {
        String url = "https://example.com/" + "a".repeat(UrlUploadRequest.MAX_URL_LENGTH - 20);

        assertEquals(UrlUploadRequest.MAX_URL_LENGTH,
                new UrlUploadRequest(url, "A track", 0L).url().length());
    }
}
