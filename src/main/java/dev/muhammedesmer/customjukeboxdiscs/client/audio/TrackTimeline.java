package dev.muhammedesmer.customjukeboxdiscs.client.audio;

/**
 * Anchors a track to the wall clock so the playback offset stays correct while the audio is still downloading.
 */
public record TrackTimeline(long originNanos, long durationMillis) {
    public static TrackTimeline startingFrom(long nowNanos, long elapsedMillis, long durationMillis) {
        return new TrackTimeline(nowNanos - Math.max(0, elapsedMillis) * 1_000_000L, durationMillis);
    }

    public long elapsedMillisAt(long nowNanos) {
        return Math.max(0, (nowNanos - originNanos) / 1_000_000L);
    }

    public long remainingMillisAt(long nowNanos) {
        return Math.max(0, durationMillis - elapsedMillisAt(nowNanos));
    }

    public boolean finishedAt(long nowNanos) {
        return elapsedMillisAt(nowNanos) >= durationMillis;
    }
}
