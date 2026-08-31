package dev.hoodoo.customjukeboxdiscs.network;

import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketDownloadChunk;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketJukeboxPlay;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketJukeboxStop;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketPlaybackPreference;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackBegin;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackRequest;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackUnavailable;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadBegin;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadBeginResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadChunk;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadFinish;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadResult;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUrlUploadRequest;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryPageRequest;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryPageResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryWriteRequest;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryWriteResponse;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(ModItems.MOD_ID);
    private static int nextId = 0;

    private ModNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(PacketUploadBegin.Handler.class, PacketUploadBegin.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketUploadBeginResponse.Handler.class, PacketUploadBeginResponse.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketUploadChunk.Handler.class, PacketUploadChunk.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketUploadFinish.Handler.class, PacketUploadFinish.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketUploadResult.Handler.class, PacketUploadResult.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketUrlUploadRequest.Handler.class, PacketUrlUploadRequest.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketTrackRequest.Handler.class, PacketTrackRequest.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketTrackBegin.Handler.class, PacketTrackBegin.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketDownloadChunk.Handler.class, PacketDownloadChunk.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketTrackUnavailable.Handler.class, PacketTrackUnavailable.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketJukeboxPlay.Handler.class, PacketJukeboxPlay.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketJukeboxStop.Handler.class, PacketJukeboxStop.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketPlaybackPreference.Handler.class, PacketPlaybackPreference.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketLibraryPageRequest.Handler.class, PacketLibraryPageRequest.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketLibraryPageResponse.Handler.class, PacketLibraryPageResponse.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketLibraryWriteRequest.Handler.class, PacketLibraryWriteRequest.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketLibraryWriteResponse.Handler.class, PacketLibraryWriteResponse.class, nextId++, Side.CLIENT);
    }
}
