package dev.hoodoo.customjukeboxdiscs.youtube;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ManagedToolInstaller {
    private static final String YT_DLP_VERSION = "2026.08.19";
    private static final String FFMPEG_RELEASE = "autobuild-2026-09-10-15-31";
    private static final String FFMPEG_BUILD = "N-126492-gefb0a7e5e7";

    private final Path directory;
    private final HttpClient http;

    public ManagedToolInstaller(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public synchronized PlatformTools resolve(YouTubeConfig.Snapshot config) {
        try {
            Path ytDlp = explicit(config.ytDlpPath());
            Path ffmpeg = explicit(config.ffmpegPath());
            if (ytDlp != null && ffmpeg != null) return new PlatformTools(ytDlp, ffmpeg);
            if (!config.managedTools()) throw new IOException("managed tools disabled");
            Files.createDirectories(directory);
            if (ytDlp == null) ytDlp = installYtDlp();
            if (ffmpeg == null) ffmpeg = installFfmpeg();
            return new PlatformTools(ytDlp, ffmpeg);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("YouTube tools are unavailable", exception);
        }
    }

    private Path installYtDlp() throws IOException, InterruptedException {
        boolean windows = windows();
        String asset = windows ? "yt-dlp.exe" : "yt-dlp_linux";
        Path target = directory.resolve(windows ? "yt-dlp.exe" : "yt-dlp");
        if (Files.isRegularFile(target)) return target;
        URI root = URI.create("https://github.com/yt-dlp/yt-dlp/releases/download/" + YT_DLP_VERSION + "/");
        String sums = downloadText(root.resolve("SHA2-256SUMS"));
        Path downloaded = download(root.resolve(asset), asset);
        verify(downloaded, checksumFor(sums, asset));
        Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        makeExecutable(target);
        return target;
    }

    private Path installFfmpeg() throws IOException, InterruptedException {
        boolean windows = windows();
        Path target = directory.resolve(windows ? "ffmpeg.exe" : "ffmpeg");
        if (Files.isRegularFile(target)) return target;
        String asset = windows
                ? "ffmpeg-" + FFMPEG_BUILD + "-win64-gpl.zip"
                : "ffmpeg-" + FFMPEG_BUILD + "-linux64-gpl.tar.xz";
        URI root = URI.create("https://github.com/BtbN/FFmpeg-Builds/releases/download/" + FFMPEG_RELEASE + "/");
        String sums = downloadText(root.resolve("checksums.sha256"));
        Path archive = download(root.resolve(asset), asset);
        verify(archive, checksumFor(sums, asset));
        Path extraction = directory.resolve("extract-" + UUID.randomUUID());
        Files.createDirectories(extraction);
        try {
            if (windows) extractWindowsFfmpeg(archive, extraction);
            else extractLinuxFfmpeg(archive, extraction);
            Path binary;
            try (var paths = Files.walk(extraction)) {
                binary = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equals(windows ? "ffmpeg.exe" : "ffmpeg"))
                        .findFirst().orElseThrow(() -> new IOException("FFmpeg binary missing from archive"));
            }
            Files.move(binary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            makeExecutable(target);
        } finally {
            Files.deleteIfExists(archive);
            deleteTree(extraction);
        }
        return target;
    }

    private void extractWindowsFfmpeg(Path archive, Path extraction) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith("/bin/ffmpeg.exe")) {
                    Files.copy(zip, extraction.resolve("ffmpeg.exe"));
                    return;
                }
            }
        }
        throw new IOException("FFmpeg binary missing from zip");
    }

    private static void extractLinuxFfmpeg(Path archive, Path extraction) throws IOException, InterruptedException {
        // Java has no xz decoder; invoking tar directly avoids shipping another native or shaded dependency.
        Process process = new ProcessBuilder(
                "tar", "-xJf", archive.toString(), "-C", extraction.toString()).inheritIO().start();
        if (!process.waitFor(2, java.util.concurrent.TimeUnit.MINUTES) || process.exitValue() != 0) {
            process.destroyForcibly();
            throw new IOException("could not extract FFmpeg archive");
        }
    }

    private Path download(URI uri, String name) throws IOException, InterruptedException {
        Path temporary = directory.resolve(name + ".download");
        Files.deleteIfExists(temporary);
        HttpResponse<Path> response = http.send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10)).GET().build(),
                HttpResponse.BodyHandlers.ofFile(temporary));
        if (response.statusCode() / 100 != 2) {
            Files.deleteIfExists(temporary);
            throw new IOException("download returned HTTP " + response.statusCode());
        }
        return temporary;
    }

    private String downloadText(URI uri) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IOException("checksum download failed");
        return response.body();
    }

    private static String checksumFor(String sums, String asset) throws IOException {
        for (String line : sums.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.endsWith(" " + asset) || trimmed.endsWith(" *" + asset)) {
                return trimmed.substring(0, 64).toLowerCase(Locale.ROOT);
            }
        }
        throw new IOException("checksum missing for " + asset);
    }

    private static void verify(Path file, String expected) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(expected)) {
                Files.deleteIfExists(file);
                throw new IOException("tool checksum mismatch");
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Path explicit(String configured) throws IOException {
        if (configured == null || configured.isBlank()) return null;
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) throw new IOException("configured tool does not exist: " + path);
        return path;
    }

    private static boolean windows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!(arch.equals("amd64") || arch.equals("x86_64"))) {
            throw new IllegalStateException("only x86-64 servers are supported");
        }
        if (os.contains("win")) return true;
        if (os.contains("linux")) return false;
        throw new IllegalStateException("unsupported server operating system: " + os);
    }

    private static void makeExecutable(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
