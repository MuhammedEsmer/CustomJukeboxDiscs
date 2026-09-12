package dev.hoodoo.customjukeboxdiscs.youtube;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

@FunctionalInterface
public interface CommandRunner {
    Result run(List<String> command, Duration timeout, BooleanSupplier cancelled) throws IOException;

    record Result(int exitCode, String output) {
    }
}
