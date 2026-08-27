package dev.hoodoo.customjukeboxdiscs.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DownloadQueueTest {
    @Test
    void allowsFirstDownloadAndQueuesSubsequent() {
        DownloadQueue queue = new DownloadQueue(2);
        UUID player = UUID.randomUUID();

        assertEquals(DownloadQueue.Admission.START, queue.submit(player, "hash1"));
        assertEquals(DownloadQueue.Admission.QUEUED, queue.submit(player, "hash2"));
        assertEquals(DownloadQueue.Admission.QUEUED, queue.submit(player, "hash3"));
        assertEquals(DownloadQueue.Admission.REJECTED, queue.submit(player, "hash4"));
        assertEquals(DownloadQueue.Admission.DUPLICATE, queue.submit(player, "hash2"));

        assertEquals("hash2", queue.complete(player, "hash1").orElse(null));
        assertEquals("hash3", queue.complete(player, "hash2").orElse(null));
        assertFalse(queue.complete(player, "hash3").isPresent());
    }
}
