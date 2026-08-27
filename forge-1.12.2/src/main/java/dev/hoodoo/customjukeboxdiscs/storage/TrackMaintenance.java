package dev.hoodoo.customjukeboxdiscs.storage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TrackMaintenance {
    private final TrackCatalogSavedData catalog;
    private final FileTrackStorage storage;

    public TrackMaintenance(TrackCatalogSavedData catalog, FileTrackStorage storage) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public boolean delete(String sha256) throws IOException {
        boolean removed = catalog.remove(sha256).isPresent();
        storage.delete(sha256);
        return removed;
    }

    public RecoveryReport recover() throws IOException {
        storage.cleanupTemporaryFiles();
        List<String> stored = storage.listStoredHashes();
        List<String> missingAudio = catalog.tracks().values().stream()
                .filter(metadata -> !storage.find(
                        metadata.reference().sha256(), metadata.reference().format()).isPresent())
                .map(metadata -> metadata.reference().sha256())
                .sorted()
                .collect(Collectors.toList());
        List<String> unreferenced = stored.stream()
                .filter(hash -> !catalog.find(hash).isPresent())
                .collect(Collectors.toList());
        return new RecoveryReport(missingAudio, unreferenced);
    }

    public static final class RecoveryReport {
        private final List<String> missingAudio;
        private final List<String> unreferencedAudio;

        public RecoveryReport(List<String> missingAudio, List<String> unreferencedAudio) {
            this.missingAudio = missingAudio;
            this.unreferencedAudio = unreferencedAudio;
        }

        public List<String> missingAudio() { return missingAudio; }
        public List<String> getMissingAudio() { return missingAudio; }
        public List<String> unreferencedAudio() { return unreferencedAudio; }
        public List<String> getUnreferencedAudio() { return unreferencedAudio; }

        public boolean isClean() {
            return missingAudio.isEmpty() && unreferencedAudio.isEmpty();
        }
    }
}
