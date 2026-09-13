package dev.hoodoo.customjukeboxdiscs.youtube;

import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImporterRegistry;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(CustomJukeboxDiscsYouTube.MOD_ID)
public final class CustomJukeboxDiscsYouTube {
    public static final String MOD_ID = "customjukeboxdiscsyoutube";
    private final YouTubeImporter importer;

    public CustomJukeboxDiscsYouTube(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, YouTubeConfig.SPEC, "customjukeboxdiscs-youtube-server.toml");
        importer = new YouTubeImporter(
                () -> {
                    YouTubeConfig.Snapshot config = YouTubeConfig.INSTANCE.snapshot();
                    return new ImportServiceClient(config.serviceUrl(), config.serviceToken(), config.timeout());
                },
                8,
                () -> YouTubeConfig.INSTANCE.snapshot().enabled());
        UrlImporterRegistry.INSTANCE.register(importer);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);
    }

    private void serverStopped(ServerStoppedEvent event) {
        importer.close();
    }
}
