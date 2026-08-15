package dev.muhammedesmer.customjukeboxdiscs.client.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Guards the strings that live inside a fixed width widget.
 *
 * <p>The vanilla font averages a little over six pixels per character, so a budget is the widget width
 * divided by six. Text that overruns its box is not caught by anything else in the build.
 */
final class ScreenTextBudgetTest {
    /** key -> how many characters fit in the widget that draws it */
    private static final Map<String, Integer> BUDGETS = Map.of(
            // Edit boxes are 176 px wide and inset by a few pixels on both sides.
            "screen.customjukeboxdiscs.disc_writer.title_hint", 27,
            "screen.customjukeboxdiscs.disc_writer.url_hint", 27,
            // Buttons are 56 px wide.
            "screen.customjukeboxdiscs.disc_writer.folder", 9,
            "screen.customjukeboxdiscs.disc_writer.refresh", 9,
            "screen.customjukeboxdiscs.disc_writer.write", 9,
            // The status line spans the window minus its border.
            "upload.customjukeboxdiscs.hashing", 29,
            "upload.customjukeboxdiscs.fetching", 29,
            "upload.customjukeboxdiscs.progress", 29,
            "upload.customjukeboxdiscs.complete", 29,
            "upload.customjukeboxdiscs.busy", 29);

    @ParameterizedTest
    @ValueSource(strings = {"en_us", "tr_tr"})
    void everyOnScreenStringFitsItsWidget(String language) throws IOException {
        JsonObject translations = load(language);

        BUDGETS.forEach((key, budget) -> {
            assertTrue(translations.has(key), language + " is missing " + key);
            String text = translations.get(key).getAsString();
            assertTrue(text.length() <= budget,
                    language + " " + key + " is " + text.length() + " characters, budget " + budget
                            + ": \"" + text + "\"");
        });
    }

    private static JsonObject load(String language) throws IOException {
        String path = "/assets/customjukeboxdiscs/lang/" + language + ".json";
        try (InputStream input = ScreenTextBudgetTest.class.getResourceAsStream(path)) {
            assertTrue(input != null, "missing language file " + path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
