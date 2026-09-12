package dev.muhammedesmer.customjukeboxdiscs.api.url;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class UrlImporterRegistry {
    public static final UrlImporterRegistry INSTANCE = new UrlImporterRegistry();

    private final List<UrlTrackImporter> importers = new ArrayList<>();

    public synchronized void register(UrlTrackImporter importer) {
        Objects.requireNonNull(importer, "importer");
        if (importer.id() == null || importer.id().isBlank()) {
            throw new IllegalArgumentException("importer id must not be blank");
        }
        if (importers.stream().anyMatch(existing -> existing.id().equals(importer.id()))) {
            throw new IllegalArgumentException("duplicate URL importer id: " + importer.id());
        }
        importers.add(importer);
    }

    public synchronized Optional<UrlTrackImporter> find(URI uri) {
        Objects.requireNonNull(uri, "uri");
        UrlTrackImporter match = null;
        for (UrlTrackImporter importer : importers) {
            if (!importer.supports(uri)) continue;
            if (match != null) {
                throw new IllegalStateException("multiple URL importers support " + uri.getHost());
            }
            match = importer;
        }
        return Optional.ofNullable(match);
    }
}
