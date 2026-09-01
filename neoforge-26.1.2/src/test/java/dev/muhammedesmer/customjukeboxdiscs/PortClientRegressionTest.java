package dev.muhammedesmer.customjukeboxdiscs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortClientRegressionTest {
    private final Path projectDirectory = Path.of(System.getProperty("customjukeboxdiscs.projectDir"));

    @Test
    void discWriterTextColorsAreOpaque() throws IOException {
        String source = source("client/screen/DiscWriterScreen.java");

        assertAll(
                () -> assertTrue(source.contains("0xFFFFFFFF")),
                () -> assertTrue(source.contains("0xFF404040")),
                () -> assertTrue(source.contains("0xFF3F3528")));
    }

    @Test
    void rackDiscsFaceAwayFromTheBlock() throws IOException {
        String source = source("client/render/DiscRackRenderer.java");

        assertTrue(source.contains("Axis.YP.rotationDegrees(180.0F)"));
    }

    @Test
    void rackDiscsDoNotUseTheOutlineRenderLayer() throws IOException {
        String source = source("client/render/DiscRackRenderer.java");

        assertTrue(source.contains("state.lightCoords, OverlayTexture.NO_OVERLAY, 0)"));
    }

    @Test
    void keyCategoryHasTranslationsForTheNewCategoryApi() throws IOException {
        String key = "\"key.category.customjukeboxdiscs.custom_jukebox_discs\"";

        assertAll(
                () -> assertTrue(language("en_us").contains(key)),
                () -> assertTrue(language("tr_tr").contains(key)));
    }

    @Test
    void playbackToastUsesTheTrackTitle() throws IOException {
        String source = source("client/transfer/ClientPlaybackManager.java");

        assertTrue(source.contains("setNowPlaying(Component.literal(play.track.title()))"));
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(projectDirectory.resolve(
                "src/main/java/dev/muhammedesmer/customjukeboxdiscs/" + relativePath));
    }

    private String language(String language) throws IOException {
        return Files.readString(projectDirectory.resolve(
                "src/main/resources/assets/customjukeboxdiscs/lang/" + language + ".json"));
    }
}
