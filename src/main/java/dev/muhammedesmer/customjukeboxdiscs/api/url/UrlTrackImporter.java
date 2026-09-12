package dev.muhammedesmer.customjukeboxdiscs.api.url;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface UrlTrackImporter {
    String id();

    boolean supports(URI uri);

    CompletableFuture<UrlImportResult> importTrack(UrlImportRequest request);
}
