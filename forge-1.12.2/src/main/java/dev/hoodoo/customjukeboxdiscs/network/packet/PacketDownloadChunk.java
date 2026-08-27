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

public class PacketDownloadChunk implements IMessage {
    public static final int MAX_BYTES = 31 * 1024;

    private String sha256;
    private int chunkIndex;
    private byte[] bytes;
    private boolean last;

    public PacketDownloadChunk() {
    }

    public PacketDownloadChunk(String sha256, int chunkIndex, byte[] bytes, boolean last) {
        this.sha256 = sha256;
        this.chunkIndex = chunkIndex;
        this.bytes = bytes;
        this.last = last;
    }

    public String getSha256() { return sha256; }
    public int getChunkIndex() { return chunkIndex; }
    public byte[] getBytes() { return bytes; }
    public boolean isLast() { return last; }

    @Override
    public void fromBytes(ByteBuf buf) {
        sha256 = PacketUtils.readUtf(buf, 64);
        chunkIndex = buf.readInt();
        bytes = PacketUtils.readByteArray(buf, MAX_BYTES);
        last = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUtf(buf, sha256, 64);
        buf.writeInt(chunkIndex);
        PacketUtils.writeByteArray(buf, bytes);
        buf.writeBoolean(last);
    }

    public static class Handler implements IMessageHandler<PacketDownloadChunk, IMessage> {
        @Override
        public IMessage onMessage(PacketDownloadChunk message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketDownloadChunk message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientPlaybackManager.getInstance().handleDownloadChunk(message);
            });
        }
    }
}
