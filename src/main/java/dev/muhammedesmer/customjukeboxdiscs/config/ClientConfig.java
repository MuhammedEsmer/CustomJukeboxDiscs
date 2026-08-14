package dev.muhammedesmer.customjukeboxdiscs.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private static final long MIB = 1024L * 1024L;

    public static final ModConfigSpec SPEC;
    public static final ClientConfig INSTANCE;

    private final ModConfigSpec.LongValue maxCacheBytes;
    private final ModConfigSpec.IntValue maxUploadScanFiles;

    static {
        var configured = new ModConfigSpec.Builder().configure(ClientConfig::new);
        INSTANCE = configured.getLeft();
        SPEC = configured.getRight();
    }

    private ClientConfig(ModConfigSpec.Builder builder) {
        maxCacheBytes = builder.defineInRange("maxCacheBytes", 512L * MIB, 1L, Long.MAX_VALUE);
        maxUploadScanFiles = builder.defineInRange("maxUploadScanFiles", 1_000, 1, 100_000);
    }

    public Limits snapshot() {
        return new Limits(maxCacheBytes.get(), maxUploadScanFiles.get());
    }

    public record Limits(long maxCacheBytes, int maxUploadScanFiles) {
        public Limits {
            if (maxCacheBytes <= 0) {
                throw new IllegalArgumentException("maxCacheBytes must be positive");
            }
            if (maxUploadScanFiles <= 0) {
                throw new IllegalArgumentException("maxUploadScanFiles must be positive");
            }
        }

        public static Limits defaults() {
            return new Limits(512L * MIB, 1_000);
        }
    }
}
