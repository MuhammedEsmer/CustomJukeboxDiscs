package dev.muhammedesmer.customjukeboxdiscs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AudioInspectorTest {
    private final AudioInspector inspector = new BoundedAudioInspector();

    @TempDir
    Path temporaryDirectory;

    @Test
    void inspectsRealMp3AndCalculatesHashAndDuration() throws IOException {
        Path source = copyFixture("one_second.mp3");

        InspectionResult result = inspector.inspect(source, 20_000, Duration.ofSeconds(2));

        assertEquals(AudioFormat.MP3, result.format());
        assertEquals(Files.size(source), result.byteCount());
        assertEquals(64, result.sha256().length());
        assertTrue(result.durationMillis() >= 950 && result.durationMillis() <= 1_100,
                () -> "decoded MP3 duration was " + result.durationMillis() + " ms");
    }

    @Test
    void inspectsRealOggVorbis() throws IOException {
        InspectionResult result = inspector.inspect(fixture("one_second.ogg"), 20_000, Duration.ofSeconds(2));

        assertEquals(AudioFormat.OGG, result.format());
        assertTrue(result.durationMillis() >= 950 && result.durationMillis() <= 1_100);
    }

    @Test
    void detectsFormatFromBytesInsteadOfExtension() throws IOException {
        Path deceptive = temporaryDirectory.resolve("not-really-ogg.ogg");
        Files.copy(fixture("one_second.mp3"), deceptive);

        assertEquals(AudioFormat.MP3,
                inspector.inspect(deceptive, 20_000, Duration.ofSeconds(2)).format());
    }

    @Test
    void rejectsBeforeParsingWhenFileIsTooLarge() {
        AudioValidationException exception = assertThrows(AudioValidationException.class,
                () -> inspector.inspect(fixture("one_second.mp3"), 100, Duration.ofSeconds(2)));

        assertEquals(AudioValidationException.Reason.SIZE_LIMIT, exception.reason());
    }

    @Test
    void rejectsUnsupportedAndTruncatedData() throws IOException {
        Path unsupported = temporaryDirectory.resolve("sound.wav");
        Files.write(unsupported, new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0});
        Path truncated = temporaryDirectory.resolve("truncated.ogg");
        Files.write(truncated, new byte[] {'O', 'g', 'g', 'S'});

        assertEquals(AudioValidationException.Reason.UNSUPPORTED_FORMAT,
                assertThrows(AudioValidationException.class,
                        () -> inspector.inspect(unsupported, 100, Duration.ofSeconds(2))).reason());
        assertEquals(AudioValidationException.Reason.MALFORMED,
                assertThrows(AudioValidationException.class,
                        () -> inspector.inspect(truncated, 100, Duration.ofSeconds(2))).reason());
    }

    @Test
    void rejectsTracksOverDurationLimit() throws IOException {
        AudioValidationException exception = assertThrows(AudioValidationException.class,
                () -> inspector.inspect(copyFixture("one_second.mp3"), 20_000, Duration.ofMillis(100)));

        assertEquals(AudioValidationException.Reason.DURATION_LIMIT, exception.reason());
    }

    private static Path fixture(String name) {
        try {
            return Path.of(AudioInspectorTest.class.getResource("/audio/" + name).toURI());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Path copyFixture(String name) throws IOException {
        return Files.copy(fixture(name), temporaryDirectory.resolve(name));
    }
}
