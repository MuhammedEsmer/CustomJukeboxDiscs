package dev.muhammedesmer.customjukeboxdiscs.content.writer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DiscWriterTransactionTest {
    private static final TrackReference TRACK = new TrackReference(
            "a".repeat(64), "Track", UUID.randomUUID(), "Player", 1_000, AudioFormat.MP3);

    @Test
    void onlyCompletesForTheSameReservedBlankDisc() {
        DiscWriterTransaction transaction = new DiscWriterTransaction(5L);

        assertTrue(transaction.canComplete(5L, true, TRACK));
        assertFalse(transaction.canComplete(6L, true, TRACK));
        assertFalse(transaction.canComplete(5L, false, TRACK));
    }
}
