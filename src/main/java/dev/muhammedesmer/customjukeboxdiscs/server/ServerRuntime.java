package dev.muhammedesmer.customjukeboxdiscs.server;

import dev.muhammedesmer.customjukeboxdiscs.config.ServerConfig;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterBlockEntity;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterMenu;
import dev.muhammedesmer.customjukeboxdiscs.network.ModPayloads;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadBeginRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadBeginResponse;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadChunk;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadFinish;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UploadResult;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.UrlUploadRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.DownloadChunk;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxPlay;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxStop;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackAnchor;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackPreference;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackBegin;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackRequest;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackUnavailable;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessPolicyData;
import dev.muhammedesmer.customjukeboxdiscs.permission.DefaultAccessService;
import dev.muhammedesmer.customjukeboxdiscs.permission.AccessService;
import dev.muhammedesmer.customjukeboxdiscs.command.CustomDiscsCommand;
import dev.muhammedesmer.customjukeboxdiscs.storage.BoundedAudioInspector;
import dev.muhammedesmer.customjukeboxdiscs.storage.FileTrackStorage;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackCatalogData;
import dev.muhammedesmer.customjukeboxdiscs.storage.TrackMaintenance;
import dev.muhammedesmer.customjukeboxdiscs.transfer.BeginUpload;
import dev.muhammedesmer.customjukeboxdiscs.transfer.BeginUploadResult;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import dev.muhammedesmer.customjukeboxdiscs.transfer.TrackUrlFetcher;
import dev.muhammedesmer.customjukeboxdiscs.transfer.TrackUrlPolicy;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadManager;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.io.InputStream;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerRuntime implements ModPayloads.ServerHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("CustomJukeboxDiscs");
    private static final int MAX_QUEUED_DOWNLOADS = 4;
    private static final String CATALOG_KEY = "customjukeboxdiscs_tracks";
    private static final String ACCESS_KEY = "customjukeboxdiscs_access";
    private static ServerRuntime instance;

    private final UploadManager uploads;
    private final ExecutorService ioExecutor;
    private final ExecutorService uploadIoExecutor;
    private final FileTrackStorage trackStorage;
    private final TrackCatalogData catalog;
    private final DefaultAccessService accessService;
    private final TrackMaintenance maintenance;
    private final Map<UUID, WriterReservation> writers = new HashMap<>();
    private final Map<ActiveKey, ActivePlayback> activePlaybacks = new HashMap<>();
    private final MinecraftServer server;
    private final DownloadQueue downloads = new DownloadQueue(MAX_QUEUED_DOWNLOADS);
    private final Set<UUID> playbackDisabled = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ServerRuntime(MinecraftServer server) {
        this.server = server;
        var storage = server.overworld().getDataStorage();
        catalog = storage.computeIfAbsent(
                new SavedData.Factory<>(TrackCatalogData::new, TrackCatalogData::load, DataFixTypes.LEVEL), CATALOG_KEY);
        AccessPolicyData access = storage.computeIfAbsent(
                new SavedData.Factory<>(AccessPolicyData::new, AccessPolicyData::load, DataFixTypes.LEVEL), ACCESS_KEY);
        ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        uploadIoExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CustomJukeboxDiscs-UploadIO");
            thread.setDaemon(true);
            return thread;
        });
        trackStorage = new FileTrackStorage(server.getWorldPath(LevelResource.ROOT));
        accessService = new DefaultAccessService(access);
        uploads = new UploadManager(
                accessService,
                trackStorage,
                catalog,
                new BoundedAudioInspector(),
                ServerConfig.INSTANCE.snapshot(),
                System::nanoTime,
                ioExecutor,
                server::execute);
        maintenance = new TrackMaintenance(catalog, trackStorage);
        ioExecutor.execute(this::recover);
    }

    private void recover() {
        try {
            TrackMaintenance.RecoveryReport report = maintenance.recover();
            if (report.isClean()) return;
            report.missingAudio().forEach(hash ->
                    LOGGER.warn("Catalogued track {} has no audio file and will report as unavailable", hash));
            report.unreferencedAudio().forEach(hash ->
                    LOGGER.warn("Audio file {} is not referenced by the catalog; delete it manually if unwanted", hash));
        } catch (java.io.IOException exception) {
            LOGGER.error("Track storage recovery failed", exception);
        }
    }

    public static void install(IEventBus eventBus) {
        eventBus.addListener(ServerRuntime::started);
        eventBus.addListener(ServerRuntime::stopped);
        eventBus.addListener(ServerRuntime::loggedOut);
        eventBus.addListener(CustomDiscsCommand::register);
        eventBus.addListener(ServerRuntime::serverTick);
    }

    private static void started(ServerStartedEvent event) {
        instance = new ServerRuntime(event.getServer());
        ModPayloads.installServerHandler(instance);
    }

    private static void stopped(ServerStoppedEvent event) {
        if (instance != null) {
            instance.ioExecutor.close();
            instance.uploadIoExecutor.close();
            instance = null;
        }
        ModPayloads.installServerHandler(ModPayloads.ServerHandler.NOT_READY);
    }

    private static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (instance != null && event.getEntity() instanceof ServerPlayer player) {
            instance.uploads.cancelAll(player.getUUID());
            instance.writers.entrySet().removeIf(entry -> entry.getValue().playerId.equals(player.getUUID()));
            instance.activePlaybacks.values().forEach(playback -> playback.listeners.remove(player.getUUID()));
            instance.downloads.forget(player.getUUID());
            instance.playbackDisabled.remove(player.getUUID());
        }
    }

    @Override
    public void handle(UploadBeginRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> begin(payload, requirePlayer(context)));
    }

    private void begin(UploadBeginRequest payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof DiscWriterMenu menu)
                || !(menu.writer() instanceof DiscWriterBlockEntity writer)
                || writer.inputFingerprint() != payload.inputFingerprint()) {
            send(player, rejected(UploadError.INVALID_WRITER));
            return;
        }
        BeginUploadResult result = uploads.begin(
                player.getUUID(), player.hasPermissions(3) ? 3 : 0,
                new BeginUpload(payload.clientHash(), payload.declaredBytes(), payload.formatHint(),
                        payload.title(), player.getGameProfile().getName()));
        if (result.alreadyPresent()) {
            if (writer.writeDisc(payload.inputFingerprint(), result.existingTrack())) {
                send(player, new UploadBeginResponse(
                        UploadBeginResponse.Status.ALREADY_PRESENT, UploadError.NONE, null, 0, result.existingTrack()));
            } else {
                send(player, rejected(UploadError.INVALID_WRITER));
            }
        } else if (result.accepted()) {
            writers.put(result.sessionId(), new WriterReservation(
                    player.getUUID(), player, (ServerLevel) writer.getLevel(), writer.getBlockPos(), writer, menu,
                    payload.inputFingerprint()));
            send(player, new UploadBeginResponse(
                    UploadBeginResponse.Status.ACCEPTED, UploadError.NONE, result.sessionId(), result.chunkBytes(), null));
        } else {
            send(player, rejected(result.error()));
        }
    }

    @Override
    public void handle(UrlUploadRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> beginUrl(payload, requirePlayer(context)));
    }

    private void beginUrl(UrlUploadRequest payload, ServerPlayer player) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.urlUploadsEnabled()) {
            send(player, new UploadResult(UploadError.URL_NOT_ALLOWED, null));
            return;
        }
        if (!(player.containerMenu instanceof DiscWriterMenu menu)
                || !(menu.writer() instanceof DiscWriterBlockEntity writer)
                || writer.inputFingerprint() != payload.inputFingerprint()) {
            send(player, new UploadResult(UploadError.INVALID_WRITER, null));
            return;
        }
        long maxBytes = config.snapshot().maxSourceBytes();
        TrackUrlFetcher fetcher = new TrackUrlFetcher(
                new TrackUrlPolicy(config.urlAllowedHosts(), config.urlAllowPrivateAddresses()),
                java.time.Duration.ofMillis(config.snapshot().uploadTimeoutMillis()));
        long fingerprint = payload.inputFingerprint();
        UUID playerId = player.getUUID();
        int permissionLevel = player.hasPermissions(3) ? 3 : 0;
        String uploaderName = player.getGameProfile().getName();
        ioExecutor.execute(() -> {
            java.nio.file.Path temporary;
            try {
                temporary = trackStorage.createTemporary(UUID.randomUUID());
            } catch (java.io.IOException exception) {
                server.execute(() -> send(player, new UploadResult(UploadError.STORAGE_FAILURE, null)));
                return;
            }
            UploadError downloaded = fetcher.download(payload.url(), temporary, maxBytes);
            if (downloaded != UploadError.NONE) {
                try { Files.deleteIfExists(temporary); } catch (java.io.IOException ignored) { }
                server.execute(() -> send(player, new UploadResult(downloaded, null)));
                return;
            }
            uploads.ingestDownloaded(playerId, permissionLevel, payload.title(), uploaderName, temporary,
                            () -> writer.inputFingerprint() == fingerprint
                                    && player.containerMenu instanceof DiscWriterMenu open
                                    && open.writer() == writer)
                    .thenAccept(result -> {
                        UploadError error = result.error();
                        if (error == UploadError.NONE && !writer.writeDisc(fingerprint, result.track())) {
                            error = UploadError.INVALID_WRITER;
                        }
                        UploadError finalError = error;
                        send(player, new UploadResult(finalError, finalError == UploadError.NONE ? result.track() : null));
                    });
        });
    }

    @Override
    public void handle(UploadChunk payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = requirePlayer(context);
            uploadIoExecutor.execute(() -> {
                UploadError error = uploads.append(player.getUUID(), payload.sessionId(), payload.offset(), payload.bytes());
                if (error != UploadError.NONE) server.execute(() -> {
                    uploads.cancel(player.getUUID(), payload.sessionId());
                    writers.remove(payload.sessionId());
                    send(player, new UploadResult(error, null));
                });
            });
        });
    }

    @Override
    public void handle(UploadFinish payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = requirePlayer(context);
            WriterReservation reservation = writers.get(payload.sessionId());
            uploadIoExecutor.execute(() -> uploads.finish(player.getUUID(), payload.sessionId(), payload.clientHash(),
                    () -> reservation != null
                            && reservation.playerId.equals(player.getUUID())
                            && reservation.player.containerMenu == reservation.menu
                            && reservation.level.getBlockEntity(reservation.pos) == reservation.writer
                            && reservation.player.distanceToSqr(
                                    reservation.pos.getX() + 0.5,
                                    reservation.pos.getY() + 0.5,
                                    reservation.pos.getZ() + 0.5) <= 64.0
                            && reservation.writer.inputFingerprint() == reservation.fingerprint)
                    .thenAccept(result -> {
                writers.remove(payload.sessionId());
                UploadError error = result.error();
                if (error == UploadError.NONE && (reservation == null
                        || !reservation.playerId.equals(player.getUUID())
                        || !reservation.writer.writeDisc(reservation.fingerprint, result.track()))) {
                    error = UploadError.INVALID_WRITER;
                }
                send(player, new UploadResult(error, error == UploadError.NONE ? result.track() : null));
            }));
        });
    }

    @Override
    public void handle(PlaybackPreference payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = requirePlayer(context);
            if (payload.enabled()) {
                playbackDisabled.remove(player.getUUID());
                return;
            }
            playbackDisabled.add(player.getUUID());
            downloads.forget(player.getUUID());
            // Drop them from anything already playing so no further chunks are sent.
            activePlaybacks.forEach((key, playback) -> {
                if (playback.listeners.remove(player.getUUID())) {
                    send(player, new JukeboxStop(key.anchor));
                }
            });
        });
    }

    @Override
    public void handle(TrackRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = requirePlayer(context);
            var metadata = catalog.find(payload.sha256());
            if (metadata.isEmpty()) {
                send(player, new TrackUnavailable(payload.sha256()));
                return;
            }
            TrackReference reference = metadata.get().reference();
            if (playbackDisabled.contains(player.getUUID())) {
                send(player, new TrackUnavailable(payload.sha256()));
                return;
            }
            boolean audible = activePlaybacks.values().stream().anyMatch(playback ->
                    playback.track.sha256().equals(reference.sha256())
                            && playback.listeners.contains(player.getUUID()));
            if (!audible) {
                send(player, new TrackUnavailable(payload.sha256()));
                return;
            }
            switch (downloads.submit(player.getUUID(), reference.sha256())) {
                case START -> startSend(player, reference);
                case QUEUED, DUPLICATE -> { }
                case REJECTED -> send(player, new TrackUnavailable(payload.sha256()));
            }
        });
    }

    private void startSend(ServerPlayer player, TrackReference reference) {
        ioExecutor.execute(() -> {
            try { sendTrack(player, reference); }
            finally { server.execute(() -> advance(player, reference.sha256())); }
        });
    }

    private void advance(ServerPlayer player, String finishedHash) {
        String next = downloads.complete(player.getUUID(), finishedHash).orElse(null);
        while (next != null) {
            var metadata = catalog.find(next);
            if (metadata.isPresent()) {
                startSend(player, metadata.get().reference());
                return;
            }
            send(player, new TrackUnavailable(next));
            next = downloads.complete(player.getUUID(), next).orElse(null);
        }
    }

    private void sendTrack(ServerPlayer player, TrackReference reference) {
        String hash = reference.sha256();
        var path = trackStorage.find(hash, reference.format());
        if (path.isEmpty()) {
            send(player, new TrackUnavailable(hash));
            return;
        }
        try (InputStream input = Files.newInputStream(path.get())) {
            long size = Files.size(path.get());
            send(player, new TrackBegin(hash, size, reference.format()));
            byte[] buffer = new byte[DownloadChunk.MAX_BYTES];
            long offset = 0;
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) continue;
                byte[] bytes = java.util.Arrays.copyOf(buffer, count);
                offset += count;
                send(player, new DownloadChunk(hash, offset - count, bytes, offset == size));
                LockSupport.parkNanos(Math.max(1L,
                        count * 1_000_000_000L / ServerConfig.INSTANCE.snapshot().uploadBytesPerSecond()));
            }
        } catch (java.io.IOException exception) {
            send(player, new TrackUnavailable(hash));
        }
    }

    public static void play(ServerLevel level, BlockPos pos, TrackReference track, long elapsedMillis) {
        play(level, PlaybackAnchor.atBlock(pos), track, elapsedMillis);
    }

    public static void play(ServerLevel level, PlaybackAnchor anchor, TrackReference track, long elapsedMillis) {
        if (instance == null) return;
        ActiveKey key = new ActiveKey(level.dimension(), anchor);
        ActivePlayback playback = new ActivePlayback(level, track,
                instance.server.getTickCount() - elapsedMillis / 50L, new HashSet<>());
        instance.activePlaybacks.put(key, playback);
        instance.reconcile(key, playback);
    }

    public static void stop(ServerLevel level, BlockPos pos) {
        stop(level, PlaybackAnchor.atBlock(pos));
    }

    public static void stop(ServerLevel level, PlaybackAnchor anchor) {
        if (instance == null) return;
        ActivePlayback playback = instance.activePlaybacks.remove(new ActiveKey(level.dimension(), anchor));
        if (playback != null) {
            playback.listeners.stream().map(instance.server.getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull).forEach(player -> send(player, new JukeboxStop(anchor)));
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        if (instance == null || event.getServer().getTickCount() % 20 != 0) return;
        // Copied because an anchor whose entity vanished removes itself while reconciling.
        new HashMap<>(instance.activePlaybacks).forEach(instance::reconcile);
        instance.uploadIoExecutor.execute(instance.uploads::expireTimedOut);
    }

    private void reconcile(ActiveKey key, ActivePlayback playback) {
        net.minecraft.world.phys.Vec3 source = sourceOf(playback.level, key.anchor);
        if (source == null) {
            // The carrier is gone; nothing can be heard from it any more.
            activePlaybacks.remove(key);
            playback.listeners.stream().map(server.getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull).forEach(player -> send(player, new JukeboxStop(key.anchor)));
            playback.listeners.clear();
            return;
        }
        Set<UUID> nearby = new HashSet<>();
        for (ServerPlayer player : playback.level.players()) {
            if (playbackDisabled.contains(player.getUUID())) {
                continue;
            }
            if (player.distanceToSqr(source.x, source.y, source.z) <= 64.0 * 64.0) {
                nearby.add(player.getUUID());
                if (playback.listeners.add(player.getUUID())) {
                    long elapsedMillis = Math.max(0, server.getTickCount() - playback.startedTick) * 50L;
                    send(player, new JukeboxPlay(key.anchor, playback.track, elapsedMillis));
                }
            }
        }
        playback.listeners.stream().filter(id -> !nearby.contains(id)).toList().forEach(id -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) send(player, new JukeboxStop(key.anchor));
            playback.listeners.remove(id);
        });
    }

    /** {@return where the anchor currently sounds from, or {@code null} once its entity is gone} */
    private static net.minecraft.world.phys.Vec3 sourceOf(ServerLevel level, PlaybackAnchor anchor) {
        if (!anchor.isEntity()) {
            return net.minecraft.world.phys.Vec3.atCenterOf(anchor.pos());
        }
        net.minecraft.world.entity.Entity entity = level.getEntity(anchor.entityId());
        return entity == null || entity.isRemoved() ? null : entity.position();
    }

    public static AccessService access() {
        return running().accessService;
    }

    public static TrackCatalogData catalog() {
        return running().catalog;
    }

    public static TrackMaintenance maintenance() {
        return running().maintenance;
    }

    public static void reloadLimits() {
        running().uploads.reload(ServerConfig.INSTANCE.snapshot());
    }

    private static ServerRuntime running() {
        if (instance == null) throw new IllegalStateException("server is not running");
        return instance;
    }

    private static ServerPlayer requirePlayer(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) return player;
        throw new IllegalStateException("serverbound payload without server player");
    }

    private static UploadBeginResponse rejected(UploadError error) {
        return new UploadBeginResponse(UploadBeginResponse.Status.REJECTED, error, null, 0, null);
    }

    private static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    private record WriterReservation(
            UUID playerId,
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos,
            DiscWriterBlockEntity writer,
            DiscWriterMenu menu,
            long fingerprint) {
    }

    private record ActiveKey(ResourceKey<Level> dimension, PlaybackAnchor anchor) { }
    private record ActivePlayback(ServerLevel level, TrackReference track, long startedTick, Set<UUID> listeners) { }
}
