package dev.muhammedesmer.customjukeboxdiscs.client;

import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackPreference;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The player's own choice about hearing custom discs. Sent to the server on join and whenever it
 * changes, so a player who opted out is never sent a track to download.
 */
public final class ClientPlaybackPreference {
    private ClientPlaybackPreference() {
    }

    public static boolean enabled() {
        return ClientConfig.INSTANCE.playbackEnabled();
    }

    public static void publish() {
        PacketDistributor.sendToServer(new PlaybackPreference(enabled()));
    }

    public static void toggle() {
        boolean nowEnabled = !enabled();
        ClientConfig.INSTANCE.setPlaybackEnabled(nowEnabled);
        if (!nowEnabled) {
            ClientPlaybackManager.INSTANCE.reset();
        }
        publish();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(nowEnabled
                    ? "playback.customjukeboxdiscs.enabled"
                    : "playback.customjukeboxdiscs.disabled"), true);
        }
    }
}
