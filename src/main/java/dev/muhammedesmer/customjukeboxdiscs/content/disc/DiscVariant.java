package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import net.minecraft.util.RandomSource;

/**
 * The interchangeable looks of a programmed disc. A disc picks one at random when it is written, so a
 * shelf of tracks is easy to tell apart at a glance.
 */
public final class DiscVariant {
    public static final int COUNT = 12;

    private DiscVariant() {
    }

    public static int random(RandomSource random) {
        return random.nextInt(COUNT);
    }

    /** {@return the variant itself, or the first design when the stored value is out of range} */
    public static int clamp(int variant) {
        return variant >= 0 && variant < COUNT ? variant : 0;
    }
}
