package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.network.PlaybackAnchor;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketJukeboxPlay implements IMessage {
    private PlaybackAnchor anchor;
    private TrackReference track;
    private long startOffsetNanos;

    public PacketJukeboxPlay() {
    }

    public PacketJukeboxPlay(PlaybackAnchor anchor, TrackReference track, long startOffsetNanos) {
        this.anchor = anchor;
        this.track = track;
        this.startOffsetNanos = startOffsetNanos;
    }

    public PlaybackAnchor getAnchor() { return anchor; }
    public TrackReference getTrack() { return track; }
    public long getStartOffsetNanos() { return startOffsetNanos; }

    @Override
    public void fromBytes(ByteBuf buf) {
        anchor = PlaybackAnchor.fromBytes(buf);
        track = TrackReference.fromBuf(buf);
        startOffsetNanos = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        anchor.toBytes(buf);
        track.writeToBuf(buf);
        buf.writeLong(startOffsetNanos);
    }

    public static class Handler implements IMessageHandler<PacketJukeboxPlay, IMessage> {
        @Override
        public IMessage onMessage(PacketJukeboxPlay message, MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(PacketJukeboxPlay message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientPlaybackManager.getInstance().handlePlay(message.getAnchor(), message.getTrack(), message.getStartOffsetNanos());
            });
        }
    }
}
