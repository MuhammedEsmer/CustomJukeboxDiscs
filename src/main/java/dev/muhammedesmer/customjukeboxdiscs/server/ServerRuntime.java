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
import dev.muhammedesmer.customjukeboxdiscs.network.payload.DownloadChunk;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxPlay;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.JukeboxStop;
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
import dev.muhammedesmer.customjukeboxdiscs.transfer.BeginUpload;
import dev.muhammedesmer.customjukeboxdiscs.transfer.BeginUploadResult;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadManager;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String CATALOG_KEY = "customjukeboxdiscs_tracks";
    private static final String ACCESS_KEY = "customjukeboxdiscs_access";
    private static ServerRuntime instance;

    private final UploadManager uploads;
    private final ExecutorService ioExecutor;
    private final ExecutorService uploadIoExecutor;
    private final FileTrackStorage trackStorage;
    private final TrackCatalogData catalog;
    private final DefaultAccessService accessService;
    private final Map<UUID, WriterReservation> writers = new HashMap<>();
    private final Map<ActiveKey, ActivePlayback> activePlaybacks = new HashMap<>();
    private final MinecraftServer server;
    private final Map<UUID, Set<String>> activeDownloads = new ConcurrentHashMap<>();

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
        ioExecutor.execute(() -> {
            try { trackStorage.cleanupTemporaryFiles(); }
            catch (java.io.IOException ignored) { }
        });
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
            instance.activeDownloads.remove(player.getUUID());
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
    public void handle(TrackRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = requirePlayer(context);
            var metadata = catalog.find(payload.sha256());
            if (metadata.isEmpty()) {
                send(player, new TrackUnavailable(payload.sha256()));
                return;
            }
            TrackReference reference = metadata.get().reference();
            boolean audible = activePlaybacks.values().stream().anyMatch(playback ->
                    playback.track.sha256().equals(reference.sha256())
                            && playback.listeners.contains(player.getUUID()));
            Set<String> playerDownloads = activeDownloads.computeIfAbsent(
                    player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            if (!audible || !playerDownloads.isEmpty() || !playerDownloads.add(reference.sha256())) {
                send(player, new TrackUnavailable(payload.sha256()));
                return;
            }
            ioExecutor.execute(() -> {
                try { sendTrack(player, reference); }
                finally { playerDownloads.remove(reference.sha256()); }
            });
        });
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
        if (instance == null) return;
        ActiveKey key = new ActiveKey(level.dimension(), pos.immutable());
        ActivePlayback playback = new ActivePlayback(level, track,
                instance.server.getTickCount() - elapsedMillis / 50L, new HashSet<>());
        instance.activePlaybacks.put(key, playback);
        instance.reconcile(key, playback);
    }

    public static void stop(ServerLevel level, BlockPos pos) {
        if (instance == null) return;
        ActivePlayback playback = instance.activePlaybacks.remove(new ActiveKey(level.dimension(), pos));
        if (playback != null) {
            playback.listeners.stream().map(instance.server.getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull).forEach(player -> send(player, new JukeboxStop(pos)));
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        if (instance == null || event.getServer().getTickCount() % 20 != 0) return;
        instance.activePlaybacks.forEach(instance::reconcile);
        instance.uploadIoExecutor.execute(instance.uploads::expireTimedOut);
    }

    private void reconcile(ActiveKey key, ActivePlayback playback) {
        Set<UUID> nearby = new HashSet<>();
        for (ServerPlayer player : playback.level.players()) {
            if (player.distanceToSqr(key.pos.getX() + 0.5, key.pos.getY() + 0.5, key.pos.getZ() + 0.5) <= 64.0 * 64.0) {
                nearby.add(player.getUUID());
                if (playback.listeners.add(player.getUUID())) {
                    long elapsedMillis = Math.max(0, server.getTickCount() - playback.startedTick) * 50L;
                    send(player, new JukeboxPlay(key.pos, playback.track, elapsedMillis));
                }
            }
        }
        playback.listeners.stream().filter(id -> !nearby.contains(id)).toList().forEach(id -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) send(player, new JukeboxStop(key.pos));
            playback.listeners.remove(id);
        });
    }

    public static AccessService access() {
        if (instance == null) throw new IllegalStateException("server is not running");
        return instance.accessService;
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

    private record ActiveKey(ResourceKey<Level> dimension, BlockPos pos) { }
    private record ActivePlayback(ServerLevel level, TrackReference track, long startedTick, Set<UUID> listeners) { }
}
