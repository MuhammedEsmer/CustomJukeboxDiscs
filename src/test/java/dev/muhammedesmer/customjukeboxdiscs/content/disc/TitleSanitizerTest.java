package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class TitleSanitizerTest {
    @Test
    void collapsesSurroundingAndRepeatedWhitespace() {
        assertEquals("My Song", TitleSanitizer.sanitize("  My   Song "));
    }

    @Test
    void removesSectionSignFormattingCodes() {
        assertEquals("Song", TitleSanitizer.sanitize("§kSo§rng"));
    }

    @Test
    void removesControlCharacters() {
        assertEquals("ab", TitleSanitizer.sanitize("a" + (char) 7 + "b"));
    }

    @Test
    void treatsLineBreaksAndTabsAsWhitespace() {
        assertEquals("a b", TitleSanitizer.sanitize("a\n\tb"));
    }

    @Test
    void capsTitleAtSixtyFourCodePoints() {
        assertEquals(64, TitleSanitizer.sanitize("x".repeat(80)).codePointCount(0, 64));
    }

    @Test
    void countsSupplementaryCharactersAsSingleCodePoints() {
        String emoji = "🎵";

        String sanitized = TitleSanitizer.sanitize(emoji.repeat(80));

        assertEquals(64, sanitized.codePointCount(0, sanitized.length()));
    }

    @Test
    void returnsEmptyForNullAndUnusableInput() {
        assertEquals("", TitleSanitizer.sanitize(null));
        assertEquals("", TitleSanitizer.sanitize("   "));
        assertEquals("", TitleSanitizer.sanitize("§a§b"));
    }
}
