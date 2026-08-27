package dev.hoodoo.customjukeboxdiscs.transfer;

public final class UploadLimits {
    private final long maxSourceBytes;
    private final long maxDurationMillis;
    private final long maxTracksPerPlayer;
    private final long maxBytesPerPlayer;
    private final long maxServerBytes;
    private final int maxSessionsPerPlayer;
    private final int chunkBytes;
    private final long uploadBytesPerSecond;
    private final long uploadTimeoutMillis;
    private final boolean mp3Enabled;
    private final boolean oggEnabled;

    public UploadLimits(
            long maxSourceBytes,
            long maxDurationMillis,
            long maxTracksPerPlayer,
            long maxBytesPerPlayer,
            long maxServerBytes,
            int maxSessionsPerPlayer,
            int chunkBytes,
            long uploadBytesPerSecond,
            long uploadTimeoutMillis,
            boolean mp3Enabled,
            boolean oggEnabled) {
        this.maxSourceBytes = maxSourceBytes;
        this.maxDurationMillis = maxDurationMillis;
        this.maxTracksPerPlayer = maxTracksPerPlayer;
        this.maxBytesPerPlayer = maxBytesPerPlayer;
        this.maxServerBytes = maxServerBytes;
        this.maxSessionsPerPlayer = maxSessionsPerPlayer;
        this.chunkBytes = chunkBytes;
        this.uploadBytesPerSecond = uploadBytesPerSecond;
        this.uploadTimeoutMillis = uploadTimeoutMillis;
        this.mp3Enabled = mp3Enabled;
        this.oggEnabled = oggEnabled;
    }

    public static UploadLimits defaultLimits() {
        return new UploadLimits(
                10 * 1024 * 1024L,   // 10 MiB
                600_000L,           // 600 seconds
                20L,                // 20 tracks
                100 * 1024 * 1024L, // 100 MiB
                2L * 1024 * 1024 * 1024L, // 2 GiB
                1,                  // 1 session
                31 * 1024,          // 31 KiB
                512 * 1024L,        // 512 KiB/s
                30_000L,            // 30s timeout
                true,
                true
        );
    }

    public long maxSourceBytes() { return maxSourceBytes; }
    public long maxDurationMillis() { return maxDurationMillis; }
    public long maxTracksPerPlayer() { return maxTracksPerPlayer; }
    public long maxBytesPerPlayer() { return maxBytesPerPlayer; }
    public long maxServerBytes() { return maxServerBytes; }
    public int maxSessionsPerPlayer() { return maxSessionsPerPlayer; }
    public int chunkBytes() { return chunkBytes; }
    public long uploadBytesPerSecond() { return uploadBytesPerSecond; }
    public long uploadTimeoutMillis() { return uploadTimeoutMillis; }
    public boolean mp3Enabled() { return mp3Enabled; }
    public boolean oggEnabled() { return oggEnabled; }
}
