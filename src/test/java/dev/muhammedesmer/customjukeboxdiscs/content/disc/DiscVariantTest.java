package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

final class DiscVariantTest {
    @Test
    void everyVariantHasATextureAndAModel() {
        Path assets = Path.of(System.getProperty("customjukeboxdiscs.projectDir"),
                "src/main/resources/assets/customjukeboxdiscs");

        for (int variant = 0; variant < DiscVariant.COUNT; variant++) {
            assertTrue(Files.isRegularFile(assets.resolve("textures/item/programmed_disc_" + variant + ".png")),
                    "missing texture for variant " + variant);
            assertTrue(Files.isRegularFile(assets.resolve("models/item/programmed_disc_" + variant + ".json")),
                    "missing model for variant " + variant);
        }
    }

    @Test
    void randomVariantsStayInsideTheAvailableRange() {
        RandomSource random = RandomSource.create(1234L);

        for (int attempt = 0; attempt < 500; attempt++) {
            int variant = DiscVariant.random(random);
            assertTrue(variant >= 0 && variant < DiscVariant.COUNT, "out of range: " + variant);
        }
    }

    @Test
    void randomVariantsCoverEveryDesign() {
        RandomSource random = RandomSource.create(9876L);
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
