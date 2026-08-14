package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CustomJukeboxDiscs.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TrackReference>> TRACK_REFERENCE =
            REGISTRAR.registerComponentType("track_reference", builder -> builder
                    .persistent(TrackReference.CODEC)
                    .networkSynchronized(TrackReference.STREAM_CODEC)
                    .cacheEncoding());

    private ModDataComponents() {
    }
}
