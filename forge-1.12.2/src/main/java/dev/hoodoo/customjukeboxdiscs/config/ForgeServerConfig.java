package dev.hoodoo.customjukeboxdiscs.config;

import dev.hoodoo.customjukeboxdiscs.transfer.UploadLimits;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import net.minecraftforge.common.config.Configuration;

public final class ForgeServerConfig {
    public static Configuration config;

    public static long maxSourceBytes = 10 * 1024 * 1024L;
    public static long maxDurationSeconds = 600L;
    public static long maxTracksPerPlayer = 20L;
    public static long maxBytesPerPlayer = 100 * 1024 * 1024L;
    public static long maxServerBytes = 2L * 1024 * 1024 * 1024L;
    public static int maxSessionsPerPlayer = 1;
    public static int chunkBytes = 31 * 1024;
    public static long uploadBytesPerSecond = 512 * 1024L;
    public static long uploadTimeoutSeconds = 30L;
    public static boolean mp3Enabled = true;
    public static boolean oggEnabled = true;
    public static boolean urlUploadsEnabled = true;
    public static List<String> urlAllowedHosts = Arrays.asList(
            "archive.org",
            "freemusicarchive.org",
            "cdn.discordapp.com",
            "media.discordapp.net",
            "github.io"
    );
    public static boolean urlAllowPrivateAddresses = false;

    public static void init(File file) {
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        if (config == null) return;
        try {
            config.load();

            String CAT_LIMITS = "limits";
            maxSourceBytes = getLong(CAT_LIMITS, "maxSourceBytes", 10 * 1024 * 1024L, "Maximum uploaded audio file size in bytes");
            maxDurationSeconds = getLong(CAT_LIMITS, "maxDurationSeconds", 600L, "Maximum decoded duration of a track in seconds");
            maxTracksPerPlayer = getLong(CAT_LIMITS, "maxTracksPerPlayer", 20L, "Maximum tracks owned by one player");
            maxBytesPerPlayer = getLong(CAT_LIMITS, "maxBytesPerPlayer", 100 * 1024 * 1024L, "Maximum storage bytes owned by one player");
            maxServerBytes = getLong(CAT_LIMITS, "maxServerBytes", 2048L * 1024 * 1024L, "Maximum total audio storage bytes on the server");
            maxSessionsPerPlayer = config.get(CAT_LIMITS, "maxSessionsPerPlayer", 1, "Maximum concurrent upload sessions per player").getInt();
            chunkBytes = config.get(CAT_LIMITS, "chunkBytes", 31 * 1024, "Network chunk payload size in bytes").getInt();
            uploadBytesPerSecond = getLong(CAT_LIMITS, "uploadBytesPerSecond", 512 * 1024L, "Allowed upload rate in bytes/sec per player");
            uploadTimeoutSeconds = getLong(CAT_LIMITS, "uploadTimeoutSeconds", 30L, "Inactivity timeout in seconds for uploads");
            mp3Enabled = config.get(CAT_LIMITS, "mp3Enabled", true, "Allow MP3 audio files").getBoolean();
            oggEnabled = config.get(CAT_LIMITS, "oggEnabled", true, "Allow OGG Vorbis audio files").getBoolean();

            String CAT_URL = "url_uploads";
            urlUploadsEnabled = config.get(CAT_URL, "enabled", true, "Allow players to write discs from direct https audio links").getBoolean();
            String[] defaultHosts = new String[]{
                    "archive.org",
                    "freemusicarchive.org",
                    "cdn.discordapp.com",
                    "media.discordapp.net",
                    "github.io"
            };
            urlAllowedHosts = Arrays.asList(config.get(CAT_URL, "allowedHosts", defaultHosts, "Hostnames allowed for URL track downloads").getStringList());
            urlAllowPrivateAddresses = config.get(CAT_URL, "allowPrivateAddresses", false, "Allow server to fetch audio from local private network IPs").getBoolean();

            if (config.hasChanged()) {
                config.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long getLong(String category, String name, long defaultValue, String comment) {
        String val = config.get(category, name, Long.toString(defaultValue), comment).getString();
        try {
            if (val.contains("E") || val.contains("e") || val.contains(".")) {
                return (long) Double.parseDouble(val);
            }
            return Long.parseLong(val);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static UploadLimits getUploadLimits() {
        return new UploadLimits(
                maxSourceBytes,
                maxDurationSeconds * 1000L,
                maxTracksPerPlayer,
                maxBytesPerPlayer,
                maxServerBytes,
                maxSessionsPerPlayer,
                chunkBytes,
                uploadBytesPerSecond,
                uploadTimeoutSeconds * 1000L,
                mp3Enabled,
                oggEnabled
        );
    }
}
