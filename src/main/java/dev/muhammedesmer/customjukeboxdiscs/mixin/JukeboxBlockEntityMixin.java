package dev.muhammedesmer.customjukeboxdiscs.mixin;

import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.server.ServerRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
abstract class JukeboxBlockEntityMixin {
    @Inject(method = "setTheItem", at = @At("TAIL"))
    private void customJukeboxDiscs$itemChanged(ItemStack stack, CallbackInfo callback) {
        JukeboxBlockEntity self = (JukeboxBlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)) return;
        TrackReference track = stack.get(ModDataComponents.TRACK_REFERENCE);
        if (track == null) ServerRuntime.stop(level, self.getBlockPos());
        else ServerRuntime.play(level, self.getBlockPos(), track, 0);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void customJukeboxDiscs$stopAtTrackDuration(
            Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, CallbackInfo callback) {
        TrackReference track = jukebox.getTheItem().get(ModDataComponents.TRACK_REFERENCE);
        if (track != null && jukebox.getSongPlayer().getTicksSinceSongStarted() * 50L >= track.durationMillis()) {
            jukebox.getSongPlayer().stop(level, state);
            if (level instanceof ServerLevel serverLevel) ServerRuntime.stop(serverLevel, pos);
        }
    }
}
