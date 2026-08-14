package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import java.util.regex.Pattern;

final class PayloadValidation {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private PayloadValidation() {
    }

    static void requireHash(String value) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException("hash must be a lowercase SHA-256 value");
        }
    }

    static void requireCodePoints(String value, int maximum, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        int length = value.codePointCount(0, value.length());
        if (length < 1 || length > maximum) {
            throw new IllegalArgumentException(name + " must contain 1-" + maximum + " code points");
        }
    }
}
