package dev.muhammedesmer.customjukeboxdiscs.transfer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class UploadErrorTranslationTest {
    @Test
    void everyFailureHasItsOwnTranslationKey() {
        List<String> keys = new ArrayList<>();
        for (UploadError error : UploadError.values()) {
            if (error != UploadError.NONE) {
                keys.add(error.translationKey());
            }
        }

        assertTrue(keys.stream().distinct().count() == keys.size(), "translation keys must be unique");
    }

    @ParameterizedTest
    @ValueSource(strings = {"en_us", "tr_tr"})
    void everyFailureIsTranslated(String language) throws IOException {
        JsonObject translations = load(language);

        for (UploadError error : UploadError.values()) {
            if (error == UploadError.NONE) {
                continue;
            }
            assertTrue(translations.has(error.translationKey()),
                    language + " is missing " + error.translationKey());
        }
    }

    private static JsonObject load(String language) throws IOException {
        String path = "/assets/customjukeboxdiscs/lang/" + language + ".json";
        try (InputStream input = UploadErrorTranslationTest.class.getResourceAsStream(path)) {
            assertTrue(input != null, "missing language file " + path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
