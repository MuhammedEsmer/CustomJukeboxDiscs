package dev.muhammedesmer.customjukeboxdiscs.client.transfer;

import dev.muhammedesmer.customjukeboxdiscs.client.audio.DynamicAudioEngine;
import dev.muhammedesmer.customjukeboxdiscs.client.cache.ClientTrackCache;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.DownloadChunk;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxPlay;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxStop;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackBegin;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackUnavailable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientPlaybackManager {
    public static final ClientPlaybackManager INSTANCE = new ClientPlaybackManager();
    private static final long HARD_MAX_TRACK_BYTES = 100L * 1024L * 1024L;
    private final DynamicAudioEngine audio = new DynamicAudioEngine();
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CustomJukeboxDiscs-ClientIO");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, List<PendingPlay>> pending = new HashMap<>();
    private final Map<String, Integer> retries = new HashMap<>();
    private ClientTrackCache cache;
    private String identity;

    private ClientPlaybackManager() {
    }

    public void play(JukeboxPlay payload) {
        ClientTrackCache current = cache();
        List<PendingPlay> waiting = pending.computeIfAbsent(payload.track().sha256(), ignored -> new ArrayList<>());
        waiting.add(new PendingPlay(payload.pos(), payload.track(), payload.elapsedMillis()));
        if (waiting.size() > 1) return;
        CompletableFuture.supplyAsync(() -> current.find(payload.track().sha256(), payload.track().format()), io)
                .thenAccept(found -> Minecraft.getInstance().execute(() -> found.ifPresentOrElse(
                        file -> complete(payload.track().sha256(), file),
                        () -> PacketDistributor.sendToServer(new TrackRequest(payload.track().sha256())))));
    }

    public void stop(JukeboxStop payload) {
        audio.stop(payload.pos());
        pending.values().forEach(list -> list.removeIf(play -> play.pos.equals(payload.pos())));
    }

    public void begin(TrackBegin payload) {
        ClientTrackCache current = cache();
        io.execute(() -> {
            try { current.begin(payload.sha256(), payload.size(), payload.format(), HARD_MAX_TRACK_BYTES); }
            catch (IOException exception) { Minecraft.getInstance().execute(() -> pending.remove(payload.sha256())); }
        });
    }

    public void chunk(DownloadChunk payload) {
        ClientTrackCache current = cache();
        io.execute(() -> {
            try {
                Optional<Path> completed = current.append(
                        payload.sha256(), payload.offset(), payload.bytes(), payload.last());
                completed.ifPresent(path -> Minecraft.getInstance().execute(() -> complete(payload.sha256(), path)));
            } catch (IOException exception) {
                current.cancel(payload.sha256());
                Minecraft.getInstance().execute(() -> retryOrFail(payload.sha256()));
            }
        });
    }

    public void unavailable(TrackUnavailable payload) {
        ClientTrackCache current = cache();
        io.execute(() -> current.cancel(payload.sha256()));
        pending.remove(payload.sha256());
        retries.remove(payload.sha256());
    }

    public void reset() {
        audio.stopAll();
        pending.clear();
        retries.clear();
        if (cache != null) {
            ClientTrackCache current = cache;
            io.execute(current::cancelAll);
        }
    }

    private void retryOrFail(String hash) {
        int attempts = retries.merge(hash, 1, Integer::sum);
        if (attempts <= 1 && pending.containsKey(hash)) PacketDistributor.sendToServer(new TrackRequest(hash));
        else { pending.remove(hash); retries.remove(hash); }
    }

    private void complete(String hash, Path path) {
        List<PendingPlay> waiting = pending.remove(hash);
        retries.remove(hash);
        if (waiting != null) waiting.forEach(play -> audio.play(play.pos, play.track, path, play.elapsedMillis));
    }

    private ClientTrackCache cache() {
        Minecraft minecraft = Minecraft.getInstance();
        String currentIdentity = minecraft.getCurrentServer() == null ? "singleplayer" : minecraft.getCurrentServer().ip;
        if (cache == null || !currentIdentity.equals(identity)) {
            reset();
            identity = currentIdentity;
            cache = new ClientTrackCache(minecraft.gameDirectory.toPath(), identity,
                    ClientConfig.INSTANCE.snapshot().maxCacheBytes());
        }
        return cache;
    }

    private record PendingPlay(BlockPos pos, TrackReference track, long elapsedMillis) {
    }
}
