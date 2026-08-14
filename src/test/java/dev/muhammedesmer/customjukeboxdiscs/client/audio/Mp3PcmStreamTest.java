package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

class Mp3PcmStreamTest {
    @Test
    void decodesRealMp3IntoBoundedPcmChunksAndClosesTwice() throws Exception {
        InputStream source = getClass().getResourceAsStream("/audio/one_second.mp3");
        Mp3PcmStream stream = new Mp3PcmStream(source);

        var first = stream.read(8_192);

        assertTrue(first.remaining() > 0);
        assertTrue(first.remaining() <= 16_384);
        stream.close();
        assertDoesNotThrow(stream::close);
    }
}
