package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.transfer.UploadError;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketUploadBeginResponse implements IMessage {
    private UploadError error;
    private UUID sessionId;
    private int chunkBytes;
    private TrackReference existingTrack;
    private int queuePosition;

    public PacketUploadBeginResponse() {
    }

    public PacketUploadBeginResponse(UploadError error, UUID sessionId, int chunkBytes, TrackReference existingTrack, int queuePosition) {
        this.error = error != null ? error : UploadError.NONE;
        this.sessionId = sessionId;
        this.chunkBytes = chunkBytes;
        this.existingTrack = existingTrack;
        this.queuePosition = queuePosition;
    }

    public static PacketUploadBeginResponse accepted(UUID sessionId, int chunkBytes) {
        return new PacketUploadBeginResponse(UploadError.NONE, sessionId, chunkBytes, null, 0);
    }

    public static PacketUploadBeginResponse alreadyPresent(TrackReference track) {
        return new PacketUploadBeginResponse(UploadError.NONE, null, 0, track, 0);
    }

    public static PacketUploadBeginResponse rejected(UploadError error) {
        return new PacketUploadBeginResponse(error, null, 0, null, 0);
    }

    public UploadError getError() { return error; }
    public UUID getSessionId() { return sessionId; }
    public int getChunkBytes() { return chunkBytes; }
    public TrackReference getExistingTrack() { return existingTrack; }
    public int getQueuePosition() { return queuePosition; }

    @Override
    public void fromBytes(ByteBuf buf) {
        error = UploadError.values()[buf.readByte()];
        sessionId = PacketUtils.readUUID(buf);
        chunkBytes = buf.readInt();
        boolean hasExisting = buf.readBoolean();
        if (hasExisting) {
            existingTrack = TrackReference.fromBuf(buf);
        } else {
            existingTrack = null;
        }
        queuePosition = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(error.ordinal());
        PacketUtils.writeUUID(buf, sessionId);
        buf.writeInt(chunkBytes);
        if (existingTrack != null) {
            buf.writeBoolean(true);
            existingTrack.writeToBuf(buf);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(queuePosition);
    }

    public static class Handler implements IMessageHandler<PacketUploadBeginResponse, IMessage> {
        @Override
        public IMessage onMessage(PacketUploadBeginResponse message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketUploadBeginResponse message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientUploadManager.getInstance().handleBeginResponse(message);
            });
        }
    }
}
