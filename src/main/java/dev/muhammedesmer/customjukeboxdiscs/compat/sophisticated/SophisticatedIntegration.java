package dev.muhammedesmer.customjukeboxdiscs.compat.sophisticated;

import dev.muhammedesmer.customjukeboxdiscs.compat.SophisticatedPlayback;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.DiscHandlerRegistry;

/**
 * Loaded only when Sophisticated Core is present; see {@code CustomJukeboxDiscs}.
 */
public final class SophisticatedIntegration {
    public static final String MOD_ID = "sophisticatedcore";

    private SophisticatedIntegration() {
    }

    public static void install(IEventBus gameBus) {
        // The bundled vanilla handler claims every JUKEBOX_PLAYABLE stack, and the registry picks the
        // first match, so a plain registerHandler call would never be reached for a programmed disc.
        DiscHandlerRegistry.getHandlers().addFirst(new ProgrammedDiscHandler());
        gameBus.addListener(SophisticatedIntegration::serverTick);
    }

    private static void serverTick(ServerTickEvent.Post event) {
        SophisticatedPlayback.expireFinished(event.getServer());
    }
}
