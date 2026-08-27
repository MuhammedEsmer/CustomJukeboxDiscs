package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUploadBegin implements IMessage {
    private String sha256;
    private String title;
    private long fileSizeBytes;
    private long durationMillis;
    private AudioFormat format;
    private long writerFingerprint;

    public PacketUploadBegin() {
    }

    public PacketUploadBegin(String sha256, String title, long fileSizeBytes, long durationMillis, AudioFormat format, long writerFingerprint) {
        this.sha256 = sha256;
        this.title = title;
        this.fileSizeBytes = fileSizeBytes;
        this.durationMillis = durationMillis;
        this.format = format;
        this.writerFingerprint = writerFingerprint;
    }

    public String getSha256() { return sha256; }
    public String getClientHash() { return sha256; }
    public String getTitle() { return title; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public long getDeclaredBytes() { return fileSizeBytes; }
    public long getDurationMillis() { return durationMillis; }
    public AudioFormat getFormat() { return format; }
    public AudioFormat getFormatHint() { return format; }
    public long getWriterFingerprint() { return writerFingerprint; }
    public long getInputFingerprint() { return writerFingerprint; }

    @Override
    public void fromBytes(ByteBuf buf) {
        sha256 = PacketUtils.readUtf(buf, 64);
        title = PacketUtils.readUtf(buf, 256);
        fileSizeBytes = buf.readLong();
        durationMillis = buf.readLong();
        format = AudioFormat.fromSerializedName(PacketUtils.readUtf(buf, 16));
        writerFingerprint = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUtf(buf, sha256, 64);
        PacketUtils.writeUtf(buf, title, 256);
        buf.writeLong(fileSizeBytes);
        buf.writeLong(durationMillis);
        PacketUtils.writeUtf(buf, format.serializedName(), 16);
        buf.writeLong(writerFingerprint);
    }

    public static class Handler implements IMessageHandler<PacketUploadBegin, IMessage> {
        @Override
        public IMessage onMessage(PacketUploadBegin message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (ServerRuntime.getInstance() != null) {
                    ServerRuntime.getInstance().handleUploadBegin(player, message);
                }
            });
            return null;
        }
    }
}
