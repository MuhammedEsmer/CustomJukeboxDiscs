package dev.muhammedesmer.customjukeboxdiscs.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class AccessServiceTest {
    private static final UUID PLAYER = UUID.fromString("12345678-1234-5678-9234-567812345678");

    private AccessPolicyData policy;
    private DefaultAccessService service;

    @BeforeEach
    void setUp() {
        policy = new AccessPolicyData();
        service = new DefaultAccessService(policy);
    }

    @ParameterizedTest
    @MethodSource("modeDecisions")
    void evaluatesModes(AccessMode mode, int permissionLevel, boolean allowlisted, boolean expected) {
        service.setMode(mode);
        if (allowlisted) {
            service.allow(PLAYER);
        }

        assertEquals(expected, service.mayUpload(AccessSubject.player(PLAYER, permissionLevel)).allowed());
    }

    @Test
    void denyTakesPrecedenceOverOperatorAndEveryMode() {
        service.setMode(AccessMode.EVERYONE);
        service.allow(PLAYER);
        service.deny(PLAYER);

        AccessDecision decision = service.mayUpload(AccessSubject.player(PLAYER, 4));

        assertFalse(decision.allowed());
        assertEquals(AccessDecision.Reason.DENIED, decision.reason());
    }

    @Test
    void consoleCannotBeDenied() {
        assertTrue(service.mayUpload(AccessSubject.serverConsole()).allowed());
    }

    @Test
    void removeClearsBothLists() {
        service.allow(PLAYER);
        service.deny(PLAYER);
        service.remove(PLAYER);

        assertFalse(policy.allowedPlayers().contains(PLAYER));
        assertFalse(policy.deniedPlayers().contains(PLAYER));
    }

    @Test
    void mutationsMarkSavedDataDirty() {
        assertFalse(policy.isDirty());
        service.allow(PLAYER);
        assertTrue(policy.isDirty());
    }

    @Test
    void policyCodecRoundTrips() {
        service.setMode(AccessMode.ALLOWLIST);
        service.allow(PLAYER);
        UUID denied = UUID.fromString("87654321-4321-6789-9234-567812345678");
        service.deny(denied);

        JsonElement encoded = AccessPolicyData.CODEC.encodeStart(JsonOps.INSTANCE, policy).getOrThrow();
        AccessPolicyData decoded = AccessPolicyData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(AccessMode.ALLOWLIST, decoded.mode());
        assertEquals(policy.allowedPlayers(), decoded.allowedPlayers());
        assertEquals(policy.deniedPlayers(), decoded.deniedPlayers());
    }

    private static Stream<Arguments> modeDecisions() {
        return Stream.of(
                Arguments.of(AccessMode.OPS, 2, false, false),
                Arguments.of(AccessMode.OPS, 3, false, true),
                Arguments.of(AccessMode.ALLOWLIST, 2, false, false),
                Arguments.of(AccessMode.ALLOWLIST, 2, true, true),
                Arguments.of(AccessMode.ALLOWLIST, 3, false, true),
                Arguments.of(AccessMode.EVERYONE, 0, false, true));
    }
}
