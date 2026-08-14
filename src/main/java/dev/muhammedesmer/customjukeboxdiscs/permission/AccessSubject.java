package dev.muhammedesmer.customjukeboxdiscs.permission;

import java.util.Objects;
import java.util.UUID;

public record AccessSubject(UUID playerId, int permissionLevel, boolean console) {
    public AccessSubject {
        if (!console) {
            Objects.requireNonNull(playerId, "playerId");
        }
        if (permissionLevel < 0) {
            throw new IllegalArgumentException("permissionLevel cannot be negative");
        }
    }

    public static AccessSubject player(UUID playerId, int permissionLevel) {
        return new AccessSubject(playerId, permissionLevel, false);
    }

    public static AccessSubject serverConsole() {
        return new AccessSubject(null, Integer.MAX_VALUE, true);
    }
}
