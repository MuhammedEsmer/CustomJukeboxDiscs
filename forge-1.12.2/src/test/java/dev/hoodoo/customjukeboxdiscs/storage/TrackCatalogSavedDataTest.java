package dev.hoodoo.customjukeboxdiscs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TrackCatalogSavedDataTest {
    @Test
    void pageClamped_RequestBeyondLastPage_ReturnsLastPage() {
        TrackCatalogSavedData catalog = new TrackCatalogSavedData();
        catalog.add(metadata("a", "2026-08-14T00:00:01Z"));
        catalog.add(metadata("b", "2026-08-14T00:00:02Z"));
        catalog.add(metadata("c", "2026-08-14T00:00:03Z"));

        TrackCatalogSavedData.CatalogPage actual = catalog.pageClamped(99, 2);

        assertEquals(2, actual.page());
        assertEquals(hash("c"), actual.entries().get(0).reference().sha256());
    }

    private static TrackMetadata metadata(String hashCharacter, String createdAt) {
        TrackReference track = new TrackReference(hash(hashCharacter), "Track",
                UUID.fromString("12345678-1234-5678-9234-567812345678"), "Player", 1_000L, AudioFormat.MP3);
        return new TrackMetadata(track, 100L, Instant.parse(createdAt));
    }

    private static String hash(String character) {
        return String.join("", java.util.Collections.nCopies(64, character));
    }
}
