package dev.muhammedesmer.customjukeboxdiscs;

import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CustomJukeboxDiscs.MOD_ID)
public final class CustomJukeboxDiscs {
    public static final String MOD_ID = "customjukeboxdiscs";

    public CustomJukeboxDiscs(IEventBus modBus) {
        ModDataComponents.REGISTRAR.register(modBus);
    }
}
