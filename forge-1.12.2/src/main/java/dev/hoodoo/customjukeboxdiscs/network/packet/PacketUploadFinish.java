package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUploadFinish implements IMessage {
    private UUID sessionId;
    private String clientHash;

    public PacketUploadFinish() {
    }

    public PacketUploadFinish(UUID sessionId, String clientHash) {
        this.sessionId = sessionId;
        this.clientHash = clientHash;
    }

    public UUID getSessionId() { return sessionId; }
    public String getClientHash() { return clientHash; }

    @Override
    public void fromBytes(ByteBuf buf) {
        sessionId = PacketUtils.readUUID(buf);
        clientHash = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUUID(buf, sessionId);
        ByteBufUtils.writeUTF8String(buf, clientHash != null ? clientHash : "");
    }

    public static class Handler implements IMessageHandler<PacketUploadFinish, IMessage> {
        @Override
        public IMessage onMessage(PacketUploadFinish message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (ServerRuntime.getInstance() != null) {
                    ServerRuntime.getInstance().handleUploadFinish(player, message);
                }
            });
            return null;
        }
    }
}
