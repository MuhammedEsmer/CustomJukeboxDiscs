package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketLibraryPageRequest implements IMessage {
    private int page;

    public PacketLibraryPageRequest() { }
    public PacketLibraryPageRequest(int page) { this.page = Math.max(1, page); }
    public int getPage() { return page; }

    @Override public void fromBytes(ByteBuf buf) { page = buf.readInt(); if (page < 1) throw new IllegalArgumentException("invalid page"); }
    @Override public void toBytes(ByteBuf buf) { buf.writeInt(page); }

    public static class Handler implements IMessageHandler<PacketLibraryPageRequest, IMessage> {
        @Override public IMessage onMessage(PacketLibraryPageRequest message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> ServerRuntime.getInstance().handleLibraryPage(player, message.page));
            return null;
        }
    }
}
