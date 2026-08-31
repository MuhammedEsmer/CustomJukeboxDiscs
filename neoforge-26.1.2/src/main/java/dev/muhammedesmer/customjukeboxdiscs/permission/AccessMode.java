package dev.muhammedesmer.customjukeboxdiscs.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum AccessMode {
    OPS,
    ALLOWLIST,
    EVERYONE;

    public static final Codec<AccessMode> CODEC = Codec.STRING.comapFlatMap(AccessMode::decode, AccessMode::serializedName);

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<AccessMode> decode(String value) {
        for (AccessMode mode : values()) {
            if (mode.serializedName().equals(value)) {
                return DataResult.success(mode);
            }
        }
        return DataResult.error(() -> "Unsupported access mode: " + value);
    }
}
