package dev.hoodoo.customjukeboxdiscs.proxy;

import dev.hoodoo.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.hoodoo.customjukeboxdiscs.client.GuiHandler;
import dev.hoodoo.customjukeboxdiscs.command.CommandCustomDiscs;
import dev.hoodoo.customjukeboxdiscs.config.ForgeServerConfig;
import dev.hoodoo.customjukeboxdiscs.network.ModNetwork;
import dev.hoodoo.customjukeboxdiscs.server.JukeboxEventHandler;
import dev.hoodoo.customjukeboxdiscs.server.ServerRuntime;
import java.io.File;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ForgeServerConfig.init(new File(event.getModConfigurationDirectory(), "customjukeboxdiscs-server.cfg"));
        ModNetwork.init();
        MinecraftForge.EVENT_BUS.register(new JukeboxEventHandler());
    }

    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(CustomJukeboxDiscs.instance, new GuiHandler());
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCustomDiscs());
        ServerRuntime.onServerStarting(event.getServer());
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        ServerRuntime.onServerStopped();
    }
}
