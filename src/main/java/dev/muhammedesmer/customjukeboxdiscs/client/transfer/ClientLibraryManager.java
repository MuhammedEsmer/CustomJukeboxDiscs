package dev.muhammedesmer.customjukeboxdiscs.client.transfer;

import dev.muhammedesmer.customjukeboxdiscs.client.screen.DiscWriterScreen;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.LibraryPageRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.LibraryPageResponse;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.LibraryWriteRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.LibraryWriteResponse;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientLibraryManager {
    public static final ClientLibraryManager INSTANCE = new ClientLibraryManager();
    private DiscWriterScreen screen;

    private ClientLibraryManager() {
    }

    public void attach(DiscWriterScreen screen) {
        this.screen = screen;
    }

    public void detach(DiscWriterScreen screen) {
        if (this.screen == screen) this.screen = null;
    }

    public void requestPage(int page) {
        PacketDistributor.sendToServer(new LibraryPageRequest(Math.max(1, page)));
    }

    public void write(String sha256) {
        PacketDistributor.sendToServer(new LibraryWriteRequest(sha256));
    }

    public void handle(LibraryPageResponse response) {
        Minecraft.getInstance().execute(() -> {
            if (screen != null && Minecraft.getInstance().screen == screen) screen.onLibraryPage(response);
        });
    }

    public void handle(LibraryWriteResponse response) {
        Minecraft.getInstance().execute(() -> {
            if (screen != null && Minecraft.getInstance().screen == screen) screen.onLibraryWrite(response);
        });
    }
}
