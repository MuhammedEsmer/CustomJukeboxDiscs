package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class LibraryPayloadCodecTest {
    @Test
    void streamCodec_ValidPage_RoundTrips() {
        LibraryPageResponse expected = new LibraryPageResponse(1, 2, 21, List.of(track("a")));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        LibraryPageResponse.STREAM_CODEC.encode(buffer, expected);
        LibraryPageResponse actual = LibraryPageResponse.STREAM_CODEC.decode(buffer);

        assertEquals(expected, actual);
        buffer.release();
    }

    @Test
    void constructor_MoreThanTwentyTracks_ThrowsIllegalArgumentException() {
        List<TrackReference> tracks = java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> track(Integer.toHexString(index)))
                .toList();

        assertThrows(IllegalArgumentException.class, () -> new LibraryPageResponse(1, 2, 21, tracks));
    }

    @Test
    void writeRequest_InvalidHash_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new LibraryWriteRequest("A".repeat(64)));
    }

    private static TrackReference track(String seed) {
        String hash = (seed + "0".repeat(64)).substring(0, 64);
        return new TrackReference(hash, "Track", UUID.fromString("12345678-1234-5678-9234-567812345678"),
                "Player", 1_000L, AudioFormat.MP3);
    }
}
