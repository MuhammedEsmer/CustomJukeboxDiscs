package dev.hoodoo.customjukeboxdiscs.client.transfer;

import dev.hoodoo.customjukeboxdiscs.network.ModNetwork;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryPageRequest;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryPageResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryWriteRequest;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryWriteResponse;

public final class ClientLibraryManager {
    public interface Listener {
        void onLibraryPage(PacketLibraryPageResponse response);
        void onLibraryWrite(PacketLibraryWriteResponse response);
    }

    private static final ClientLibraryManager INSTANCE = new ClientLibraryManager();
    private Listener listener;

    private ClientLibraryManager() { }
    public static ClientLibraryManager getInstance() { return INSTANCE; }
    public void attach(Listener listener) { this.listener = listener; }
    public void detach(Listener listener) { if (this.listener == listener) this.listener = null; }
    public void requestPage(int page) { ModNetwork.CHANNEL.sendToServer(new PacketLibraryPageRequest(Math.max(1, page))); }
    public void write(String sha256) { ModNetwork.CHANNEL.sendToServer(new PacketLibraryWriteRequest(sha256)); }
    public void handle(PacketLibraryPageResponse response) { if (listener != null) listener.onLibraryPage(response); }
    public void handle(PacketLibraryWriteResponse response) { if (listener != null) listener.onLibraryWrite(response); }
}
