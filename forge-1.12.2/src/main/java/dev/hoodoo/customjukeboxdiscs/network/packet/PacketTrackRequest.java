package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTrackRequest implements IMessage {
    private String sha256;

    public PacketTrackRequest() {
    }

    public PacketTrackRequest(String sha256) {
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

    public static class Handler implements IMessageHandler<PacketTrackRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketTrackRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (ServerRuntime.getInstance() != null) {
                    ServerRuntime.getInstance().handleTrackRequest(player, message.getSha256());
                }
            });
            return null;
        }
    }
}
