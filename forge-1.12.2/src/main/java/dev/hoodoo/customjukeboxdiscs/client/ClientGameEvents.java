package dev.hoodoo.customjukeboxdiscs.client;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class ClientGameEvents {
    public static final KeyBinding TOGGLE_PLAYBACK = new KeyBinding(
            "key.customjukeboxdiscs.toggle_playback",
            Keyboard.KEY_J,
            "key.categories.customjukeboxdiscs");

    private ClientGameEvents() {
    }

    public static void tick() {
        while (TOGGLE_PLAYBACK.isPressed()) {
            ClientPlaybackPreference.toggle();
        }
    }
}
