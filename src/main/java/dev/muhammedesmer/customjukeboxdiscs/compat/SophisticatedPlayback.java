package dev.muhammedesmer.customjukeboxdiscs.compat;

import dev.muhammedesmer.customjukeboxdiscs.server.ServerRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackAnchor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Tracks the programmed discs that a Sophisticated storage is currently playing.
 *
 * <p>This class deliberately mentions no Sophisticated class, so the mixin that observes their stop
 * calls can reach it even when the integration itself was never installed.
 */
public final class SophisticatedPlayback {
    private static final Map<UUID, Anchor> ACTIVE = new HashMap<>();

    private SophisticatedPlayback() {
    }

    public static synchronized void started(
            ServerLevel level, UUID storageId, PlaybackAnchor anchor, long finishTick) {
        stopped(level, storageId);
        ACTIVE.put(storageId, new Anchor(level.dimension(), anchor, finishTick));
    }

    /** Stops the stream a storage started, whether it ended on its own or was stopped early. */
    public static synchronized void stopped(Level level, UUID storageId) {
        Anchor anchor = ACTIVE.remove(storageId);
        if (anchor != null && level instanceof ServerLevel serverLevel
                && serverLevel.dimension().equals(anchor.dimension)) {
            ServerRuntime.stop(serverLevel, anchor.anchor);
        }
    }

    /** Releases anchors whose track has run to its validated end. */
    public static synchronized void expireFinished(net.minecraft.server.MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ACTIVE.entrySet().removeIf(entry -> {
            Anchor anchor = entry.getValue();
            ServerLevel level = server.getLevel(anchor.dimension);
            if (level == null) {
                return true;
            }
            if (level.getGameTime() < anchor.finishTick) {
                return false;
            }
            ServerRuntime.stop(level, anchor.anchor);
            return true;
        });
    }

    private record Anchor(ResourceKey<Level> dimension, PlaybackAnchor anchor, long finishTick) {
    }
}
