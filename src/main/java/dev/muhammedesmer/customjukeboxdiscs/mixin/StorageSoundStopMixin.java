package dev.muhammedesmer.customjukeboxdiscs.mixin;

import dev.muhammedesmer.customjukeboxdiscs.compat.SophisticatedPlayback;
import java.util.UUID;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sophisticated Core sends its own stop packet when a storage stops a disc early or loses its
 * keep-alive. That path never reaches {@code IDiscHandler}, so this is where a programmed disc
 * learns that it should stop streaming.
 */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler", remap = false)
abstract class StorageSoundStopMixin {
    @Inject(method = "sendStopMessage", at = @At("HEAD"), remap = false)
    private static void customJukeboxDiscs$stopStream(
            Level level, Vec3 position, UUID storageId, CallbackInfo callback) {
        SophisticatedPlayback.stopped(level, storageId);
    }
}
