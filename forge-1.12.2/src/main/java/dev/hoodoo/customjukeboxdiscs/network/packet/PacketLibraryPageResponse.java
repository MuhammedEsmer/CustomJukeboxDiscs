package dev.hoodoo.customjukeboxdiscs.network.packet;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientLibraryManager;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketLibraryPageResponse implements IMessage {
    public static final int MAX_TRACKS = 20;
    private int page;
    private int pageCount;
    private int totalTracks;
    private List<TrackReference> tracks = Collections.emptyList();

    public PacketLibraryPageResponse() { }
    public PacketLibraryPageResponse(int page, int pageCount, int totalTracks, List<TrackReference> tracks) {
        if (page < 1 || pageCount < 1 || page > pageCount || totalTracks < 0 || tracks.size() > MAX_TRACKS) {
            throw new IllegalArgumentException("invalid library page");
        }
        this.page = page;
        this.pageCount = pageCount;
        this.totalTracks = totalTracks;
        this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
    }
    public int getPage() { return page; }
    public int getPageCount() { return pageCount; }
    public int getTotalTracks() { return totalTracks; }
    public List<TrackReference> getTracks() { return tracks; }

    @Override public void fromBytes(ByteBuf buf) {
        page = buf.readInt(); pageCount = buf.readInt(); totalTracks = buf.readInt();
        int count = buf.readUnsignedByte();
        if (page < 1 || pageCount < 1 || page > pageCount || totalTracks < 0 || count > MAX_TRACKS) {
            throw new IllegalArgumentException("invalid library page");
        }
        List<TrackReference> decoded = new ArrayList<>(count);
        for (int index = 0; index < count; index++) decoded.add(TrackReference.fromBuf(buf));
        tracks = Collections.unmodifiableList(decoded);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(page); buf.writeInt(pageCount); buf.writeInt(totalTracks); buf.writeByte(tracks.size());
        for (TrackReference track : tracks) track.writeToBuf(buf);
    }

    public static class Handler implements IMessageHandler<PacketLibraryPageResponse, IMessage> {
        @Override public IMessage onMessage(PacketLibraryPageResponse message, MessageContext context) { handleClient(message); return null; }
        @SideOnly(Side.CLIENT) private void handleClient(PacketLibraryPageResponse message) {
            Minecraft.getMinecraft().addScheduledTask(() -> ClientLibraryManager.getInstance().handle(message));
        }
    }
}
