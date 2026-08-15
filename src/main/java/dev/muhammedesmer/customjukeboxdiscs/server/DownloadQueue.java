package dev.muhammedesmer.customjukeboxdiscs.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serialises track downloads per player so a second jukebox does not silently lose its audio.
 */
public final class DownloadQueue {
    public enum Admission { START, QUEUED, DUPLICATE, REJECTED }

    private final int maxQueued;
    private final Map<UUID, PlayerQueue> players = new HashMap<>();

    public DownloadQueue(int maxQueued) {
        if (maxQueued < 1) {
            throw new IllegalArgumentException("maxQueued must be positive");
        }
        this.maxQueued = maxQueued;
    }

    public synchronized Admission submit(UUID playerId, String sha256) {
        PlayerQueue player = players.computeIfAbsent(playerId, ignored -> new PlayerQueue());
        if (sha256.equals(player.active) || player.waiting.contains(sha256)) {
            return Admission.DUPLICATE;
        }
        if (player.active == null) {
            player.active = sha256;
            return Admission.START;
        }
        if (player.waiting.size() >= maxQueued) {
            return Admission.REJECTED;
        }
        player.waiting.addLast(sha256);
        return Admission.QUEUED;
    }

    public synchronized Optional<String> complete(UUID playerId, String sha256) {
        PlayerQueue player = players.get(playerId);
        if (player == null || !sha256.equals(player.active)) {
            return Optional.empty();
        }
        player.active = player.waiting.pollFirst();
        if (player.active == null) {
            players.remove(playerId);
        }
        return Optional.ofNullable(player.active);
    }

    public synchronized void forget(UUID playerId) {
        players.remove(playerId);
    }

    private static final class PlayerQueue {
        private String active;
        private final Deque<String> waiting = new ArrayDeque<>();
    }
}
