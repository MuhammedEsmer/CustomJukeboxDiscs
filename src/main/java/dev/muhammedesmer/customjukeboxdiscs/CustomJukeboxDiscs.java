package dev.muhammedesmer.customjukeboxdiscs;

import dev.muhammedesmer.customjukeboxdiscs.content.ModCreativeTabs;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.ModBlockEntities;
import dev.muhammedesmer.customjukeboxdiscs.content.ModBlocks;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.ModMenus;
import dev.muhammedesmer.customjukeboxdiscs.content.ModSounds;
import dev.muhammedesmer.customjukeboxdiscs.compat.sophisticated.SophisticatedIntegration;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.config.ServerConfig;
import dev.muhammedesmer.customjukeboxdiscs.network.ModPayloads;
import dev.muhammedesmer.customjukeboxdiscs.server.ServerRuntime;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CustomJukeboxDiscs.MOD_ID)
public final class CustomJukeboxDiscs {
    public static final String MOD_ID = "customjukeboxdiscs";

    public CustomJukeboxDiscs(IEventBus modBus, ModContainer modContainer) {
        ModDataComponents.REGISTRAR.register(modBus);
        ModBlocks.REGISTRAR.register(modBus);
        ModItems.REGISTRAR.register(modBus);
        ModBlockEntities.REGISTRAR.register(modBus);
        ModMenus.REGISTRAR.register(modBus);
        ModSounds.REGISTRAR.register(modBus);
        ModCreativeTabs.REGISTRAR.register(modBus);
        modBus.addListener(ModPayloads::register);
        ServerRuntime.install(NeoForge.EVENT_BUS);
        if (ModList.get().isLoaded(SophisticatedIntegration.MOD_ID)) {
            SophisticatedIntegration.install(NeoForge.EVENT_BUS);
        }
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

}
