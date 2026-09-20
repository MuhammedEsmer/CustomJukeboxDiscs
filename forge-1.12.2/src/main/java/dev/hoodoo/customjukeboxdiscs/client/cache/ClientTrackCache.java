package dev.hoodoo.customjukeboxdiscs.client.cache;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ClientTrackCache {
    private final Path root;
    private final long maxBytes;
    private final Map<String, Download> downloads = new ConcurrentHashMap<>();

    public ClientTrackCache(Path gameDirectory, String serverIdentity, long maxBytes) {
        this.root = gameDirectory.toAbsolutePath().normalize().resolve("customjukeboxdiscs/cache").resolve(key(serverIdentity));
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
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException ignored) {
        }
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
        if (download.received != download.size || !bytesToHex(download.digest.digest()).equals(hash)) {
            Files.deleteIfExists(download.temporary);
            throw new IOException("download hash mismatch");
        }
        Path target = target(hash, download.format);
        Files.move(download.temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        evict(target);
        return Optional.of(target);
    }

    public synchronized Path importVerified(Path source, String hash, AudioFormat format) throws IOException {
        requireHash(hash);
        long size = Files.size(source);
        if (size <= 0 || size > maxBytes) throw new IOException("invalid cached file size");
        Files.createDirectories(root);
        Path temporary = root.resolve(hash + ".import.part");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (!fileHash(temporary).equals(hash)) throw new IOException("cached file hash mismatch");
            Path target = target(hash, format);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            evict(target);
            return target;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public synchronized void cancel(String hash) {
        Download download = downloads.remove(hash);
        if (download == null) return;
        try { download.output.close(); } catch (IOException ignored) { }
        try { Files.deleteIfExists(download.temporary); } catch (IOException ignored) { }
    }

    public synchronized void cancelAll() {
        new ArrayList<>(downloads.keySet()).forEach(this::cancel);
    }

    private Path target(String hash, AudioFormat format) {
        requireHash(hash);
        return root.resolve(hash + "." + format.serializedName());
    }

    private void evict(Path protectedPath) throws IOException {
        try (Stream<Path> listed = Files.list(root)) {
            List<Path> files = listed.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().endsWith(".part"))
                    .sorted(Comparator.comparingLong(ClientTrackCache::lastModified))
                    .collect(Collectors.toList());
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
        return bytesToHex(digest.digest(identity.getBytes(StandardCharsets.UTF_8))).substring(0, 24);
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String fileHash(Path path) throws IOException {
        MessageDigest digest = digest();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return bytesToHex(digest.digest());
    }

    private static void requireHash(String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("invalid hash");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private static final class Download {
        final long size;
        final AudioFormat format;
        final Path temporary;
        final OutputStream output;
        final MessageDigest digest;
        long received;

        Download(long size, AudioFormat format, Path temporary, OutputStream output, MessageDigest digest) {
            this.size = size;
            this.format = format;
            this.temporary = temporary;
            this.output = output;
            this.digest = digest;
        }
    }
}
