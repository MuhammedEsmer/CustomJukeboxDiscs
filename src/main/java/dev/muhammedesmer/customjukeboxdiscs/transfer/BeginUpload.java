package dev.muhammedesmer.customjukeboxdiscs.transfer;

import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.util.Objects;
import java.util.regex.Pattern;

public record BeginUpload(
        String clientHash,
        long declaredBytes,
        AudioFormat formatHint,
        String title,
        String uploaderName) {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public BeginUpload {
        if (clientHash == null || !SHA_256.matcher(clientHash).matches()) {
            throw new IllegalArgumentException("clientHash must be a lowercase SHA-256 value");
        }
        if (declaredBytes <= 0) {
            throw new IllegalArgumentException("declaredBytes must be positive");
        }
        Objects.requireNonNull(formatHint, "formatHint");
        requireCodePoints(title, 64, "title");
        requireCodePoints(uploaderName, 16, "uploaderName");
    }

    public BeginUpload withTitle(String value) {
        return new BeginUpload(clientHash, declaredBytes, formatHint, value, uploaderName);
    }

    private static void requireCodePoints(String value, int maximum, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        int length = value.codePointCount(0, value.length());
        if (length < 1 || length > maximum) {
            throw new IllegalArgumentException(name + " must contain 1-" + maximum + " code points");
        }
    }
}
