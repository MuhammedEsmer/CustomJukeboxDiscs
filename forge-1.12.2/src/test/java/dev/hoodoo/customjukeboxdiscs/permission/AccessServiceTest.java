package dev.hoodoo.customjukeboxdiscs.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AccessServiceTest {
    private static final UUID PLAYER = UUID.fromString("12345678-1234-5678-9234-567812345678");

    @Test
    void singleplayerOwnerCannotBeDenied() {
        AccessPolicySavedData policy = new AccessPolicySavedData();
        policy.setMode(AccessMode.OPS);
        policy.deny(PLAYER);

        assertTrue(new DefaultAccessService(policy)
                .mayUpload(AccessSubject.singleplayerOwner(PLAYER)).allowed());
    }

    @Test
    void newPolicyAllowsEveryoneByDefault() {
        AccessPolicySavedData policy = new AccessPolicySavedData();

        assertEquals(AccessMode.EVERYONE, policy.mode());
        assertTrue(new DefaultAccessService(policy)
                .mayUpload(AccessSubject.player(PLAYER, 0)).allowed());
    }
}
