package dev.muhammedesmer.customjukeboxdiscs.client;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CustomJukeboxDiscs.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static ResourceKey<Level> lastDimension;

    private ClientGameEvents() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        ResourceKey<Level> current = level == null ? null : level.dimension();
        if (lastDimension != null && !lastDimension.equals(current)) {
            ClientPlaybackManager.INSTANCE.reset();
        }
        lastDimension = current;
    }
}
