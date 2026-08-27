package dev.hoodoo.customjukeboxdiscs.content.disc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DiscVariantTest {
    @Test
    void randomVariantsStayInsideTheAvailableRange() {
        Random random = new Random(1234L);

        for (int attempt = 0; attempt < 500; attempt++) {
            int variant = DiscVariant.random(random);
            assertTrue(variant >= 0 && variant < DiscVariant.COUNT, "out of range: " + variant);
        }
    }

    @Test
    void randomVariantsCoverEveryDesign() {
        Random random = new Random(9876L);
        Set<Integer> seen = new HashSet<>();

        for (int attempt = 0; attempt < 2_000; attempt++) {
            seen.add(DiscVariant.random(random));
        }

        assertEquals(DiscVariant.COUNT, seen.size(), "every disc design should be reachable");
    }

    @Test
    void unknownVariantsFallBackToTheFirstDesign() {
        assertEquals(0, DiscVariant.clamp(-1));
        assertEquals(0, DiscVariant.clamp(DiscVariant.COUNT));
        assertEquals(3, DiscVariant.clamp(3));
    }
}
