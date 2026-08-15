package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class PlaybackAnchorCodecTest {
    @Test
    void blockAnchorRoundTrips() {
        PlaybackAnchor expected = PlaybackAnchor.atBlock(new BlockPos(12, 64, -30));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        PlaybackAnchor.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, PlaybackAnchor.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void entityAnchorRoundTrips() {
        PlaybackAnchor expected = PlaybackAnchor.onEntity(4711);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        PlaybackAnchor.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, PlaybackAnchor.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void blockAndEntityAnchorsAreNeverEqual() {
        assertEquals(false, PlaybackAnchor.atBlock(BlockPos.ZERO).equals(PlaybackAnchor.onEntity(0)));
    }

    @Test
    void rejectsAnUnknownAnchorKind() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(7);

        assertThrows(IllegalArgumentException.class, () -> PlaybackAnchor.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void jukeboxPlayCarriesTheAnchor() {
        JukeboxPlay expected = new JukeboxPlay(PlaybackAnchor.onEntity(9), track(), 1_500);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        JukeboxPlay.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, JukeboxPlay.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void jukeboxStopCarriesTheAnchor() {
        JukeboxStop expected = new JukeboxStop(PlaybackAnchor.atBlock(new BlockPos(1, 2, 3)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        JukeboxStop.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, JukeboxStop.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    private static TrackReference track() {
        return new TrackReference(
                "a".repeat(64),
                "Track",
                UUID.fromString("12345678-1234-5678-9234-567812345678"),
                "Player",
                5_000,
                AudioFormat.OGG);
    }
}
