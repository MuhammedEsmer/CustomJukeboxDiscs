package dev.hoodoo.customjukeboxdiscs.storage;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import java.util.Objects;

public final class InspectionResult {
    private final AudioFormat format;
    private final String sha256;
    private final long byteCount;
    private final long durationMillis;

    public InspectionResult(AudioFormat format, String sha256, long byteCount, long durationMillis) {
        if (byteCount <= 0) {
            throw new IllegalArgumentException("byteCount must be positive");
        }
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        this.format = Objects.requireNonNull(format, "format");
        this.sha256 = Objects.requireNonNull(sha256, "sha256");
        this.byteCount = byteCount;
        this.durationMillis = durationMillis;
    }

    public AudioFormat format() { return format; }
    public AudioFormat getFormat() { return format; }
    public String sha256() { return sha256; }
    public String getSha256() { return sha256; }
    public long byteCount() { return byteCount; }
    public long getByteCount() { return byteCount; }
    public long durationMillis() { return durationMillis; }
    public long getDurationMillis() { return durationMillis; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InspectionResult)) return false;
        InspectionResult that = (InspectionResult) o;
        return byteCount == that.byteCount &&
                durationMillis == that.durationMillis &&
                format == that.format &&
                Objects.equals(sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, sha256, byteCount, durationMillis);
    }
}
