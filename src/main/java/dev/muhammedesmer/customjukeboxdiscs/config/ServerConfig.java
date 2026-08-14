package dev.muhammedesmer.customjukeboxdiscs.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    private static final long MIB = 1024L * 1024L;

    public static final ModConfigSpec SPEC;
    public static final ServerConfig INSTANCE;

    private final ModConfigSpec.LongValue maxSourceBytes;
    private final ModConfigSpec.LongValue maxDurationMillis;
    private final ModConfigSpec.IntValue maxTracksPerPlayer;
    private final ModConfigSpec.LongValue maxBytesPerPlayer;
    private final ModConfigSpec.LongValue maxServerBytes;
    private final ModConfigSpec.IntValue maxSessionsPerPlayer;
    private final ModConfigSpec.IntValue chunkBytes;
    private final ModConfigSpec.IntValue uploadBytesPerSecond;
    private final ModConfigSpec.LongValue uploadTimeoutMillis;
    private final ModConfigSpec.BooleanValue mp3Enabled;
    private final ModConfigSpec.BooleanValue oggEnabled;

    static {
        var configured = new ModConfigSpec.Builder().configure(ServerConfig::new);
        INSTANCE = configured.getLeft();
        SPEC = configured.getRight();
    }

    private ServerConfig(ModConfigSpec.Builder builder) {
        builder.push("limits");
        maxSourceBytes = builder.defineInRange("maxSourceBytes", 10L * MIB, 1L, Integer.MAX_VALUE);
        maxDurationMillis = builder.defineInRange("maxDurationMillis", 600_000L, 1L, 600_000L);
        maxTracksPerPlayer = builder.defineInRange("maxTracksPerPlayer", 20, 1, 10_000);
        maxBytesPerPlayer = builder.defineInRange("maxBytesPerPlayer", 100L * MIB, 1L, Long.MAX_VALUE);
        maxServerBytes = builder.defineInRange("maxServerBytes", 2L * 1024L * MIB, 1L, Long.MAX_VALUE);
        maxSessionsPerPlayer = builder.defineInRange("maxSessionsPerPlayer", 1, 1, 16);
        chunkBytes = builder.defineInRange("chunkBytes", 32 * 1024, 1, 1024 * 1024);
        uploadBytesPerSecond = builder.defineInRange("uploadBytesPerSecond", 512 * 1024, 1, Integer.MAX_VALUE);
        uploadTimeoutMillis = builder.defineInRange("uploadTimeoutMillis", 30_000L, 1L, 3_600_000L);
        builder.pop();

        builder.push("formats");
        mp3Enabled = builder.define("mp3Enabled", true);
        oggEnabled = builder.define("oggEnabled", true);
        builder.pop();
    }

    public Limits snapshot() {
        return new Limits(
                maxSourceBytes.get(),
                maxDurationMillis.get(),
                maxTracksPerPlayer.get(),
                maxBytesPerPlayer.get(),
                maxServerBytes.get(),
                maxSessionsPerPlayer.get(),
                chunkBytes.get(),
                uploadBytesPerSecond.get(),
                uploadTimeoutMillis.get(),
                mp3Enabled.get(),
                oggEnabled.get());
    }

    public record Limits(
            long maxSourceBytes,
            long maxDurationMillis,
            int maxTracksPerPlayer,
            long maxBytesPerPlayer,
            long maxServerBytes,
            int maxSessionsPerPlayer,
            int chunkBytes,
            int uploadBytesPerSecond,
            long uploadTimeoutMillis,
            boolean mp3Enabled,
            boolean oggEnabled) {
        public Limits {
            requirePositive(maxSourceBytes, "maxSourceBytes");
            requirePositive(maxDurationMillis, "maxDurationMillis");
            if (maxDurationMillis > 600_000L) {
                throw new IllegalArgumentException("maxDurationMillis cannot exceed 600000");
            }
            requirePositive(maxTracksPerPlayer, "maxTracksPerPlayer");
            requirePositive(maxBytesPerPlayer, "maxBytesPerPlayer");
            requirePositive(maxServerBytes, "maxServerBytes");
            requirePositive(maxSessionsPerPlayer, "maxSessionsPerPlayer");
            requirePositive(chunkBytes, "chunkBytes");
            requirePositive(uploadBytesPerSecond, "uploadBytesPerSecond");
            requirePositive(uploadTimeoutMillis, "uploadTimeoutMillis");
            if (chunkBytes > maxSourceBytes) {
                throw new IllegalArgumentException("chunkBytes cannot exceed maxSourceBytes");
            }
            if (maxSourceBytes > maxBytesPerPlayer) {
                throw new IllegalArgumentException("maxSourceBytes cannot exceed maxBytesPerPlayer");
            }
            if (maxBytesPerPlayer > maxServerBytes) {
                throw new IllegalArgumentException("maxBytesPerPlayer cannot exceed maxServerBytes");
            }
            if (!mp3Enabled && !oggEnabled) {
                throw new IllegalArgumentException("at least one audio format must be enabled");
            }
        }

        public static Limits defaults() {
            return new Limits(
                    10L * MIB, 600_000L, 20, 100L * MIB, 2L * 1024L * MIB,
                    1, 32 * 1024, 512 * 1024, 30_000L, true, true);
        }

        public Limits withMaxSourceBytes(long value) {
            return new Limits(value, maxDurationMillis, maxTracksPerPlayer, maxBytesPerPlayer, maxServerBytes,
                    maxSessionsPerPlayer, chunkBytes, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withMaxDurationMillis(long value) {
            return new Limits(maxSourceBytes, value, maxTracksPerPlayer, maxBytesPerPlayer, maxServerBytes,
                    maxSessionsPerPlayer, chunkBytes, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withMaxTracksPerPlayer(int value) {
            return new Limits(maxSourceBytes, maxDurationMillis, value, maxBytesPerPlayer, maxServerBytes,
                    maxSessionsPerPlayer, chunkBytes, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withMaxBytesPerPlayer(long value) {
            return new Limits(maxSourceBytes, maxDurationMillis, maxTracksPerPlayer, value, maxServerBytes,
                    maxSessionsPerPlayer, chunkBytes, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withMaxServerBytes(long value) {
            return new Limits(maxSourceBytes, maxDurationMillis, maxTracksPerPlayer, maxBytesPerPlayer, value,
                    maxSessionsPerPlayer, chunkBytes, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withMaxSessionsPerPlayer(int value) {
            return new Limits(maxSourceBytes, maxDurationMillis, maxTracksPerPlayer, maxBytesPerPlayer, maxServerBytes,
                    value, chunkBytes, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withChunkBytes(int value) {
            return new Limits(maxSourceBytes, maxDurationMillis, maxTracksPerPlayer, maxBytesPerPlayer, maxServerBytes,
                    maxSessionsPerPlayer, value, uploadBytesPerSecond, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withUploadBytesPerSecond(int value) {
            return new Limits(maxSourceBytes, maxDurationMillis, maxTracksPerPlayer, maxBytesPerPlayer, maxServerBytes,
                    maxSessionsPerPlayer, chunkBytes, value, uploadTimeoutMillis, mp3Enabled, oggEnabled);
        }

        public Limits withUploadTimeoutMillis(long value) {
            return new Limits(maxSourceBytes, maxDurationMillis, maxTracksPerPlayer, maxBytesPerPlayer, maxServerBytes,
                    maxSessionsPerPlayer, chunkBytes, uploadBytesPerSecond, value, mp3Enabled, oggEnabled);
        }

        private static void requirePositive(long value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
        }
    }
}
