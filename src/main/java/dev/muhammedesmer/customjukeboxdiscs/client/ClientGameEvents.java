package dev.muhammedesmer.customjukeboxdiscs.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CustomJukeboxDiscs.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    public static final KeyMapping TOGGLE_PLAYBACK = new KeyMapping(
            "key.customjukeboxdiscs.toggle_playback",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.customjukeboxdiscs");

    private static ResourceKey<Level> lastDimension;

    private ClientGameEvents() { }

    @SubscribeEvent
    public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientPlaybackPreference.publish();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        while (TOGGLE_PLAYBACK.consumeClick()) {
            ClientPlaybackPreference.toggle();
        }
        var level = Minecraft.getInstance().level;
        ResourceKey<Level> current = level == null ? null : level.dimension();
        if (lastDimension != null && !lastDimension.equals(current)) {
            ClientPlaybackManager.INSTANCE.reset();
        }
        lastDimension = current;
    }
}
