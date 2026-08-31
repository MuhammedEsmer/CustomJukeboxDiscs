package dev.muhammedesmer.customjukeboxdiscs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

final class ModIdentityTest {
    @Test
    void metadataTargetsTheApprovedPlatformAndIdentity() throws IOException {
        Properties properties = new Properties();
        Path projectDirectory = Path.of(System.getProperty("customjukeboxdiscs.projectDir"));
        try (var reader = Files.newBufferedReader(projectDirectory.resolve("gradle.properties"))) {
            properties.load(reader);
        }

        assertEquals("customjukeboxdiscs", properties.getProperty("mod_id"));
        assertEquals("Custom Jukebox Discs", properties.getProperty("mod_name"));
        assertEquals("dev.muhammedesmer.customjukeboxdiscs", properties.getProperty("mod_group_id"));
        assertEquals("26.1.2", properties.getProperty("minecraft_version"));
        assertEquals("[26.1.2,26.1.3)", properties.getProperty("minecraft_version_range"));
        assertEquals("26.1.2.94", properties.getProperty("neo_version"));
        assertEquals("[26.1.2.94,26.2)", properties.getProperty("neo_version_range"));
        assertEquals("25", properties.getProperty("java_version"));
    }
}
