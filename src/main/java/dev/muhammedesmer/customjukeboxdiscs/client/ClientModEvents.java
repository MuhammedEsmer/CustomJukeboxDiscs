package dev.muhammedesmer.customjukeboxdiscs.client;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.screen.DiscWriterScreen;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.muhammedesmer.customjukeboxdiscs.content.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CustomJukeboxDiscs.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        dev.muhammedesmer.customjukeboxdiscs.network.ModPayloads.installClientHandler(ClientUploadManager.INSTANCE);
        event.register(ModMenus.DISC_WRITER.get(), DiscWriterScreen::new);
    }
}
