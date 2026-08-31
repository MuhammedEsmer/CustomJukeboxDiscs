package dev.hoodoo.customjukeboxdiscs.server;

import dev.hoodoo.customjukeboxdiscs.config.ForgeServerConfig;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.content.writer.ContainerDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.writer.TileEntityDiscWriter;
import dev.hoodoo.customjukeboxdiscs.network.ModNetwork;
import dev.hoodoo.customjukeboxdiscs.network.PlaybackAnchor;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketDownloadChunk;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketJukeboxPlay;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketJukeboxStop;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryPageResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryWriteResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackBegin;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketTrackUnavailable;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadBegin;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadBeginResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadChunk;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadFinish;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadResult;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUrlUploadRequest;
import dev.hoodoo.customjukeboxdiscs.permission.AccessPolicySavedData;
import dev.hoodoo.customjukeboxdiscs.permission.AccessService;
import dev.hoodoo.customjukeboxdiscs.permission.DefaultAccessService;
import dev.hoodoo.customjukeboxdiscs.storage.BoundedAudioInspector;
import dev.hoodoo.customjukeboxdiscs.storage.FileTrackStorage;
import dev.hoodoo.customjukeboxdiscs.storage.TrackCatalogSavedData;
import dev.hoodoo.customjukeboxdiscs.storage.TrackMaintenance;
import dev.hoodoo.customjukeboxdiscs.storage.TrackMetadata;
import dev.hoodoo.customjukeboxdiscs.transfer.BeginUpload;
import dev.hoodoo.customjukeboxdiscs.transfer.BeginUploadResult;
import dev.hoodoo.customjukeboxdiscs.transfer.TrackUrlFetcher;
import dev.hoodoo.customjukeboxdiscs.transfer.TrackUrlPolicy;
import dev.hoodoo.customjukeboxdiscs.transfer.UploadError;
import dev.hoodoo.customjukeboxdiscs.transfer.UploadManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ServerRuntime {
    private static final Logger LOGGER = LogManager.getLogger("CustomJukeboxDiscs");
    private static final int MAX_QUEUED_DOWNLOADS = 4;
    private static ServerRuntime instance;

    private final MinecraftServer server;
    private final UploadManager uploads;
    private final ExecutorService ioExecutor;
    private final ExecutorService uploadIoExecutor;
    private final FileTrackStorage trackStorage;
    private final TrackCatalogSavedData catalog;
    private final DefaultAccessService accessService;
    private final TrackMaintenance maintenance;
    private final Map<UUID, WriterReservation> writers = new HashMap<>();
    private final Map<ActiveKey, ActivePlayback> activePlaybacks = new HashMap<>();
    private final DownloadQueue downloads = new DownloadQueue(MAX_QUEUED_DOWNLOADS);
    private final Set<UUID> playbackDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ServerRuntime(MinecraftServer server) {
        this.server = server;
        WorldServer overworld = server.getWorld(0);

        TrackCatalogSavedData cat = (TrackCatalogSavedData) overworld.getMapStorage().getOrLoadData(TrackCatalogSavedData.class, TrackCatalogSavedData.DATA_NAME);
        if (cat == null) {
            cat = new TrackCatalogSavedData();
            overworld.getMapStorage().setData(TrackCatalogSavedData.DATA_NAME, cat);
        }
        this.catalog = cat;

        AccessPolicySavedData access = (AccessPolicySavedData) overworld.getMapStorage().getOrLoadData(AccessPolicySavedData.class, AccessPolicySavedData.DATA_NAME);
        if (access == null) {
            access = new AccessPolicySavedData();
            overworld.getMapStorage().setData(AccessPolicySavedData.DATA_NAME, access);
        }
        this.accessService = new DefaultAccessService(access);

        this.ioExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CustomJukeboxDiscs-ServerIO");
            t.setDaemon(true);
            return t;
        });
        this.uploadIoExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CustomJukeboxDiscs-UploadIO");
            t.setDaemon(true);
            return t;
        });

        this.trackStorage = new FileTrackStorage(overworld.getSaveHandler().getWorldDirectory().toPath());
        this.uploads = new UploadManager(
                accessService,
                trackStorage,
                catalog,
                new BoundedAudioInspector(),
                ForgeServerConfig.getUploadLimits(),
                System::nanoTime,
                ioExecutor,
                server::addScheduledTask
        );
        this.maintenance = new TrackMaintenance(catalog, trackStorage);
        this.ioExecutor.execute(this::recover);
    }

    public static void onServerStarting(MinecraftServer server) {
        instance = new ServerRuntime(server);
    }

    public static void onServerStopped() {
        if (instance != null) {
            instance.ioExecutor.shutdown();
            instance.uploadIoExecutor.shutdown();
            instance = null;
        }
    }

    public static void onPlayerLoggedOut(EntityPlayerMP player) {
        if (instance != null) {
            instance.uploads.cancelAll(player.getUniqueID());
            instance.writers.entrySet().removeIf(entry -> entry.getValue().playerId.equals(player.getUniqueID()));
            for (ActivePlayback playback : instance.activePlaybacks.values()) {
                playback.listeners.remove(player.getUniqueID());
            }
            instance.downloads.forget(player.getUniqueID());
            instance.playbackDisabled.remove(player.getUniqueID());
        }
    }

    private void recover() {
        try {
            TrackMaintenance.RecoveryReport report = maintenance.recover();
            if (report.isClean()) return;
            for (String hash : report.getMissingAudio()) {
                LOGGER.warn("Catalogued track {} has no audio file and will report as unavailable", hash);
            }
            for (String hash : report.getUnreferencedAudio()) {
                LOGGER.warn("Audio file {} is not referenced by the catalog; delete it manually if unwanted", hash);
            }
        } catch (IOException exception) {
            LOGGER.error("Track storage recovery failed", exception);
        }
    }

    public void handleUploadBegin(EntityPlayerMP player, PacketUploadBegin payload) {
        if (!(player.openContainer instanceof ContainerDiscWriter)) {
            LOGGER.warn("Upload begin rejected for player {}: not in ContainerDiscWriter", player.getName());
            send(player, PacketUploadBeginResponse.rejected(UploadError.INVALID_WRITER));
            return;
        }
        ContainerDiscWriter container = (ContainerDiscWriter) player.openContainer;
        TileEntityDiscWriter writer = container.getWriter();
        long fingerprint = writer.inputFingerprint();

        BeginUploadResult result = uploads.begin(
                player.getUniqueID(),
                getPermissionLevel(player),
                new BeginUpload(payload.getClientHash(), payload.getDeclaredBytes(), payload.getFormatHint(),
                        payload.getTitle(), player.getName()));

        LOGGER.info("Upload begin from {} (title='{}', format={}, size={} bytes) -> accepted={}, alreadyPresent={}, error={}",
                player.getName(), payload.getTitle(), payload.getFormatHint(), payload.getDeclaredBytes(),
                result.accepted(), result.alreadyPresent(), result.error());

        if (result.alreadyPresent()) {
            if (writer.writeDisc(fingerprint, result.existingTrack())) {
                LOGGER.info("Disc programmed from existing track for {} (track: {})",
                        player.getName(), result.existingTrack().getTitle());
                player.openContainer.detectAndSendChanges();
                player.sendContainerToPlayer(player.openContainer);
                send(player, PacketUploadBeginResponse.alreadyPresent(result.existingTrack()));
            } else {
                LOGGER.warn("Write disc failed for existing track for {} (error=INVALID_WRITER)", player.getName());
                send(player, PacketUploadBeginResponse.rejected(UploadError.INVALID_WRITER));
            }
        } else if (result.accepted()) {
            writers.put(result.sessionId(), new WriterReservation(
                    player.getUniqueID(), player, writer.getWorld(), writer.getPos(), writer, container, fingerprint));
            send(player, PacketUploadBeginResponse.accepted(result.sessionId(), result.chunkBytes()));
        } else {
            send(player, PacketUploadBeginResponse.rejected(result.error()));
        }
    }

    public void handleUrlUpload(EntityPlayerMP player, PacketUrlUploadRequest payload) {
        if (!ForgeServerConfig.urlUploadsEnabled) {
            send(player, new PacketUploadResult(UploadError.URL_NOT_ALLOWED));
            return;
        }
        if (!(player.openContainer instanceof ContainerDiscWriter)) {
            send(player, new PacketUploadResult(UploadError.INVALID_WRITER));
            return;
        }
        ContainerDiscWriter container = (ContainerDiscWriter) player.openContainer;
        TileEntityDiscWriter writer = container.getWriter();
        long fingerprint = writer.inputFingerprint();

        long maxBytes = ForgeServerConfig.maxSourceBytes;
        TrackUrlFetcher fetcher = new TrackUrlFetcher(
                new TrackUrlPolicy(ForgeServerConfig.urlAllowedHosts, ForgeServerConfig.urlAllowPrivateAddresses),
                Duration.ofSeconds(ForgeServerConfig.uploadTimeoutSeconds));

        UUID playerId = player.getUniqueID();
        int perm = getPermissionLevel(player);
        String uploaderName = player.getName();

        ioExecutor.execute(() -> {
            Path temporary;
            try {
                temporary = trackStorage.createTemporary(UUID.randomUUID());
            } catch (IOException exception) {
                server.addScheduledTask(() -> send(player, new PacketUploadResult(UploadError.STORAGE_FAILURE)));
                return;
            }
            UploadError downloaded = fetcher.download(payload.getUrl(), temporary, maxBytes);
            if (downloaded != UploadError.NONE) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
                server.addScheduledTask(() -> send(player, new PacketUploadResult(downloaded)));
                return;
            }
            uploads.ingestDownloaded(playerId, perm, payload.getTitle(), uploaderName, temporary,
                            () -> writer.inputFingerprint() == fingerprint
                                    && player.openContainer == container)
                    .thenAccept(result -> {
                        server.addScheduledTask(() -> {
                            UploadError error = result.error();
                            if (error == UploadError.NONE) {
                                if (!writer.writeDisc(fingerprint, result.track())) {
                                    error = UploadError.INVALID_WRITER;
                                } else {
                                    player.openContainer.detectAndSendChanges();
                                    player.sendContainerToPlayer(player.openContainer);
                                }
                            }
                            send(player, new PacketUploadResult(error));
                        });
                    });
        });
    }

    public void handleUploadChunk(EntityPlayerMP player, PacketUploadChunk payload) {
        uploadIoExecutor.execute(() -> {
            UploadError error = uploads.append(player.getUniqueID(), payload.getSessionId(), payload.getOffset(), payload.getBytes());
            if (error != UploadError.NONE) {
                LOGGER.warn("Upload chunk failed for player {} (session {} offset {}): {}",
                        player.getName(), payload.getSessionId(), payload.getOffset(), error);
                server.addScheduledTask(() -> {
                    uploads.cancel(player.getUniqueID(), payload.getSessionId());
                    writers.remove(payload.getSessionId());
                    send(player, new PacketUploadResult(error, null));
                });
            }
        });
    }

    public void handleUploadFinish(EntityPlayerMP player, PacketUploadFinish payload) {
        WriterReservation reservation = writers.get(payload.getSessionId());
        LOGGER.info("Upload finish received from {} for session {}, reservation present: {}",
                player.getName(), payload.getSessionId(), reservation != null);
        uploadIoExecutor.execute(() -> uploads.finish(player.getUniqueID(), payload.getSessionId(), payload.getClientHash(),
                () -> reservation != null
                        && reservation.playerId.equals(player.getUniqueID())
                        && reservation.player.openContainer == reservation.menu
                        && reservation.world.getTileEntity(reservation.pos) == reservation.writer
                        && reservation.player.getDistanceSq(reservation.pos.getX() + 0.5D, reservation.pos.getY() + 0.5D, reservation.pos.getZ() + 0.5D) <= 64.0D
                        && reservation.writer.inputFingerprint() == reservation.fingerprint)
                .thenAccept(result -> {
                    Runnable action = () -> {
                        writers.remove(payload.getSessionId());
                        UploadError error = result.error();
                        LOGGER.info("Upload finish completed for session {}, result error: {}", payload.getSessionId(), error);
                        if (error == UploadError.NONE) {
                            if (reservation == null
                                    || !reservation.playerId.equals(player.getUniqueID())
                                    || !reservation.writer.writeDisc(reservation.fingerprint, result.track())) {
                                LOGGER.warn("Write disc failed for player {} (reservation={}, error=INVALID_WRITER)",
                                        player.getName(), reservation != null);
                                error = UploadError.INVALID_WRITER;
                            } else {
                                LOGGER.info("Disc successfully written for {} (track: {})",
                                        player.getName(), result.track().getTitle());
                                player.openContainer.detectAndSendChanges();
                                player.sendContainerToPlayer(player.openContainer);
                            }
                        }
                        send(player, new PacketUploadResult(error, error == UploadError.NONE ? result.track() : null));
                    };
                    if (server.isCallingFromMinecraftThread()) {
                        action.run();
                    } else {
                        server.addScheduledTask(action);
                    }
                }));
    }

    public void handlePlaybackPreference(EntityPlayerMP player, boolean enabled) {
        if (enabled) {
            playbackDisabled.remove(player.getUniqueID());
            return;
        }
        playbackDisabled.add(player.getUniqueID());
        downloads.forget(player.getUniqueID());
        for (Map.Entry<ActiveKey, ActivePlayback> entry : activePlaybacks.entrySet()) {
            if (entry.getValue().listeners.remove(player.getUniqueID())) {
                send(player, new PacketJukeboxStop(entry.getKey().anchor));
            }
        }
    }

    public void handleTrackRequest(EntityPlayerMP player, String sha256) {
        Optional<TrackMetadata> metadata = catalog.find(sha256);
        if (!metadata.isPresent() || playbackDisabled.contains(player.getUniqueID())) {
            send(player, new PacketTrackUnavailable(sha256));
            return;
        }
        TrackReference reference = metadata.get().reference();
        boolean audible = activePlaybacks.values().stream().anyMatch(playback ->
                playback.track.getSha256().equals(reference.getSha256())
                        && playback.listeners.contains(player.getUniqueID()));
        if (!audible) {
            send(player, new PacketTrackUnavailable(sha256));
            return;
        }
        switch (downloads.submit(player.getUniqueID(), reference.getSha256())) {
            case START:
                startSend(player, reference);
                break;
            case QUEUED:
            case DUPLICATE:
                break;
            case REJECTED:
                send(player, new PacketTrackUnavailable(sha256));
                break;
        }
    }

    public void handleLibraryPage(EntityPlayerMP player, int requestedPage) {
        if (!(player.openContainer instanceof ContainerDiscWriter)) return;
        TrackCatalogSavedData.CatalogPage page = catalog.pageClamped(requestedPage, PacketLibraryPageResponse.MAX_TRACKS);
        List<TrackReference> tracks = page.entries().stream()
                .map(TrackMetadata::reference).collect(java.util.stream.Collectors.toList());
        send(player, new PacketLibraryPageResponse(page.page(), page.pageCount(), page.totalTracks(), tracks));
    }

    public void handleLibraryWrite(EntityPlayerMP player, String sha256, long inputFingerprint) {
        if (!(player.openContainer instanceof ContainerDiscWriter)) {
            send(player, new PacketLibraryWriteResponse(PacketLibraryWriteResponse.Result.INVALID_WRITER));
            return;
        }
        ContainerDiscWriter menu = (ContainerDiscWriter) player.openContainer;
        TileEntityDiscWriter writer = menu.getWriter();
        Optional<TrackMetadata> metadata = catalog.find(sha256);
        if (!metadata.isPresent() || !trackStorage.find(
                metadata.get().reference().sha256(), metadata.get().reference().format()).isPresent()) {
            send(player, new PacketLibraryWriteResponse(PacketLibraryWriteResponse.Result.TRACK_UNAVAILABLE));
            return;
        }
        if (!writer.isUsableByPlayer(player) || !writer.writeDisc(inputFingerprint, metadata.get().reference())) {
            send(player, new PacketLibraryWriteResponse(PacketLibraryWriteResponse.Result.INVALID_WRITER));
            return;
        }
        menu.detectAndSendChanges();
        player.sendContainerToPlayer(menu);
        send(player, new PacketLibraryWriteResponse(PacketLibraryWriteResponse.Result.WRITTEN));
    }

    private void startSend(EntityPlayerMP player, TrackReference reference) {
        ioExecutor.execute(() -> {
            try {
                sendTrack(player, reference);
            } finally {
                server.addScheduledTask(() -> advance(player, reference.getSha256()));
            }
        });
    }

    private void advance(EntityPlayerMP player, String finishedHash) {
        String next = downloads.complete(player.getUniqueID(), finishedHash).orElse(null);
        while (next != null) {
            Optional<TrackMetadata> metadata = catalog.find(next);
            if (metadata.isPresent()) {
                startSend(player, metadata.get().reference());
                return;
            }
            send(player, new PacketTrackUnavailable(next));
            next = downloads.complete(player.getUniqueID(), next).orElse(null);
        }
    }

    private void sendTrack(EntityPlayerMP player, TrackReference reference) {
        String hash = reference.getSha256();
        Optional<Path> path = trackStorage.find(hash, reference.getFormat());
        if (!path.isPresent()) {
            send(player, new PacketTrackUnavailable(hash));
            return;
        }
        try (InputStream input = Files.newInputStream(path.get())) {
            long size = Files.size(path.get());
            send(player, new PacketTrackBegin(hash, size, 0L, reference.getFormat()));
            byte[] buffer = new byte[PacketDownloadChunk.MAX_BYTES];
            int chunkIndex = 0;
            long offset = 0;
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) continue;
                byte[] bytes = Arrays.copyOf(buffer, count);
                offset += count;
                send(player, new PacketDownloadChunk(hash, chunkIndex++, bytes, offset == size));
                LockSupport.parkNanos(Math.max(1L, count * 1_000_000_000L / ForgeServerConfig.uploadBytesPerSecond));
            }
        } catch (IOException exception) {
            send(player, new PacketTrackUnavailable(hash));
        }
    }

    public static void play(World world, BlockPos pos, TrackReference track, long elapsedMillis) {
        play(world, PlaybackAnchor.atBlock(pos), track, elapsedMillis);
    }

    public static void play(World world, PlaybackAnchor anchor, TrackReference track, long elapsedMillis) {
        if (instance == null) return;
        ActiveKey key = new ActiveKey(world.provider.getDimension(), anchor);
        ActivePlayback playback = new ActivePlayback(world, track,
                instance.server.getTickCounter() - elapsedMillis / 50L, new HashSet<>());
        instance.activePlaybacks.put(key, playback);
        instance.reconcile(key, playback);
    }

    public static void stop(World world, BlockPos pos) {
        stop(world, PlaybackAnchor.atBlock(pos));
    }

    public static void stop(World world, PlaybackAnchor anchor) {
        if (instance == null) return;
        ActivePlayback playback = instance.activePlaybacks.remove(new ActiveKey(world.provider.getDimension(), anchor));
        if (playback != null) {
            for (UUID id : playback.listeners) {
                EntityPlayerMP player = instance.server.getPlayerList().getPlayerByUUID(id);
                if (player != null) {
                    send(player, new PacketJukeboxStop(anchor));
                }
            }
        }
    }

    public static void serverTick() {
        if (instance == null || instance.server.getTickCounter() % 20 != 0) return;
        for (Map.Entry<ActiveKey, ActivePlayback> entry : new HashMap<>(instance.activePlaybacks).entrySet()) {
            instance.reconcile(entry.getKey(), entry.getValue());
        }
        instance.uploadIoExecutor.execute(instance.uploads::expireTimedOut);
    }

    private void reconcile(ActiveKey key, ActivePlayback playback) {
        Vec3d source = sourceOf(playback.world, key.anchor);
        if (source == null) {
            activePlaybacks.remove(key);
            for (UUID id : playback.listeners) {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null) {
                    send(player, new PacketJukeboxStop(key.anchor));
                }
            }
            playback.listeners.clear();
            return;
        }

        Set<UUID> nearby = new HashSet<>();
        for (EntityPlayerMP player : playback.world.getPlayers(EntityPlayerMP.class, p -> true)) {
            if (playbackDisabled.contains(player.getUniqueID())) continue;
            if (player.getDistanceSq(source.x, source.y, source.z) <= 64.0D * 64.0D) {
                nearby.add(player.getUniqueID());
                if (playback.listeners.add(player.getUniqueID())) {
                    long elapsedMillis = Math.max(0, server.getTickCounter() - playback.startedTick) * 50L;
                    send(player, new PacketJukeboxPlay(key.anchor, playback.track, System.nanoTime() - elapsedMillis * 1_000_000L));
                }
            }
        }

        for (UUID id : new HashSet<>(playback.listeners)) {
            if (!nearby.contains(id)) {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null) {
                    send(player, new PacketJukeboxStop(key.anchor));
                }
                playback.listeners.remove(id);
            }
        }
    }

    private static Vec3d sourceOf(World world, PlaybackAnchor anchor) {
        if (!anchor.isEntity()) {
            BlockPos pos = anchor.getPos();
            return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }
        Entity entity = world.getEntityByID(anchor.getEntityId());
        return (entity == null || entity.isDead) ? null : entity.getPositionVector();
    }

    public static AccessService access() {
        return running().accessService;
    }

    public static TrackCatalogSavedData catalog() {
        return running().catalog;
    }

    public static TrackMaintenance maintenance() {
        return running().maintenance;
    }

    public static ServerRuntime getInstance() {
        return instance;
    }

    private static ServerRuntime running() {
        if (instance == null) throw new IllegalStateException("server is not running");
        return instance;
    }

    private static void send(EntityPlayerMP player, net.minecraftforge.fml.common.network.simpleimpl.IMessage message) {
        ModNetwork.CHANNEL.sendTo(message, player);
    }

    private static final class WriterReservation {
        final UUID playerId;
        final EntityPlayerMP player;
        final World world;
        final BlockPos pos;
        final TileEntityDiscWriter writer;
        final ContainerDiscWriter menu;
        final long fingerprint;

        WriterReservation(
                UUID playerId,
                EntityPlayerMP player,
                World world,
                BlockPos pos,
                TileEntityDiscWriter writer,
                ContainerDiscWriter menu,
                long fingerprint) {
            this.playerId = playerId;
            this.player = player;
            this.world = world;
            this.pos = pos;
            this.writer = writer;
            this.menu = menu;
            this.fingerprint = fingerprint;
        }
    }

    private static final class ActiveKey {
        final int dimension;
        final PlaybackAnchor anchor;

        ActiveKey(int dimension, PlaybackAnchor anchor) {
            this.dimension = dimension;
            this.anchor = anchor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ActiveKey)) return false;
            ActiveKey activeKey = (ActiveKey) o;
            return dimension == activeKey.dimension && Objects.equals(anchor, activeKey.anchor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, anchor);
        }
    }

    private int getPermissionLevel(EntityPlayerMP player) {
        if (server.isSinglePlayer()) {
            return 3;
        }
        if (player.canUseCommand(2, "") || server.getPlayerList().canSendCommands(player.getGameProfile())) {
            return 3;
        }
        return 0;
    }

    private static final class ActivePlayback {
        final World world;
        final TrackReference track;
        final long startedTick;
        final Set<UUID> listeners;

        ActivePlayback(World world, TrackReference track, long startedTick, Set<UUID> listeners) {
            this.world = world;
            this.track = track;
            this.startedTick = startedTick;
            this.listeners = listeners;
        }
    }
}
