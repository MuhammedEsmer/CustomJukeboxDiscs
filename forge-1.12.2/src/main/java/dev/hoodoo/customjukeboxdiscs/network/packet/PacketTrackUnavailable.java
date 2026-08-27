package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketTrackUnavailable implements IMessage {
    private String sha256;

    public PacketTrackUnavailable() {
    }

    public PacketTrackUnavailable(String sha256) {
        this.sha256 = sha256;
    }

    public String getSha256() { return sha256; }

    @Override
    public void fromBytes(ByteBuf buf) {
        sha256 = PacketUtils.readUtf(buf, 64);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUtf(buf, sha256, 64);
    }

    public static class Handler implements IMessageHandler<PacketTrackUnavailable, IMessage> {
        @Override
        public IMessage onMessage(PacketTrackUnavailable message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketTrackUnavailable message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientPlaybackManager.getInstance().handleTrackUnavailable(message.getSha256());
            });
        }
    }
}
