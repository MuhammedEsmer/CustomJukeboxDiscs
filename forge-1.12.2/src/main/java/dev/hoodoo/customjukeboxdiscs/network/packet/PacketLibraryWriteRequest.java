package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.network.PacketUtils;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import java.util.regex.Pattern;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketLibraryWriteRequest implements IMessage {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private String sha256;

    public PacketLibraryWriteRequest() { }
    public PacketLibraryWriteRequest(String sha256) {
        requireHash(sha256); this.sha256 = sha256;
    }
    public String getSha256() { return sha256; }
    @Override public void fromBytes(ByteBuf buf) { sha256 = PacketUtils.readUtf(buf, 64); requireHash(sha256); }
    @Override public void toBytes(ByteBuf buf) { PacketUtils.writeUtf(buf, sha256, 64); }
    private static void requireHash(String value) { if (value == null || !SHA_256.matcher(value).matches()) throw new IllegalArgumentException("invalid track hash"); }

    public static class Handler implements IMessageHandler<PacketLibraryWriteRequest, IMessage> {
        @Override public IMessage onMessage(PacketLibraryWriteRequest message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> ServerRuntime.getInstance().handleLibraryWrite(
                    player, message.sha256));
            return null;
        }
    }
}
