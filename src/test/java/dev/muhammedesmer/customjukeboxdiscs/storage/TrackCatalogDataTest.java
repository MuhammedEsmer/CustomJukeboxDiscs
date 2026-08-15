package dev.muhammedesmer.customjukeboxdiscs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TrackCatalogDataTest {
    private static final UUID OWNER = UUID.fromString("12345678-1234-5678-9234-567812345678");

    @Test
    void catalogsTracksAndCalculatesOwnerAndServerUsage() {
        TrackCatalogData catalog = new TrackCatalogData();
        TrackMetadata first = metadata("a".repeat(64), 100);
        TrackMetadata second = metadata("b".repeat(64), 250);

        assertTrue(catalog.add(first));
        assertTrue(catalog.add(second));
        assertFalse(catalog.add(first));

        assertEquals(2, catalog.trackCount(OWNER));
        assertEquals(350, catalog.byteCount(OWNER));
        assertEquals(350, catalog.totalByteCount());
        assertTrue(catalog.isDirty());
    }

    @Test
    void codecRoundTripsCatalog() {
        TrackCatalogData catalog = new TrackCatalogData();
        TrackMetadata metadata = metadata("c".repeat(64), 400);
        catalog.add(metadata);

        JsonElement encoded = TrackCatalogData.CODEC.encodeStart(JsonOps.INSTANCE, catalog).getOrThrow();
        TrackCatalogData decoded = TrackCatalogData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(metadata, decoded.find(metadata.reference().sha256()).orElseThrow());
        assertEquals(1, decoded.tracks().size());
    }

    @Test
    void pagesTracksOldestFirst() {
        TrackCatalogData catalog = new TrackCatalogData();
        catalog.add(metadata("c".repeat(64), 100, "2026-08-14T00:00:03Z"));
        catalog.add(metadata("a".repeat(64), 100, "2026-08-14T00:00:01Z"));
        catalog.add(metadata("b".repeat(64), 100, "2026-08-14T00:00:02Z"));

        TrackCatalogData.CatalogPage page = catalog.page(1, 2);

        assertEquals(2, page.pageCount());
        assertEquals(3, page.totalTracks());
        assertEquals(java.util.List.of("a".repeat(64), "b".repeat(64)),
                page.entries().stream().map(entry -> entry.reference().sha256()).toList());
    }

    @Test
    void returnsTheRemainderOnTheLastPage() {
        TrackCatalogData catalog = new TrackCatalogData();
        catalog.add(metadata("a".repeat(64), 100, "2026-08-14T00:00:01Z"));
        catalog.add(metadata("b".repeat(64), 100, "2026-08-14T00:00:02Z"));
        catalog.add(metadata("c".repeat(64), 100, "2026-08-14T00:00:03Z"));

        assertEquals(1, catalog.page(2, 2).entries().size());
    }

    @Test
    void reportsAnEmptyPageBeyondTheLastOne() {
        TrackCatalogData catalog = new TrackCatalogData();
        catalog.add(metadata("a".repeat(64), 100, "2026-08-14T00:00:01Z"));

        assertTrue(catalog.page(5, 2).entries().isEmpty());
    }

    @Test
    void reportsOnePageForAnEmptyCatalog() {
        assertEquals(1, new TrackCatalogData().page(1, 10).pageCount());
    }

    private static TrackMetadata metadata(String hash, long bytes) {
        return metadata(hash, bytes, "2026-08-14T00:00:00Z");
    }

    private static TrackMetadata metadata(String hash, long bytes, String createdAt) {
        TrackReference reference = new TrackReference(
                hash, "Track", OWNER, "Player", 1_000, AudioFormat.OGG);
        return new TrackMetadata(reference, bytes, Instant.parse(createdAt));
    }
}
