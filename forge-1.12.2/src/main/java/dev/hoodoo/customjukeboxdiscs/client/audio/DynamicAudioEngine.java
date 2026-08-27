package dev.hoodoo.customjukeboxdiscs.client.audio;

import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.network.PlaybackAnchor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class DynamicAudioEngine {
    private final Map<PlaybackAnchor, DynamicTrackSound> playing = new HashMap<>();

    public synchronized void play(PlaybackAnchor anchor, TrackReference track, Path file, TrackTimeline timeline) {
        stop(anchor);
        if (timeline.finishedAt(System.nanoTime())) {
            return;
        }
        DynamicTrackSound sound = new DynamicTrackSound(anchor, file, track, timeline);
        playing.put(anchor, sound);
        sound.start();
    }

    public synchronized void stop(PlaybackAnchor anchor) {
        DynamicTrackSound sound = playing.remove(anchor);
        if (sound != null) {
            sound.stop();
        }
    }

    public synchronized void stopAll() {
        for (DynamicTrackSound sound : playing.values()) {
            sound.stop();
        }
        playing.clear();
    }

    public synchronized void tick() {
        playing.entrySet().removeIf(entry -> {
            DynamicTrackSound sound = entry.getValue();
            sound.update();
            return sound.isDonePlaying();
        });
    }
}
