package dev.hoodoo.customjukeboxdiscs.proxy;

import dev.hoodoo.customjukeboxdiscs.client.render.TileEntityDiscRackRenderer;
import dev.hoodoo.customjukeboxdiscs.config.ForgeClientConfig;
import dev.hoodoo.customjukeboxdiscs.content.rack.TileEntityDiscRack;
import java.io.File;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ForgeClientConfig.init(new File(event.getModConfigurationDirectory(), "customjukeboxdiscs-client.cfg"));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDiscRack.class, new TileEntityDiscRackRenderer());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        try {
            paulscode.sound.SoundSystemConfig.setCodec("mp3", dev.hoodoo.customjukeboxdiscs.client.audio.CodecMP3.class);
        } catch (Throwable t) {
            // ignore
        }
    }
}
