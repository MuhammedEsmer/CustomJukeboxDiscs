package dev.hoodoo.customjukeboxdiscs.content.disc;

import java.util.Random;

public final class DiscVariant {
    public static final int COUNT = 12;

    private DiscVariant() {
    }

    public static int random(Random random) {
        return random.nextInt(COUNT);
    }

    public static int clamp(int variant) {
        return variant >= 0 && variant < COUNT ? variant : 0;
    }
}
