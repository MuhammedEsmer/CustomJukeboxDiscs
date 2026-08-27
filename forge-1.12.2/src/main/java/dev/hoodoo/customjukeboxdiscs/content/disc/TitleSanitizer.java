package dev.hoodoo.customjukeboxdiscs.content.disc;

import java.util.regex.Pattern;

public final class TitleSanitizer {
    private static final Pattern FORMATTING_CODE = Pattern.compile("§.?");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TitleSanitizer() {
    }

    public static String sanitize(String title) {
        if (title == null) {
            return "";
        }
        String plain = FORMATTING_CODE.matcher(title).replaceAll("");
        String spaced = WHITESPACE.matcher(plain).replaceAll(" ");
        StringBuilder builder = new StringBuilder(spaced.length());
        int length = spaced.length();
        for (int offset = 0; offset < length; ) {
            int codePoint = spaced.codePointAt(offset);
            if (!Character.isISOControl(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        String compact = builder.toString().trim();
        int codePoints = compact.codePointCount(0, compact.length());
        return codePoints <= TrackReference.MAX_TITLE_CODE_POINTS
                ? compact
                : compact.substring(0, compact.offsetByCodePoints(0, TrackReference.MAX_TITLE_CODE_POINTS));
    }
}
