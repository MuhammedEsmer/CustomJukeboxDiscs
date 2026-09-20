package dev.hoodoo.customjukeboxdiscs.client.transfer;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import dev.hoodoo.customjukeboxdiscs.network.ModNetwork;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadBegin;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadBeginResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadChunk;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadFinish;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUploadResult;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketUrlUploadRequest;
import dev.hoodoo.customjukeboxdiscs.transfer.UploadError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public final class ClientUploadManager {
    private static final ClientUploadManager INSTANCE = new ClientUploadManager();
    private static final long SAFE_BYTES_PER_SECOND = 256L * 1024L;
    private static final int MAX_QUEUE_WAITS = 40;
    private static final int RETRY_DELAY_SECONDS = 3;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CustomJukeboxDiscs-UploadScheduler");
        t.setDaemon(true);
        return t;
    });

    private volatile Pending pending;
    private volatile int queueWaits;

    private ClientUploadManager() {
    }

    public static ClientUploadManager getInstance() {
        return INSTANCE;
    }

    public void beginFromUrl(String url, String title, long inputFingerprint, Consumer<ITextComponent> status) {
        if (pending != null) {
            status.accept(new TextComponentTranslation("upload.customjukeboxdiscs.busy"));
            return;
        }
        pending = new Pending(null, "", 0, AudioFormat.MP3, title, inputFingerprint, status, progress -> { });
        status.accept(new TextComponentTranslation("upload.customjukeboxdiscs.fetching"));
        ModNetwork.CHANNEL.sendToServer(new PacketUrlUploadRequest(url, title, inputFingerprint));
    }

    public void begin(
            Path file, String title, long inputFingerprint, Consumer<ITextComponent> status, Consumer<Double> progress) {
        if (pending != null) {
            status.accept(new TextComponentTranslation("upload.customjukeboxdiscs.busy"));
            return;
        }
        status.accept(new TextComponentTranslation("upload.customjukeboxdiscs.hashing"));
        queueWaits = 0;
        CompletableFuture.runAsync(() -> {
            try {
                long size = Files.size(file);
                String hash = sha256(file);
                Pending upload = new Pending(file, hash, size, format(file), title, inputFingerprint, status, progress);
                pending = upload;
                ModNetwork.CHANNEL.sendToServer(new PacketUploadBegin(
                        hash, title, size, 0L, upload.format, inputFingerprint));
            } catch (IOException | RuntimeException exception) {
                Minecraft.getMinecraft().addScheduledTask(() -> status.accept(
                        new TextComponentTranslation("upload.customjukeboxdiscs.read_failed")));
            }
        });
    }

    public void handleBeginResponse(PacketUploadBeginResponse response) {
        Pending current = pending;
        if (current == null) return;
        if (response.getError() != UploadError.NONE) {
            if (response.getError() == UploadError.ANOTHER_UPLOAD_ACTIVE
                    && current.file != null && queueWaits < MAX_QUEUE_WAITS) {
                queueWaits++;
                Minecraft.getMinecraft().addScheduledTask(() -> current.status.accept(
                        new TextComponentTranslation("upload.customjukeboxdiscs.queued")));
                scheduleQueueRetry(current);
                return;
            }
            finishStatus(current, new TextComponentTranslation(
                    "upload.customjukeboxdiscs.failed", new TextComponentTranslation(response.getError().translationKey())));
        } else if (response.getExistingTrack() != null) {
            ClientPlaybackManager.getInstance().cacheLocal(current.file, response.getExistingTrack());
            finishStatus(current, new TextComponentTranslation("upload.customjukeboxdiscs.complete"));
        } else {
            CompletableFuture.runAsync(() -> upload(current, response));
        }
    }

    private void upload(Pending current, PacketUploadBeginResponse response) {
        try (InputStream input = Files.newInputStream(current.file)) {
            byte[] buffer = new byte[response.getChunkBytes() > 0 ? response.getChunkBytes() : 31 * 1024];
            long offset = 0;
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (pending != current) return;
                if (count == 0) continue;
                byte[] chunkData = Arrays.copyOf(buffer, count);
                ModNetwork.CHANNEL.sendToServer(new PacketUploadChunk(
                        response.getSessionId(), offset, chunkData));
                offset += count;
                long totalSent = offset;
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (pending == current) {
                        current.status.accept(new TextComponentTranslation(
                                "upload.customjukeboxdiscs.progress", totalSent * 100L / Math.max(1L, current.size)));
                        current.progress.accept((double) totalSent / current.size);
                    }
                });
                LockSupport.parkNanos(Math.max(1L, count * 1_000_000_000L / SAFE_BYTES_PER_SECOND));
            }
            if (pending == current) {
                ModNetwork.CHANNEL.sendToServer(new PacketUploadFinish(response.getSessionId(), current.hash));
            }
        } catch (IOException exception) {
            finishStatus(current, new TextComponentTranslation("upload.customjukeboxdiscs.read_failed"));
        }
    }

    public void handleUploadResult(PacketUploadResult result) {
        Pending current = pending;
        if (current != null) {
            if (result.getError() == UploadError.NONE && current.file != null) {
                ClientPlaybackManager.getInstance().cacheLocal(current.file, result.getTrack());
            }
            finishStatus(current, result.getError() == UploadError.NONE
                    ? new TextComponentTranslation("upload.customjukeboxdiscs.complete")
                    : new TextComponentTranslation("upload.customjukeboxdiscs.failed",
                            new TextComponentTranslation(result.getError().translationKey())));
        }
    }

    private void scheduleQueueRetry(Pending current) {
        SCHEDULER.schedule(() -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (pending == current) {
                ModNetwork.CHANNEL.sendToServer(new PacketUploadBegin(
                        current.hash, current.title, current.size, 0L, current.format, current.inputFingerprint));
            }
        }), RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void finishStatus(Pending current, ITextComponent message) {
        pending = null;
        Minecraft.getMinecraft().addScheduledTask(() -> {
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
                while ((count = input.read(bytes)) >= 0) {
                    if (count > 0) digest.update(bytes, 0, count);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static AudioFormat format(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg")
                ? AudioFormat.OGG : AudioFormat.MP3;
    }

    private static final class Pending {
        final Path file;
        final String hash;
        final long size;
        final AudioFormat format;
        final String title;
        final long inputFingerprint;
        final Consumer<ITextComponent> status;
        final Consumer<Double> progress;

        Pending(
                Path file, String hash, long size, AudioFormat format, String title, long inputFingerprint,
                Consumer<ITextComponent> status, Consumer<Double> progress) {
            this.file = file;
            this.hash = hash;
            this.size = size;
            this.format = format;
            this.title = title;
            this.inputFingerprint = inputFingerprint;
            this.status = status;
            this.progress = progress;
        }
    }
}
