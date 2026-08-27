package dev.hoodoo.customjukeboxdiscs.client.audio;

public final class TrackTimeline {
    private final long originNanos;
    private final long durationMillis;

    public TrackTimeline(long originNanos, long durationMillis) {
        this.originNanos = originNanos;
        this.durationMillis = durationMillis;
    }

    public static TrackTimeline startingFrom(long nowNanos, long elapsedMillis, long durationMillis) {
        return new TrackTimeline(nowNanos - Math.max(0, elapsedMillis) * 1_000_000L, durationMillis);
    }

    public long originNanos() {
        return originNanos;
    }

    public long durationMillis() {
        return durationMillis;
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
