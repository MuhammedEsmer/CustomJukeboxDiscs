package dev.hoodoo.customjukeboxdiscs.network.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LibraryPacketTest {
    @Test
    void bytes_ValidPage_RoundTrips() {
        PacketLibraryPageResponse expected = new PacketLibraryPageResponse(
                1, 2, 21, Collections.singletonList(track()));
        ByteBuf buffer = Unpooled.buffer();

        expected.toBytes(buffer);
        PacketLibraryPageResponse actual = new PacketLibraryPageResponse();
        actual.fromBytes(buffer);

        assertEquals(1, actual.getPage(), "page");
        assertEquals(track(), actual.getTracks().get(0), "track");
        buffer.release();
    }

    @Test
    void constructor_MoreThanTwentyTracks_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new PacketLibraryPageResponse(
                1, 2, 21, Collections.nCopies(21, track())));
    }

    @Test
    void writeRequest_InvalidHash_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new PacketLibraryWriteRequest(hash("A")));
    }

    private static TrackReference track() {
        return new TrackReference(hash("a"), "Track",
                UUID.fromString("12345678-1234-5678-9234-567812345678"), "Player", 1_000L, AudioFormat.MP3);
    }

    private static String hash(String character) {
        return String.join("", Collections.nCopies(64, character));
    }
}
