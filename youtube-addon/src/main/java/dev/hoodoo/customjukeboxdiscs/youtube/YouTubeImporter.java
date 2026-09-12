package dev.hoodoo.customjukeboxdiscs.youtube;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportRequest;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlImportResult;
import dev.muhammedesmer.customjukeboxdiscs.api.url.UrlTrackImporter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class YouTubeImporter implements UrlTrackImporter, AutoCloseable {
    private final Supplier<PlatformTools> tools;
    private final CommandRunner runner;
    private final ThreadPoolExecutor executor;
    private final BooleanSupplier enabled;

    public YouTubeImporter(Supplier<PlatformTools> tools, CommandRunner runner, int maxQueued) {
        this(tools, runner, maxQueued, () -> true);
    }

    public YouTubeImporter(
            Supplier<PlatformTools> tools, CommandRunner runner, int maxQueued, BooleanSupplier enabled) {
        this.tools = Objects.requireNonNull(tools, "tools");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        if (maxQueued < 1) throw new IllegalArgumentException("maxQueued must be positive");
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueued), runnable -> {
                    Thread thread = new Thread(runnable, "CustomJukeboxDiscs-YouTube");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public String id() {
        return "customjukeboxdiscsyoutube:youtube";
    }

    @Override
    public boolean supports(URI uri) {
        return enabled.getAsBoolean() && YouTubeUrl.canonicalize(uri).isPresent();
    }

    @Override
    public CompletableFuture<UrlImportResult> importTrack(UrlImportRequest request) {
        CompletableFuture<UrlImportResult> result = new CompletableFuture<>();
        try {
            executor.execute(() -> result.complete(importNow(request)));
        } catch (RejectedExecutionException exception) {
            result.complete(UrlImportResult.failure(UrlImportResult.Error.QUEUE_FULL));
        }
        return result;
    }

    private UrlImportResult importNow(UrlImportRequest request) {
        URI uri = YouTubeUrl.canonicalize(request.uri()).orElse(null);
        if (uri == null) return UrlImportResult.failure(UrlImportResult.Error.UNSUPPORTED_MEDIA);
        Path staged = Path.of(request.destination().toAbsolutePath() + ".mp3");
        try {
            if (request.cancelled().getAsBoolean()) return cancelled();
            PlatformTools resolved = tools.get();
            request.progress().accept(UrlImportRequest.Stage.DOWNLOADING);
            CommandRunner.Result metadata = runner.run(
                    MediaCommands.metadata(resolved.ytDlp(), uri), request.timeout(), request.cancelled());
            if (metadata.exitCode() == -1) return interrupted(request);
            if (metadata.exitCode() != 0) return failed(UrlImportResult.Error.DOWNLOAD_FAILED);
            JsonObject json = JsonParser.parseString(metadata.output()).getAsJsonObject();
            if (json.has("is_live") && json.get("is_live").getAsBoolean()) {
                return failed(UrlImportResult.Error.UNSUPPORTED_MEDIA);
            }
            long duration = json.has("duration") ? json.get("duration").getAsLong() : Long.MAX_VALUE;
            if (duration > request.maxDuration().toSeconds()) return failed(UrlImportResult.Error.TOO_LONG);
            if (request.cancelled().getAsBoolean()) return cancelled();
            request.progress().accept(UrlImportRequest.Stage.CONVERTING);
            CommandRunner.Result download = runner.run(
                    MediaCommands.download(
                            resolved.ytDlp(), resolved.ffmpeg(), uri, request.destination(), request.maxBytes()),
                    request.timeout(), request.cancelled());
            if (download.exitCode() == -1) return interrupted(request);
            if (download.exitCode() != 0 || !Files.isRegularFile(staged)) {
                return failed(UrlImportResult.Error.CONVERSION_FAILED);
            }
            if (Files.size(staged) > request.maxBytes()) return failed(UrlImportResult.Error.TOO_LONG);
            Files.move(staged, request.destination(), StandardCopyOption.REPLACE_EXISTING);
            return UrlImportResult.success();
        } catch (IOException | RuntimeException exception) {
            return failed(UrlImportResult.Error.TOOLS_UNAVAILABLE);
        } finally {
            cleanupStaged(request.destination());
        }
    }

    private static void cleanupStaged(Path destination) {
        Path absolute = destination.toAbsolutePath();
        Path parent = absolute.getParent();
        String prefix = absolute.getFileName() + ".";
        try (var files = Files.list(parent)) {
            for (Path file : files.filter(path -> path.getFileName().toString().startsWith(prefix)).toList()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
        }
    }

    private static UrlImportResult failed(UrlImportResult.Error error) {
        return UrlImportResult.failure(error);
    }

    private static UrlImportResult cancelled() {
        return failed(UrlImportResult.Error.CANCELLED);
    }

    private static UrlImportResult interrupted(UrlImportRequest request) {
        return request.cancelled().getAsBoolean()
                ? cancelled()
                : failed(UrlImportResult.Error.TIMED_OUT);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
