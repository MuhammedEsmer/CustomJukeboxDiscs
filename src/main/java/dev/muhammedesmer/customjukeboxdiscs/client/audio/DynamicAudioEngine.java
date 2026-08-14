package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class DynamicAudioEngine {
    private final Map<BlockPos, DynamicTrackSound> playing = new HashMap<>();

    public void play(BlockPos pos, TrackReference track, Path file, long elapsedMillis) {
        stop(pos);
        long remaining = Math.max(1, track.durationMillis() - elapsedMillis);
        DynamicTrackSound sound = new DynamicTrackSound(pos.immutable(), file, track.format(), elapsedMillis, remaining);
        playing.put(pos.immutable(), sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public void stop(BlockPos pos) {
        DynamicTrackSound sound = playing.remove(pos);
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
    }

    public void stopAll() {
        playing.values().forEach(sound -> Minecraft.getInstance().getSoundManager().stop(sound));
        playing.clear();
    }
}
