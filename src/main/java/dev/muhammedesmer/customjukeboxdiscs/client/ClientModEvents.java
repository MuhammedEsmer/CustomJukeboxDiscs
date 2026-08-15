package dev.muhammedesmer.customjukeboxdiscs.client;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.render.DiscRackRenderer;
import dev.muhammedesmer.customjukeboxdiscs.client.screen.DiscRackScreen;
import dev.muhammedesmer.customjukeboxdiscs.client.screen.DiscWriterScreen;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.muhammedesmer.customjukeboxdiscs.content.ModBlockEntities;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.ModMenus;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.DiscVariant;
import dev.muhammedesmer.customjukeboxdiscs.network.ModPayloads;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CustomJukeboxDiscs.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ModPayloads.installClientHandler(ClientUploadManager.INSTANCE);
        event.register(ModMenus.DISC_WRITER.get(), DiscWriterScreen::new);
        event.register(ModMenus.DISC_RACK.get(), DiscRackScreen::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientGameEvents.TOGGLE_PLAYBACK);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DISC_RACK.get(), DiscRackRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.PROGRAMMED_DISC.get(),
                ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "variant"),
                (stack, level, entity, seed) ->
                        DiscVariant.clamp(stack.getOrDefault(ModDataComponents.DISC_VARIANT, 0))));
    }
}
