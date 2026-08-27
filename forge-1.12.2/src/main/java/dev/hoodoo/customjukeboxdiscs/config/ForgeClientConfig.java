package dev.hoodoo.customjukeboxdiscs.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class ForgeClientConfig {
    public static Configuration config;

    public static long maxCacheBytes;
    public static int maxUploadScanFiles;
    public static boolean playbackEnabled;

    public static void init(File file) {
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        try {
            config.load();

            String CAT_CLIENT = "client";
            maxCacheBytes = (long) config.get(CAT_CLIENT, "maxCacheBytes", 512 * 1024 * 1024, "Maximum local audio cache size in bytes").getInt();
            maxUploadScanFiles = config.get(CAT_CLIENT, "maxUploadScanFiles", 1000, "Maximum scanned files in uploads directory").getInt();
            playbackEnabled = config.get(CAT_CLIENT, "playbackEnabled", true, "Play custom audio discs in world").getBoolean();

            if (config.hasChanged()) {
                config.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPlaybackEnabled(boolean enabled) {
        playbackEnabled = enabled;
        if (config != null) {
            config.get("client", "playbackEnabled", true).set(enabled);
            config.save();
        }
    }
}
