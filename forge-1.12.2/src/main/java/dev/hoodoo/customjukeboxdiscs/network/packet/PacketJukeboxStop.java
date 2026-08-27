package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.hoodoo.customjukeboxdiscs.network.PlaybackAnchor;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketJukeboxStop implements IMessage {
    private PlaybackAnchor anchor;

    public PacketJukeboxStop() {
    }

    public PacketJukeboxStop(PlaybackAnchor anchor) {
        this.anchor = anchor;
    }

    public PlaybackAnchor getAnchor() { return anchor; }

    @Override
    public void fromBytes(ByteBuf buf) {
        anchor = PlaybackAnchor.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        anchor.toBytes(buf);
    }

    public static class Handler implements IMessageHandler<PacketJukeboxStop, IMessage> {
        @Override
        public IMessage onMessage(PacketJukeboxStop message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketJukeboxStop message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientPlaybackManager.getInstance().handleStop(message.getAnchor());
            });
        }
    }
}
