package dev.hoodoo.customjukeboxdiscs.youtube;

import java.net.URI;
import java.time.Duration;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class YouTubeConfig {
    public static final ModConfigSpec SPEC;
    public static final YouTubeConfig INSTANCE;

    private final ModConfigSpec.BooleanValue enabled;
    private final ModConfigSpec.ConfigValue<String> serviceUrl;
    private final ModConfigSpec.ConfigValue<String> serviceToken;
    private final ModConfigSpec.IntValue requestTimeoutMillis;
    private final ModConfigSpec.IntValue maxQueuedJobs;

    static {
        var configured = new ModConfigSpec.Builder().configure(YouTubeConfig::new);
        INSTANCE = configured.getLeft();
        SPEC = configured.getRight();
    }

    private YouTubeConfig(ModConfigSpec.Builder builder) {
        enabled = builder.define("enabled", true);
        maxQueuedJobs = builder.defineInRange("maxQueuedJobs", 8, 1, 64);
        builder.push("service");
        serviceUrl = builder.define("url", "http://127.0.0.1:8765/v1/import");
        serviceToken = builder.define("token", "");
        requestTimeoutMillis = builder.defineInRange("timeoutMillis", 120000, 1000, 600000);
        builder.pop();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                enabled.get(), URI.create(serviceUrl.get()), serviceToken.get(),
                Duration.ofMillis(requestTimeoutMillis.get()), maxQueuedJobs.get());
    }

    public record Snapshot(boolean enabled, URI serviceUrl, String serviceToken, Duration timeout, int maxQueuedJobs) {
    }
}
