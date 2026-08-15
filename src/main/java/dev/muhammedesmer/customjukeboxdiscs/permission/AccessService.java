package dev.muhammedesmer.customjukeboxdiscs.permission;

import java.util.UUID;

public interface AccessService {
    AccessDecision mayUpload(AccessSubject subject);

    AccessMode mode();

    boolean isAllowed(UUID playerId);

    boolean isDenied(UUID playerId);

    void setMode(AccessMode mode);

    void allow(UUID playerId);

    void deny(UUID playerId);

    void remove(UUID playerId);
}
