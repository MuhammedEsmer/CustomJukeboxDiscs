package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackAnchor;

public final class DynamicAudioEngine {
    private final Map<PlaybackAnchor, DynamicTrackSound> playing = new HashMap<>();

    public void play(PlaybackAnchor anchor, TrackReference track, Path file, TrackTimeline timeline) {
        stop(anchor);
        if (timeline.finishedAt(System.nanoTime())) return;
        DynamicTrackSound sound = new DynamicTrackSound(anchor, file, track.format(), timeline);
        playing.put(anchor, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public void stop(PlaybackAnchor anchor) {
        DynamicTrackSound sound = playing.remove(anchor);
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
    }

    public void stopAll() {
        playing.values().forEach(sound -> Minecraft.getInstance().getSoundManager().stop(sound));
        playing.clear();
    }
}
