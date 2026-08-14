package dev.muhammedesmer.customjukeboxdiscs.client.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UploadFileScannerTest {
    @TempDir
    Path directory;

    @Test
    void scansOnlySupportedFilesInStableOrder() throws Exception {
        Files.writeString(directory.resolve("z-song.MP3"), "x");
        Files.writeString(directory.resolve("A-song.ogg"), "x");
        Files.writeString(directory.resolve("notes.txt"), "x");

        assertEquals(List.of("A-song.ogg", "z-song.MP3"),
                UploadFileScanner.scan(directory, 1_000).stream().map(path -> path.getFileName().toString()).toList());
    }

    @Test
    void appliesScanCapBeforeReturningFiles() throws Exception {
        Files.writeString(directory.resolve("a.mp3"), "x");
        Files.writeString(directory.resolve("b.ogg"), "x");

        assertEquals(1, UploadFileScanner.scan(directory, 1).size());
    }

    @Test
    void resolvesSelectionsOnlyInsideUploadFolder() {
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileScanner.resolveSelection(directory, "../outside.mp3"));
    }

    @Test
    void derivesAndSanitizesTitles() {
        assertEquals("My Song", UploadFileScanner.titleFromFile("  My   Song.MP3 "));
        assertEquals(64, UploadFileScanner.sanitizeTitle("x".repeat(80)).length());
    }
}
