package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketTrackBegin implements IMessage {
    private String sha256;
    private long totalBytes;
    private long durationMillis;
    private AudioFormat format = AudioFormat.MP3;

    public PacketTrackBegin() {
    }

    public PacketTrackBegin(String sha256, long totalBytes, long durationMillis, AudioFormat format) {
        this.sha256 = sha256;
        this.totalBytes = totalBytes;
        this.durationMillis = durationMillis;
        this.format = format != null ? format : AudioFormat.MP3;
    }

    public String getSha256() { return sha256; }
    public long getTotalBytes() { return totalBytes; }
    public long getDurationMillis() { return durationMillis; }
    public AudioFormat getFormat() { return format; }

    @Override
    public void fromBytes(ByteBuf buf) {
        sha256 = PacketUtils.readUtf(buf, 64);
        totalBytes = buf.readLong();
        durationMillis = buf.readLong();
        format = AudioFormat.fromSerializedName(PacketUtils.readUtf(buf, 16));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUtf(buf, sha256, 64);
        buf.writeLong(totalBytes);
        buf.writeLong(durationMillis);
        PacketUtils.writeUtf(buf, format.serializedName(), 16);
    }

    public static class Handler implements IMessageHandler<PacketTrackBegin, IMessage> {
        @Override
        public IMessage onMessage(PacketTrackBegin message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketTrackBegin message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientPlaybackManager.getInstance().handleTrackBegin(message);
            });
        }
    }
}
