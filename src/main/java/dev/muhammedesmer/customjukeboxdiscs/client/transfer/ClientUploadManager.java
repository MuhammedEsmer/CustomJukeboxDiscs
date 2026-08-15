package dev.muhammedesmer.customjukeboxdiscs.client.transfer;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
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
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackBegin;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.TrackUnavailable;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientUploadManager implements ModPayloads.ClientHandler {
    public static final ClientUploadManager INSTANCE = new ClientUploadManager();
    private static final long SAFE_BYTES_PER_SECOND = 256L * 1024L;

    private volatile Pending pending;

    private ClientUploadManager() {
    }

    /** Starts a link based write; the server does the downloading. */
    public void beginFromUrl(String url, String title, long inputFingerprint, Consumer<Component> status) {
        if (pending != null) {
            status.accept(Component.translatable("upload.customjukeboxdiscs.busy"));
            return;
        }
        pending = new Pending(null, "", 0, status, progress -> { });
        status.accept(Component.translatable("upload.customjukeboxdiscs.fetching"));
        PacketDistributor.sendToServer(new UrlUploadRequest(url, title, inputFingerprint));
    }

    public void begin(
            Path file, String title, long inputFingerprint, Consumer<Component> status, DoubleConsumer progress) {
        if (pending != null) {
            status.accept(Component.translatable("upload.customjukeboxdiscs.busy"));
            return;
        }
        status.accept(Component.translatable("upload.customjukeboxdiscs.hashing"));
        CompletableFuture.runAsync(() -> {
            try {
                long size = Files.size(file);
                String hash = sha256(file);
                Pending upload = new Pending(file, hash, size, status, progress);
                pending = upload;
                PacketDistributor.sendToServer(new UploadBeginRequest(
                        hash, size, format(file), title, inputFingerprint));
            } catch (IOException | RuntimeException exception) {
                Minecraft.getInstance().execute(() -> status.accept(
                        Component.translatable("upload.customjukeboxdiscs.read_failed")));
            }
        });
    }

    @Override
    public void handle(UploadBeginResponse response, IPayloadContext context) {
        Pending current = pending;
        if (current == null) return;
        if (response.status() == UploadBeginResponse.Status.REJECTED) {
            finishStatus(current, Component.translatable(
                    "upload.customjukeboxdiscs.failed", Component.translatable(response.error().translationKey())));
        } else if (response.status() == UploadBeginResponse.Status.ALREADY_PRESENT) {
            finishStatus(current, Component.translatable("upload.customjukeboxdiscs.complete"));
        } else {
            CompletableFuture.runAsync(() -> upload(current, response));
        }
    }

    private void upload(Pending current, UploadBeginResponse response) {
        try (InputStream input = Files.newInputStream(current.file)) {
            byte[] buffer = new byte[response.chunkBytes()];
            long offset = 0;
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) continue;
                PacketDistributor.sendToServer(new UploadChunk(
                        response.sessionId(), offset, Arrays.copyOf(buffer, count)));
                offset += count;
                long sent = offset;
                Minecraft.getInstance().execute(() -> {
                    current.status.accept(Component.translatable(
                            "upload.customjukeboxdiscs.progress", sent * 100L / Math.max(1L, current.size)));
                    current.progress.accept((double) sent / current.size);
                });
                LockSupport.parkNanos(Math.max(1L, count * 1_000_000_000L / SAFE_BYTES_PER_SECOND));
            }
            PacketDistributor.sendToServer(new UploadFinish(response.sessionId(), current.hash));
        } catch (IOException exception) {
            finishStatus(current, Component.translatable("upload.customjukeboxdiscs.read_failed"));
        }
    }

    @Override
    public void handle(UploadResult result, IPayloadContext context) {
        Pending current = pending;
        if (current != null) {
            finishStatus(current, result.error() == UploadError.NONE
                    ? Component.translatable("upload.customjukeboxdiscs.complete")
                    : Component.translatable("upload.customjukeboxdiscs.failed",
                            Component.translatable(result.error().translationKey())));
        }
    }

    @Override public void handle(JukeboxPlay payload, IPayloadContext context) { context.enqueueWork(() -> ClientPlaybackManager.INSTANCE.play(payload)); }
    @Override public void handle(JukeboxStop payload, IPayloadContext context) { context.enqueueWork(() -> ClientPlaybackManager.INSTANCE.stop(payload)); }
    @Override public void handle(TrackBegin payload, IPayloadContext context) { context.enqueueWork(() -> ClientPlaybackManager.INSTANCE.begin(payload)); }
    @Override public void handle(DownloadChunk payload, IPayloadContext context) { context.enqueueWork(() -> ClientPlaybackManager.INSTANCE.chunk(payload)); }
    @Override public void handle(TrackUnavailable payload, IPayloadContext context) { context.enqueueWork(() -> ClientPlaybackManager.INSTANCE.unavailable(payload)); }

    private void finishStatus(Pending current, Component message) {
        pending = null;
        Minecraft.getInstance().execute(() -> {
            current.status.accept(message);
            current.progress.accept(0.0);
        });
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] bytes = new byte[8192];
                int count;
                while ((count = input.read(bytes)) >= 0) if (count > 0) digest.update(bytes, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static AudioFormat format(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg")
                ? AudioFormat.OGG : AudioFormat.MP3;
    }

    private record Pending(
            Path file, String hash, long size, Consumer<Component> status, DoubleConsumer progress) {
    }
}
