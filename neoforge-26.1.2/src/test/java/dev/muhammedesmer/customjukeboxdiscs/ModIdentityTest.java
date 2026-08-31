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
        assertEquals("1.21.1", properties.getProperty("minecraft_version"));
        assertEquals("[1.21.1,1.21.2)", properties.getProperty("minecraft_version_range"));
        assertEquals("21.1.231", properties.getProperty("neo_version"));
        assertEquals("[21.1.231,22)", properties.getProperty("neo_version_range"));
        assertEquals("21", properties.getProperty("java_version"));
    }
}
