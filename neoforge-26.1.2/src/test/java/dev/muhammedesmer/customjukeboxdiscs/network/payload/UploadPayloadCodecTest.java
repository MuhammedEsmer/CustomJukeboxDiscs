package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class UploadPayloadCodecTest {
    @Test
    void beginRequestRoundTrips() {
        UploadBeginRequest expected = new UploadBeginRequest(
                "a".repeat(64), 10_000, AudioFormat.OGG, "Track", 42L);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        UploadBeginRequest.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, UploadBeginRequest.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void maximumChunkRoundTripsAndOversizeIsRejected() {
        UUID session = UUID.randomUUID();
        byte[] maximum = new byte[UploadChunk.MAX_BYTES];
        UploadChunk expected = new UploadChunk(session, 42, maximum);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        UploadChunk.STREAM_CODEC.encode(buffer, expected);
        org.junit.jupiter.api.Assertions.assertTrue(buffer.readableBytes() < 32 * 1024,
                "serverbound custom payload must remain below Minecraft's 32 KiB limit");
        UploadChunk decoded = UploadChunk.STREAM_CODEC.decode(buffer);

        assertEquals(session, decoded.sessionId());
        assertEquals(42, decoded.offset());
        assertArrayEquals(maximum, decoded.bytes());
        assertThrows(IllegalArgumentException.class,
                () -> new UploadChunk(session, 0, new byte[UploadChunk.MAX_BYTES + 1]));
        buffer.release();
    }

    @Test
    void finishHashIsBoundedAndValidated() {
        assertThrows(IllegalArgumentException.class,
                () -> new UploadFinish(UUID.randomUUID(), "A".repeat(64)));
    }

    @Test
    void clientResponsesRoundTripStableErrorsAndTracks() {
        TrackReference track = new TrackReference(
                "b".repeat(64), "Track", UUID.randomUUID(), "Player", 1_000, AudioFormat.MP3);
        UploadBeginResponse response = new UploadBeginResponse(
                UploadBeginResponse.Status.ALREADY_PRESENT, UploadError.NONE, null, 0, track);
        UploadResult result = new UploadResult(UploadError.NONE, track);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        UploadBeginResponse.STREAM_CODEC.encode(buffer, response);
        UploadResult.STREAM_CODEC.encode(buffer, result);

        assertEquals(response, UploadBeginResponse.STREAM_CODEC.decode(buffer));
        assertEquals(result, UploadResult.STREAM_CODEC.decode(buffer));
        buffer.release();
    }
}
