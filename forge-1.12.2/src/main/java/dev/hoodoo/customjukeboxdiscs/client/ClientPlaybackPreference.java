package dev.hoodoo.customjukeboxdiscs.client;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.hoodoo.customjukeboxdiscs.config.ForgeClientConfig;
import dev.hoodoo.customjukeboxdiscs.network.ModNetwork;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketPlaybackPreference;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentTranslation;

public final class ClientPlaybackPreference {
    private ClientPlaybackPreference() {
    }

    public static boolean enabled() {
        return ForgeClientConfig.playbackEnabled;
    }

    public static void publish() {
        ModNetwork.CHANNEL.sendToServer(new PacketPlaybackPreference(enabled()));
    }

    public static void toggle() {
        boolean nowEnabled = !enabled();
        ForgeClientConfig.setPlaybackEnabled(nowEnabled);
        if (!nowEnabled) {
            ClientPlaybackManager.getInstance().reset();
        }
        publish();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player != null) {
            minecraft.player.sendStatusMessage(new TextComponentTranslation(nowEnabled
                    ? "playback.customjukeboxdiscs.enabled"
                    : "playback.customjukeboxdiscs.disabled"), true);
        }
    }
}
