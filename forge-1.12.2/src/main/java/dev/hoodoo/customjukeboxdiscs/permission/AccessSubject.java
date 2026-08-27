package dev.hoodoo.customjukeboxdiscs.permission;

import java.util.Objects;
import java.util.UUID;

public final class AccessSubject {
    private final UUID playerId;
    private final int permissionLevel;
    private final boolean console;

    public AccessSubject(UUID playerId, int permissionLevel, boolean console) {
        if (!console) {
            Objects.requireNonNull(playerId, "playerId");
        }
        if (permissionLevel < 0) {
            throw new IllegalArgumentException("permissionLevel cannot be negative");
        }
        this.playerId = playerId;
        this.permissionLevel = permissionLevel;
        this.console = console;
    }

    public static AccessSubject player(UUID playerId, int permissionLevel) {
        return new AccessSubject(playerId, permissionLevel, false);
    }

    public static AccessSubject serverConsole() {
        return new AccessSubject(null, Integer.MAX_VALUE, true);
    }

    public UUID playerId() { return playerId; }
    public UUID getPlayerId() { return playerId; }
    public int permissionLevel() { return permissionLevel; }
    public int getPermissionLevel() { return permissionLevel; }
    public boolean console() { return console; }
    public boolean isConsole() { return console; }
}
