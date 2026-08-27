package dev.hoodoo.customjukeboxdiscs.content.disc;

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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) sb.append("x");
        assertEquals(64, TitleSanitizer.sanitize(sb.toString()).codePointCount(0, 64));
    }

    @Test
    void countsSupplementaryCharactersAsSingleCodePoints() {
        String emoji = "🎵";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) sb.append(emoji);

        String sanitized = TitleSanitizer.sanitize(sb.toString());
        assertEquals(64, sanitized.codePointCount(0, sanitized.length()));
    }

    @Test
    void returnsEmptyForNullAndUnusableInput() {
        assertEquals("", TitleSanitizer.sanitize(null));
        assertEquals("", TitleSanitizer.sanitize("   "));
        assertEquals("", TitleSanitizer.sanitize("§a§b"));
    }
}
