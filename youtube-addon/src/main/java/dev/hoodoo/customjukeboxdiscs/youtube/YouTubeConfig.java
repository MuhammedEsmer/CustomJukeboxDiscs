package dev.hoodoo.customjukeboxdiscs.youtube;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class YouTubeConfig {
    public static final ModConfigSpec SPEC;
    public static final YouTubeConfig INSTANCE;

    private final ModConfigSpec.BooleanValue enabled;
    private final ModConfigSpec.BooleanValue managedTools;
    private final ModConfigSpec.ConfigValue<String> ytDlpPath;
    private final ModConfigSpec.ConfigValue<String> ffmpegPath;
    private final ModConfigSpec.IntValue maxQueuedJobs;

    static {
        var configured = new ModConfigSpec.Builder().configure(YouTubeConfig::new);
        INSTANCE = configured.getLeft();
        SPEC = configured.getRight();
    }

    private YouTubeConfig(ModConfigSpec.Builder builder) {
        enabled = builder.define("enabled", true);
        maxQueuedJobs = builder.defineInRange("maxQueuedJobs", 8, 1, 64);
        builder.push("tools");
        managedTools = builder
                .comment("Download verified yt-dlp and FFmpeg builds when explicit paths are empty.")
                .define("managedDownloads", true);
        ytDlpPath = builder.define("ytDlpPath", "");
        ffmpegPath = builder.define("ffmpegPath", "");
        builder.pop();
    }

    public Snapshot snapshot() {
        return new Snapshot(enabled.get(), managedTools.get(), ytDlpPath.get(), ffmpegPath.get(), maxQueuedJobs.get());
    }

    public record Snapshot(boolean enabled, boolean managedTools, String ytDlpPath, String ffmpegPath, int maxQueuedJobs) {
    }
}
