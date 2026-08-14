package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRAR = DeferredRegister.create(Registries.SOUND_EVENT, CustomJukeboxDiscs.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> DYNAMIC_TRACK = REGISTRAR.register(
            "dynamic_track", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "dynamic_track")));
    private ModSounds() { }
}
