package dev.hoodoo.customjukeboxdiscs;

import dev.hoodoo.customjukeboxdiscs.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

@Mod(
    modid = CustomJukeboxDiscs.MOD_ID,
    name = CustomJukeboxDiscs.NAME,
    version = CustomJukeboxDiscs.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class CustomJukeboxDiscs {
    public static final String MOD_ID = "customjukeboxdiscs";
    public static final String NAME = "Custom Jukebox Discs";
    public static final String VERSION = "0.2.8";

    @Mod.Instance(MOD_ID)
    public static CustomJukeboxDiscs instance;

    @SidedProxy(
        clientSide = "dev.hoodoo.customjukeboxdiscs.proxy.ClientProxy",
        serverSide = "dev.hoodoo.customjukeboxdiscs.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }
}
