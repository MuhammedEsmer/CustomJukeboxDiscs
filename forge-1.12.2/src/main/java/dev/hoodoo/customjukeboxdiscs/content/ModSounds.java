package dev.hoodoo.customjukeboxdiscs.content;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID)
public final class ModSounds {
    public static final SoundEvent DYNAMIC_TRACK = createSoundEvent("dynamic_track");

    private ModSounds() {
    }

    private static SoundEvent createSoundEvent(String name) {
        ResourceLocation id = new ResourceLocation(ModItems.MOD_ID, name);
        SoundEvent event = new SoundEvent(id);
        event.setRegistryName(id);
        return event;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(DYNAMIC_TRACK);
    }
}
