package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientLibraryManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketLibraryWriteResponse implements IMessage {
    public enum Result { WRITTEN, INVALID_WRITER, TRACK_UNAVAILABLE }
    private Result result;
    public PacketLibraryWriteResponse() { }
    public PacketLibraryWriteResponse(Result result) { this.result = result; }
    public Result getResult() { return result; }
    @Override public void fromBytes(ByteBuf buf) { int value = buf.readUnsignedByte(); if (value >= Result.values().length) throw new IllegalArgumentException("invalid result"); result = Result.values()[value]; }
    @Override public void toBytes(ByteBuf buf) { buf.writeByte(result.ordinal()); }

    public static class Handler implements IMessageHandler<PacketLibraryWriteResponse, IMessage> {
        @Override public IMessage onMessage(PacketLibraryWriteResponse message, MessageContext context) { handleClient(message); return null; }
        @SideOnly(Side.CLIENT) private void handleClient(PacketLibraryWriteResponse message) {
            Minecraft.getMinecraft().addScheduledTask(() -> ClientLibraryManager.getInstance().handle(message));
        }
    }
}
