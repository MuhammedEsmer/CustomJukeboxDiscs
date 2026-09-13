package dev.hoodoo.customjukeboxdiscs.youtube;

import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;

@FunctionalInterface
public interface TrackImportClient {
    UrlImportResult importTrack(String videoId, UrlImportRequest request);
}
