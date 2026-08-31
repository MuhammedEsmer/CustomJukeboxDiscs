package dev.muhammedesmer.customjukeboxdiscs.client.cache;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientTrackCache {
    private final Path root;
    private final long maxBytes;
    private final Map<String, Download> downloads = new ConcurrentHashMap<>();

    public ClientTrackCache(Path gameDirectory, String serverIdentity, long maxBytes) {
        root = gameDirectory.toAbsolutePath().normalize().resolve("customjukeboxdiscs/cache").resolve(key(serverIdentity));
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
        this.maxBytes = maxBytes;
    }

    public Optional<Path> find(String hash, AudioFormat format) {
        Path path = target(hash, format);
        if (!Files.isRegularFile(path)) return Optional.empty();
        try {
            if (!fileHash(path).equals(hash)) {
                Files.deleteIfExists(path);
                return Optional.empty();
            }
        } catch (IOException exception) {
            return Optional.empty();
        }
        try { Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis())); }
        catch (IOException ignored) { }
        return Optional.of(path);
    }

    public synchronized void begin(String hash, long size, AudioFormat format, long maxBytes) throws IOException {
        if (size <= 0 || size > Math.min(maxBytes, this.maxBytes) || downloads.containsKey(hash)) {
            throw new IOException("invalid download declaration");
        }
        Files.createDirectories(root);
        Path temporary = root.resolve(hash + ".part");
        Files.deleteIfExists(temporary);
        downloads.put(hash, new Download(size, format, temporary, Files.newOutputStream(temporary), digest()));
    }

    public synchronized Optional<Path> append(String hash, long offset, byte[] bytes, boolean last) throws IOException {
        Download download = downloads.get(hash);
        if (download == null || offset != download.received || download.received + bytes.length > download.size) {
            throw new IOException("invalid download offset");
        }
        download.output.write(bytes);
        download.digest.update(bytes);
        download.received += bytes.length;
        if (!last) return Optional.empty();
        downloads.remove(hash);
        download.output.close();
        if (download.received != download.size || !HexFormat.of().formatHex(download.digest.digest()).equals(hash)) {
            Files.deleteIfExists(download.temporary);
            throw new IOException("download hash mismatch");
        }
        Path target = target(hash, download.format);
        Files.move(download.temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        evict(target);
        return Optional.of(target);
    }

    public synchronized void cancel(String hash) {
        Download download = downloads.remove(hash);
        if (download == null) return;
        try { download.output.close(); } catch (IOException ignored) { }
        try { Files.deleteIfExists(download.temporary); } catch (IOException ignored) { }
    }

    public synchronized void cancelAll() {
        java.util.List.copyOf(downloads.keySet()).forEach(this::cancel);
    }

    private Path target(String hash, AudioFormat format) {
        requireHash(hash);
        return root.resolve(hash + "." + format.serializedName());
    }

    private void evict(Path protectedPath) throws IOException {
        try (var listed = Files.list(root)) {
            var files = listed.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().endsWith(".part"))
                    .sorted(java.util.Comparator.comparingLong(ClientTrackCache::lastModified))
                    .toList();
            long total = 0;
            for (Path file : files) total += Files.size(file);
            for (Path file : files) {
                if (total <= maxBytes) break;
                if (file.equals(protectedPath)) continue;
                long size = Files.size(file);
                Files.deleteIfExists(file);
                total -= size;
            }
        }
    }

    private static long lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return Long.MIN_VALUE; }
    }

    private static String key(String identity) {
        MessageDigest digest = digest();
        return HexFormat.of().formatHex(digest.digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 24);
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String fileHash(Path path) throws IOException {
        MessageDigest digest = digest();
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void requireHash(String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("invalid hash");
    }

    private static final class Download {
        final long size;
        final AudioFormat format;
        final Path temporary;
        final OutputStream output;
        final MessageDigest digest;
        long received;
        Download(long size, AudioFormat format, Path temporary, OutputStream output, MessageDigest digest) {
            this.size = size; this.format = format; this.temporary = temporary; this.output = output; this.digest = digest;
        }
    }
}
