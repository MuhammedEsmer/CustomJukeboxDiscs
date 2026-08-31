package dev.muhammedesmer.customjukeboxdiscs.compat.sophisticated;

import dev.muhammedesmer.customjukeboxdiscs.compat.SophisticatedPlayback;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackAnchor;
import dev.muhammedesmer.customjukeboxdiscs.server.ServerRuntime;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;

/**
 * Plays programmed discs through the Sophisticated Backpacks/Storage jukebox upgrade.
 *
 * <p>Sophisticated Core keeps owning the upgrade's lifecycle: it advances the playlist when the
 * validated duration is over and it expires the entry when the storage disappears. Only the audio
 * itself is ours.
 */
public final class ProgrammedDiscHandler implements IDiscHandler<TrackReference> {
    @Override
    public Optional<TrackReference> getSongInfo(ItemStack stack, Level level) {
        return Optional.ofNullable(stack.get(ModDataComponents.TRACK_REFERENCE));
    }

    @Override
    public boolean supports(ItemStack stack) {
        return stack.has(ModDataComponents.TRACK_REFERENCE);
    }

    @Override
    public Optional<Integer> getMusicLengthInTicks(ItemStack stack, Level level) {
        return getSongInfo(stack, level).map(track -> (int) Math.max(1L, track.durationMillis() / 50L));
    }

    @Override
    public void playDisc(ServerLevel level, BlockPos position, UUID storageId, ItemStack stack, Runnable onFinished) {
        start(level, PlaybackAnchor.atBlock(position), Vec3.atCenterOf(position), storageId, stack, onFinished);
    }

    /** A carried storage: the music travels with whoever holds it. */
    @Override
    public void playDisc(
            ServerLevel level, Vec3 position, UUID storageId, ItemStack stack, int entityId, Runnable onFinished) {
        start(level, PlaybackAnchor.onEntity(entityId), position, storageId, stack, onFinished);
    }

    /** Programmed discs are never handed out at random; they would carry no track. */
    @Override
    public Optional<ItemStack> getRandomDisc(RandomSource random) {
        return Optional.empty();
    }

    @Override
    public int getMusicDiscSize() {
        return 0;
    }

    private static void start(
            ServerLevel level,
            PlaybackAnchor anchor,
            Vec3 position,
            UUID storageId,
            ItemStack stack,
            Runnable onFinished) {
        TrackReference track = stack.get(ModDataComponents.TRACK_REFERENCE);
        if (track == null) {
            return;
        }
        long finishTick = level.getGameTime() + Math.max(1L, track.durationMillis() / 50L);
        // Order matters: skipping to another disc reuses the same storage and the same anchor, so the
        // previous stream has to be released before the new one starts or it would cancel the new one.
        SophisticatedPlayback.started(level, storageId, anchor, finishTick);
        ServerRuntime.play(level, anchor, track, 0);
        ServerStorageSoundHandler.putSoundInfo(level, storageId, onFinished, position, finishTick);
    }
}
