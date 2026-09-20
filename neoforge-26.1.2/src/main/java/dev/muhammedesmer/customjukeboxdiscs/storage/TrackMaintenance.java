package dev.muhammedesmer.customjukeboxdiscs.storage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Operator-facing deletion and conservative start-up repair of the track store.
 */
public final class TrackMaintenance {
    private final TrackCatalogData catalog;
    private final FileTrackStorage storage;

    public TrackMaintenance(TrackCatalogData catalog, FileTrackStorage storage) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /** {@return whether a catalogued track was removed from the catalog and from disk} */
    public boolean delete(String hashOrPrefix) throws IOException {
        if (hashOrPrefix == null || !hashOrPrefix.matches("[0-9a-f]{12,64}")) return false;
        List<String> matches = catalog.tracks().keySet().stream()
                .filter(hash -> hash.startsWith(hashOrPrefix))
                .limit(2)
                .toList();
        if (matches.size() != 1) return false;
        String sha256 = matches.getFirst();
        catalog.remove(sha256);
        storage.delete(sha256);
        return true;
    }

    /**
     * Deletes leftover temporary uploads and reports every mismatch between the catalog and the audio store.
     * Validated audio is never deleted automatically.
     */
    public RecoveryReport recover() throws IOException {
        storage.cleanupTemporaryFiles();
        List<String> stored = storage.listStoredHashes();
        List<String> missingAudio = catalog.tracks().values().stream()
                .filter(metadata -> storage.find(
                        metadata.reference().sha256(), metadata.reference().format()).isEmpty())
                .map(metadata -> metadata.reference().sha256())
                .sorted()
                .toList();
        List<String> unreferenced = stored.stream()
                .filter(hash -> catalog.find(hash).isEmpty())
                .toList();
        return new RecoveryReport(missingAudio, unreferenced);
    }

    public record RecoveryReport(List<String> missingAudio, List<String> unreferencedAudio) {
        public boolean isClean() {
            return missingAudio.isEmpty() && unreferencedAudio.isEmpty();
        }
    }
}
