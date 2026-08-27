package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUrlUploadRequest implements IMessage {
    private String url;
    private String title;
    private long writerFingerprint;

    public PacketUrlUploadRequest() {
    }

    public PacketUrlUploadRequest(String url, String title, long writerFingerprint) {
        this.url = url;
        this.title = title;
        this.writerFingerprint = writerFingerprint;
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public long getWriterFingerprint() { return writerFingerprint; }

    @Override
    public void fromBytes(ByteBuf buf) {
        url = PacketUtils.readUtf(buf, 1024);
        title = PacketUtils.readUtf(buf, 256);
        writerFingerprint = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketUtils.writeUtf(buf, url, 1024);
        PacketUtils.writeUtf(buf, title, 256);
        buf.writeLong(writerFingerprint);
    }

    public static class Handler implements IMessageHandler<PacketUrlUploadRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketUrlUploadRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (ServerRuntime.getInstance() != null) {
                    ServerRuntime.getInstance().handleUrlUpload(player, message);
                }
            });
            return null;
        }
    }
}
