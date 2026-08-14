package dev.muhammedesmer.customjukeboxdiscs;

import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CustomJukeboxDiscs.MOD_ID)
public final class CustomJukeboxDiscs {
    public static final String MOD_ID = "customjukeboxdiscs";

    public CustomJukeboxDiscs(IEventBus modBus, ModContainer modContainer) {
        ModDataComponents.REGISTRAR.register(modBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
