package dev.hoodoo.customjukeboxdiscs.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TrackTimelineTest {
    @Test
    void tracksElapsedAndRemainingTime() {
        long origin = 1_000_000_000L; // 1 sec
        long duration = 5_000L; // 5 sec
        TrackTimeline timeline = new TrackTimeline(origin, duration);

        long now = 3_000_000_000L; // 3 sec (2 sec elapsed)
        assertEquals(2_000L, timeline.elapsedMillisAt(now));
        assertEquals(3_000L, timeline.remainingMillisAt(now));
        assertFalse(timeline.finishedAt(now));

        long finishedTime = 7_000_000_000L; // 7 sec (6 sec elapsed)
        assertEquals(6_000L, timeline.elapsedMillisAt(finishedTime));
        assertEquals(0L, timeline.remainingMillisAt(finishedTime));
        assertTrue(timeline.finishedAt(finishedTime));
    }
}
