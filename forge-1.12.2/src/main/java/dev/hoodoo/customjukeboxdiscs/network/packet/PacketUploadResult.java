package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.transfer.UploadError;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketUploadResult implements IMessage {
    private UploadError error;
    private TrackReference track;

    public PacketUploadResult() {
    }

    public PacketUploadResult(UploadError error) {
        this(error, null);
    }

    public PacketUploadResult(UploadError error, TrackReference track) {
        this.error = error != null ? error : UploadError.NONE;
        this.track = track;
    }

    public UploadError getError() { return error; }
    public TrackReference getTrack() { return track; }

    @Override
    public void fromBytes(ByteBuf buf) {
        error = UploadError.values()[buf.readByte()];
        boolean hasTrack = buf.readBoolean();
        if (hasTrack) {
            track = TrackReference.fromBuf(buf);
        } else {
            track = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(error.ordinal());
        if (track != null) {
            buf.writeBoolean(true);
            track.writeToBuf(buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static class Handler implements IMessageHandler<PacketUploadResult, IMessage> {
        @Override
        public IMessage onMessage(PacketUploadResult message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketUploadResult message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientUploadManager.getInstance().handleUploadResult(message);
            });
        }
    }
}
