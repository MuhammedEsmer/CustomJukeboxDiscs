package dev.hoodoo.customjukeboxdiscs.client.audio;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PlaybackOffsetRegistry {
    private static final Map<String, Long> OFFSETS = new ConcurrentHashMap<>();

    private PlaybackOffsetRegistry() {
    }

    static void register(URL url, long elapsedMillis) {
        OFFSETS.put(url.toExternalForm(), Math.max(0L, elapsedMillis));
    }

    static long consume(URL url) {
        Long offset = OFFSETS.remove(url.toExternalForm());
        return offset == null ? 0L : offset;
    }
}
