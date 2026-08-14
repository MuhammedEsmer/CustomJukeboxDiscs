package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class TrackReferenceCodecTest {
    private static final UUID UPLOADER = UUID.fromString("12345678-1234-5678-9234-567812345678");
    private static final String HASH = "a".repeat(64);

    @Test
    void persistentCodecRoundTrips() {
        TrackReference expected = validReference();

        JsonElement encoded = TrackReference.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        TrackReference decoded = TrackReference.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(expected, decoded);
    }

    @Test
    void networkCodecRoundTrips() {
        TrackReference expected = validReference();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        TrackReference.STREAM_CODEC.encode(buffer, expected);

        assertEquals(expected, TrackReference.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        buffer.release();
    }

    @ParameterizedTest
    @MethodSource("invalidReferences")
    void constructorRejectsInvalidValues(String hash, String title, String uploaderName, long durationMillis) {
        assertThrows(IllegalArgumentException.class,
                () -> new TrackReference(hash, title, UPLOADER, uploaderName, durationMillis, AudioFormat.OGG));
    }

    @ParameterizedTest
    @MethodSource("invalidReferences")
    void persistentCodecRejectsInvalidValues(String hash, String title, String uploaderName, long durationMillis) {
        String json = """
                {"sha256":"%s","title":"%s","uploader_uuid":"%s","uploader_name":"%s","duration_millis":%d,"format":"ogg"}
                """.formatted(hash, title, UPLOADER, uploaderName, durationMillis);

        assertTrue(TrackReference.CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json)).isError());
    }

    @Test
    void formatCodecRejectsUnsupportedValues() {
        assertTrue(AudioFormat.CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("\"wav\"")).isError());
    }

    @Test
    void titleLimitCountsUnicodeCodePoints() {
        TrackReference reference = new TrackReference(
                HASH, "🎵".repeat(64), UPLOADER, "Player", 1, AudioFormat.OGG);

        assertEquals(64, reference.title().codePointCount(0, reference.title().length()));
        assertThrows(IllegalArgumentException.class, () -> new TrackReference(
                HASH, "🎵".repeat(65), UPLOADER, "Player", 1, AudioFormat.OGG));
    }

    private static Stream<Arguments> invalidReferences() {
        return Stream.of(
                Arguments.of("A".repeat(64), "Title", "Player", 1),
                Arguments.of("a".repeat(63), "Title", "Player", 1),
                Arguments.of("g".repeat(64), "Title", "Player", 1),
                Arguments.of(HASH, "", "Player", 1),
                Arguments.of(HASH, "x".repeat(65), "Player", 1),
                Arguments.of(HASH, "Title", "", 1),
                Arguments.of(HASH, "Title", "x".repeat(17), 1),
                Arguments.of(HASH, "Title", "Player", 0),
                Arguments.of(HASH, "Title", "Player", 600_001));
    }

    private static TrackReference validReference() {
        return new TrackReference(HASH, "My Song", UPLOADER, "Player", 123_456, AudioFormat.MP3);
    }
}
