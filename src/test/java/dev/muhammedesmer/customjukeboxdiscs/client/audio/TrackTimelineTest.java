package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TrackTimelineTest {
    private static final long MILLI = 1_000_000L;

    @Test
    void reportsTheAnnouncedOffsetAtTheAnchorInstant() {
        TrackTimeline timeline = TrackTimeline.startingFrom(5_000L * MILLI, 2_000L, 10_000L);

        assertEquals(2_000L, timeline.elapsedMillisAt(5_000L * MILLI));
    }

    @Test
    void advancesTheOffsetWhileTheDownloadRuns() {
        TrackTimeline timeline = TrackTimeline.startingFrom(5_000L * MILLI, 2_000L, 10_000L);

        assertEquals(9_000L, timeline.elapsedMillisAt(12_000L * MILLI));
    }

    @Test
    void reportsRemainingTimeAgainstTheValidatedDuration() {
        TrackTimeline timeline = TrackTimeline.startingFrom(0L, 2_000L, 10_000L);

        assertEquals(8_000L, timeline.remainingMillisAt(0L));
        assertEquals(3_000L, timeline.remainingMillisAt(5_000L * MILLI));
    }

    @Test
    void clampsRemainingTimeAtZeroOnceTheTrackEnded() {
        TrackTimeline timeline = TrackTimeline.startingFrom(0L, 2_000L, 10_000L);

        assertEquals(0L, timeline.remainingMillisAt(60_000L * MILLI));
    }

    @Test
    void finishesOnlyAfterTheValidatedDurationElapsed() {
        TrackTimeline timeline = TrackTimeline.startingFrom(0L, 2_000L, 10_000L);

        assertFalse(timeline.finishedAt(7_999L * MILLI));
        assertTrue(timeline.finishedAt(8_000L * MILLI));
    }
}
