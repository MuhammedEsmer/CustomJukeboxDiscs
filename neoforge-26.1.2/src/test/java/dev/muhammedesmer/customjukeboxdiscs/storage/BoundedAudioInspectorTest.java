package dev.muhammedesmer.customjukeboxdiscs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BoundedAudioInspectorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsMp3WithLeadingMetadataPadding() throws Exception {
        byte[] source = fixture("audio/one_second.mp3");
        byte[] padded = new byte[source.length + 32];
        System.arraycopy(source, 0, padded, 32, source.length);
        Path file = temporaryDirectory.resolve("padded.mp3");
        Files.write(file, padded);

        InspectionResult result = new BoundedAudioInspector()
                .inspect(file, padded.length, Duration.ofMinutes(10));

        assertEquals(AudioFormat.MP3, result.format());
    }

    @Test
    void rejectsNonAudioFileNamedMp3() throws Exception {
        Path file = temporaryDirectory.resolve("fake.mp3");
        Files.writeString(file, "not audio");

        assertThrows(AudioValidationException.class, () -> new BoundedAudioInspector()
                .inspect(file, 1024, Duration.ofMinutes(10)));
    }

    private static byte[] fixture(String name) throws IOException {
        try (var input = BoundedAudioInspectorTest.class.getClassLoader().getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing fixture: " + name);
            return input.readAllBytes();
        }
    }
}
