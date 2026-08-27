package dev.hoodoo.customjukeboxdiscs.client.transfer;

import dev.hoodoo.customjukeboxdiscs.client.ClientPlaybackPreference;
import dev.hoodoo.customjukeboxdiscs.client.audio.DynamicAudioEngine;
import dev.hoodoo.customjukeboxdiscs.client.audio.TrackTimeline;
import dev.hoodoo.customjukeboxdiscs.client.cache.ClientTrackCache;
import dev.hoodoo.customjukeboxdiscs.config.ForgeClientConfig;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.network.ModNetwork;
import dev.hoodoo.customjukeboxdiscs.network.PlaybackAnchor;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketDownloadChunk;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackBegin;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackRequest;
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

public final class ClientPlaybackManager {
    private static final ClientPlaybackManager INSTANCE = new ClientPlaybackManager();
    private static final long HARD_MAX_TRACK_BYTES = 100L * 1024L * 1024L;

    private final DynamicAudioEngine audio = new DynamicAudioEngine();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CustomJukeboxDiscs-ClientIO");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, List<PendingPlay>> pending = new HashMap<>();
    private final Map<String, Integer> retries = new HashMap<>();
    private ClientTrackCache cache;
    private String identity;

    private ClientPlaybackManager() {
    }

    public static ClientPlaybackManager getInstance() {
        return INSTANCE;
    }

    public DynamicAudioEngine getAudioEngine() {
        return audio;
    }

    public void handlePlay(PlaybackAnchor anchor, TrackReference track, long startOffsetNanos) {
        if (!ClientPlaybackPreference.enabled()) {
            return;
        }
        ClientTrackCache current = cache();
        List<PendingPlay> waiting = pending.computeIfAbsent(track.getSha256(), k -> new ArrayList<>());
        long elapsedMillis = startOffsetNanos > 0 ? (System.nanoTime() - startOffsetNanos) / 1_000_000L : 0L;
        waiting.add(new PendingPlay(anchor, track, TrackTimeline.startingFrom(
                System.nanoTime(), elapsedMillis, track.getDurationMillis())));
        if (waiting.size() > 1) return;

        CompletableFuture.supplyAsync(() -> current.find(track.getSha256(), track.getFormat()), io)
                .thenAccept(found -> Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (found.isPresent()) {
                        complete(track.getSha256(), found.get());
                    } else {
                        ModNetwork.CHANNEL.sendToServer(new PacketTrackRequest(track.getSha256()));
                    }
                }));
    }

    public void handleStop(PlaybackAnchor anchor) {
        audio.stop(anchor);
        for (List<PendingPlay> list : pending.values()) {
            list.removeIf(play -> play.anchor.equals(anchor));
        }
    }

    public void handleTrackBegin(PacketTrackBegin packet) {
        ClientTrackCache current = cache();
        io.execute(() -> {
            try {
                // Determine format from pending play or default to MP3
                List<PendingPlay> plays = pending.get(packet.getSha256());
                dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat format =
                        (plays != null && !plays.isEmpty()) ? plays.get(0).track.getFormat() : dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat.MP3;
                current.begin(packet.getSha256(), packet.getTotalBytes(), format, HARD_MAX_TRACK_BYTES);
            } catch (IOException exception) {
                Minecraft.getMinecraft().addScheduledTask(() -> pending.remove(packet.getSha256()));
            }
        });
    }

    public void handleDownloadChunk(PacketDownloadChunk packet) {
        ClientTrackCache current = cache();
        io.execute(() -> {
            try {
                // Check if last chunk
                Optional<Path> completed = current.append(
                        packet.getSha256(), packet.getChunkIndex() * 32768L, packet.getBytes(), packet.getBytes().length < 32768);
                if (completed.isPresent()) {
                    Minecraft.getMinecraft().addScheduledTask(() -> complete(packet.getSha256(), completed.get()));
                }
            } catch (IOException exception) {
                current.cancel(packet.getSha256());
                Minecraft.getMinecraft().addScheduledTask(() -> retryOrFail(packet.getSha256()));
            }
        });
    }

    public void handleTrackUnavailable(String sha256) {
        ClientTrackCache current = cache();
        io.execute(() -> current.cancel(sha256));
        pending.remove(sha256);
        retries.remove(sha256);
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
        if (attempts <= 1 && pending.containsKey(hash)) {
            ModNetwork.CHANNEL.sendToServer(new PacketTrackRequest(hash));
        } else {
            pending.remove(hash);
            retries.remove(hash);
        }
    }

    private void complete(String hash, Path path) {
        List<PendingPlay> waiting = pending.remove(hash);
        retries.remove(hash);
        if (waiting != null) {
            for (PendingPlay play : waiting) {
                audio.play(play.anchor, play.track, path, play.timeline);
                if (Minecraft.getMinecraft().player != null) {
                    if (play.anchor.isEntity()) {
                        if (play.anchor.getEntityId() == Minecraft.getMinecraft().player.getEntityId()) {
                            Minecraft.getMinecraft().ingameGUI.setRecordPlayingMessage(play.track.getTitle());
                        }
                    } else if (play.anchor.getPos() != null) {
                        double distSq = Minecraft.getMinecraft().player.getDistanceSq(
                                play.anchor.getPos().getX() + 0.5D,
                                play.anchor.getPos().getY() + 0.5D,
                                play.anchor.getPos().getZ() + 0.5D);
                        if (distSq <= 64.0D * 64.0D) {
                            Minecraft.getMinecraft().ingameGUI.setRecordPlayingMessage(play.track.getTitle());
                        }
                    }
                }
            }
        }
    }

    private ClientTrackCache cache() {
        Minecraft minecraft = Minecraft.getMinecraft();
        String currentIdentity = minecraft.getCurrentServerData() == null ? "singleplayer" : minecraft.getCurrentServerData().serverIP;
        if (cache == null || !currentIdentity.equals(identity)) {
            reset();
            identity = currentIdentity;
            cache = new ClientTrackCache(minecraft.gameDir.toPath(), identity, ForgeClientConfig.maxCacheBytes);
        }
        return cache;
    }

    private static final class PendingPlay {
        final PlaybackAnchor anchor;
        final TrackReference track;
        final TrackTimeline timeline;

        PendingPlay(PlaybackAnchor anchor, TrackReference track, TrackTimeline timeline) {
            this.anchor = anchor;
            this.track = track;
            this.timeline = timeline;
        }
    }
}
