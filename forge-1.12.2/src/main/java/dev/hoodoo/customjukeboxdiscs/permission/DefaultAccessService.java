package dev.hoodoo.customjukeboxdiscs.permission;

import java.util.Objects;
import java.util.UUID;

public final class DefaultAccessService implements AccessService {
    public static final int OPERATOR_PERMISSION_LEVEL = 3;

    private final AccessPolicySavedData policy;

    public DefaultAccessService(AccessPolicySavedData policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public AccessDecision mayUpload(AccessSubject subject) {
        if (subject.isConsole() || subject.singleplayerOwner()) {
            return AccessDecision.allowedDecision();
        }
        if (policy.deniedPlayers().contains(subject.getPlayerId())) {
            return AccessDecision.denied(AccessDecision.Reason.DENIED);
        }

        boolean operator = subject.getPermissionLevel() >= OPERATOR_PERMISSION_LEVEL;
        boolean allowed;
        switch (policy.mode()) {
            case OPS:
                allowed = operator;
                break;
            case ALLOWLIST:
                allowed = operator || policy.allowedPlayers().contains(subject.getPlayerId());
                break;
            case EVERYONE:
                allowed = true;
                break;
            default:
                allowed = false;
                break;
        }
        return allowed
                ? AccessDecision.allowedDecision()
                : AccessDecision.denied(AccessDecision.Reason.NOT_GRANTED);
    }

    @Override
    public AccessMode mode() {
        return policy.mode();
    }

    @Override
    public boolean isAllowed(UUID playerId) {
        return policy.allowedPlayers().contains(playerId);
    }

    @Override
    public boolean isDenied(UUID playerId) {
        return policy.deniedPlayers().contains(playerId);
    }

    @Override
    public void setMode(AccessMode mode) {
        policy.setMode(Objects.requireNonNull(mode, "mode"));
    }

    @Override
    public void allow(UUID playerId) {
        policy.allow(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public void deny(UUID playerId) {
        policy.deny(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public void remove(UUID playerId) {
        policy.remove(Objects.requireNonNull(playerId, "playerId"));
    }
}
