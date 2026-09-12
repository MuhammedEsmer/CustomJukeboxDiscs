package dev.hoodoo.customjukeboxdiscs.youtube;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class ProcessCommandRunner implements CommandRunner {
    private static final int MAX_CAPTURE_BYTES = 64 * 1024;

    @Override
    public Result run(List<String> command, Duration timeout, BooleanSupplier cancelled) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> drain(process.getInputStream(), captured));
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (process.isAlive()) {
                if (cancelled.getAsBoolean() || System.nanoTime() >= deadline) {
                    destroyTree(process);
                    return new Result(-1, captured.toString(StandardCharsets.UTF_8));
                }
                try {
                    process.waitFor(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    destroyTree(process);
                    return new Result(-1, captured.toString(StandardCharsets.UTF_8));
                }
            }
            try {
                reader.join(1000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return new Result(process.exitValue(), captured.toString(StandardCharsets.UTF_8));
        } finally {
            if (process.isAlive()) destroyTree(process);
        }
    }

    private static void drain(InputStream input, ByteArrayOutputStream captured) {
        try (input) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                int remaining = MAX_CAPTURE_BYTES - captured.size();
                if (remaining > 0) captured.write(buffer, 0, Math.min(remaining, count));
            }
        } catch (IOException ignored) {
        }
    }

    private static void destroyTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}
