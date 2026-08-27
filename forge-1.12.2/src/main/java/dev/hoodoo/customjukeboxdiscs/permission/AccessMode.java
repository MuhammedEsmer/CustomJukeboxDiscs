package dev.hoodoo.customjukeboxdiscs.permission;

import java.util.Locale;

public enum AccessMode {
    OPS,
    ALLOWLIST,
    EVERYONE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AccessMode fromSerializedName(String value) {
        for (AccessMode mode : values()) {
            if (mode.serializedName().equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported access mode: " + value);
    }
}
