package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUploadChunk implements IMessage {
    private UUID sessionId;
    private long offset;
    private byte[] bytes;

    public PacketUploadChunk() {
    }

    public PacketUploadChunk(UUID sessionId, long offset, byte[] bytes) {
        this.sessionId = sessionId;
        this.offset = offset;
        this.bytes = bytes;
    }

    public UUID getSessionId() { return sessionId; }
    public long getOffset() { return offset; }
    public byte[] getBytes() { return bytes; }

    @Override
    public void fromBytes(ByteBuf buf) {
        sessionId = PacketUtils.readUUID(buf);
        offset = buf.readLong();
        bytes = PacketUtils.readByteArray(buf, 32768);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUUID(buf, sessionId);
        buf.writeLong(offset);
        PacketUtils.writeByteArray(buf, bytes);
    }

    public static class Handler implements IMessageHandler<PacketUploadChunk, IMessage> {
        @Override
        public IMessage onMessage(PacketUploadChunk message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (ServerRuntime.getInstance() != null) {
                    ServerRuntime.getInstance().handleUploadChunk(player, message);
                }
            });
            return null;
        }
    }
}
