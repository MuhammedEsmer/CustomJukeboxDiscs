package dev.muhammedesmer.customjukeboxdiscs.client;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.render.DiscRackRenderer;
import dev.muhammedesmer.customjukeboxdiscs.client.render.DiscVariantProperty;
import dev.muhammedesmer.customjukeboxdiscs.client.screen.DiscRackScreen;
import dev.muhammedesmer.customjukeboxdiscs.client.screen.DiscWriterScreen;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.muhammedesmer.customjukeboxdiscs.content.ModBlockEntities;
import dev.muhammedesmer.customjukeboxdiscs.content.ModMenus;
import dev.muhammedesmer.customjukeboxdiscs.network.ModPayloads;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

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
        event.registerCategory(ClientGameEvents.CATEGORY);
        event.register(ClientGameEvents.TOGGLE_PLAYBACK);
    }

    @SubscribeEvent
    public static void registerItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "disc_variant"),
                DiscVariantProperty.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DISC_RACK.get(), DiscRackRenderer::new);
    }
}
