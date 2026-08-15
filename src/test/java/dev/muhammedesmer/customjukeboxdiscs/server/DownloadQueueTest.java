package dev.muhammedesmer.customjukeboxdiscs.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DownloadQueueTest {
    private static final UUID PLAYER = UUID.fromString("12345678-1234-5678-1234-567812345678");
    private static final UUID OTHER = UUID.fromString("87654321-4321-6789-9234-567812345678");

    private final DownloadQueue queue = new DownloadQueue(2);

    @Test
    void startsTheFirstRequestImmediately() {
        assertEquals(DownloadQueue.Admission.START, queue.submit(PLAYER, hash('a')));
    }

    @Test
    void queuesFurtherRequestsWhileOneIsActive() {
        queue.submit(PLAYER, hash('a'));

        assertEquals(DownloadQueue.Admission.QUEUED, queue.submit(PLAYER, hash('b')));
    }

    @Test
    void startsTheQueuedRequestWhenTheActiveOneCompletes() {
        queue.submit(PLAYER, hash('a'));
        queue.submit(PLAYER, hash('b'));

        assertEquals(Optional.of(hash('b')), queue.complete(PLAYER, hash('a')));
    }

    @Test
    void servesQueuedRequestsInArrivalOrder() {
        queue.submit(PLAYER, hash('a'));
        queue.submit(PLAYER, hash('b'));
        queue.submit(PLAYER, hash('c'));

        assertEquals(Optional.of(hash('b')), queue.complete(PLAYER, hash('a')));
        assertEquals(Optional.of(hash('c')), queue.complete(PLAYER, hash('b')));
        assertEquals(Optional.empty(), queue.complete(PLAYER, hash('c')));
    }

    @Test
    void ignoresARepeatedRequestForTheSameTrack() {
        queue.submit(PLAYER, hash('a'));
        queue.submit(PLAYER, hash('b'));

        assertEquals(DownloadQueue.Admission.DUPLICATE, queue.submit(PLAYER, hash('a')));
        assertEquals(DownloadQueue.Admission.DUPLICATE, queue.submit(PLAYER, hash('b')));
        assertEquals(Optional.of(hash('b')), queue.complete(PLAYER, hash('a')));
    }

    @Test
    void rejectsRequestsBeyondTheQueueDepth() {
        queue.submit(PLAYER, hash('a'));
        queue.submit(PLAYER, hash('b'));
        queue.submit(PLAYER, hash('c'));

        assertEquals(DownloadQueue.Admission.REJECTED, queue.submit(PLAYER, hash('d')));
    }

    @Test
    void keepsPlayersIndependent() {
        queue.submit(PLAYER, hash('a'));

        assertEquals(DownloadQueue.Admission.START, queue.submit(OTHER, hash('a')));
    }

    @Test
    void ignoresCompletionOfATrackThatIsNotActive() {
        queue.submit(PLAYER, hash('a'));

        assertEquals(Optional.empty(), queue.complete(PLAYER, hash('z')));
        assertEquals(DownloadQueue.Admission.DUPLICATE, queue.submit(PLAYER, hash('a')));
    }

    @Test
    void forgettingAPlayerClearsActiveAndQueuedWork() {
        queue.submit(PLAYER, hash('a'));
        queue.submit(PLAYER, hash('b'));

        queue.forget(PLAYER);

        assertEquals(DownloadQueue.Admission.START, queue.submit(PLAYER, hash('b')));
        assertTrue(queue.complete(PLAYER, hash('b')).isEmpty());
    }

    private static String hash(char fill) {
        return String.valueOf(fill).repeat(64);
    }
}
