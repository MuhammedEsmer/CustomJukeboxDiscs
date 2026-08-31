package dev.muhammedesmer.customjukeboxdiscs.permission;

import java.util.Objects;
import java.util.UUID;

public final class DefaultAccessService implements AccessService {
    public static final int OPERATOR_PERMISSION_LEVEL = 3;

    private final AccessPolicyData policy;

    public DefaultAccessService(AccessPolicyData policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public AccessDecision mayUpload(AccessSubject subject) {
        if (subject.console()) {
            return AccessDecision.allowedDecision();
        }
        if (policy.deniedPlayers().contains(subject.playerId())) {
            return AccessDecision.denied(AccessDecision.Reason.DENIED);
        }

        boolean operator = subject.permissionLevel() >= OPERATOR_PERMISSION_LEVEL;
        boolean allowed = switch (policy.mode()) {
            case OPS -> operator;
            case ALLOWLIST -> operator || policy.allowedPlayers().contains(subject.playerId());
            case EVERYONE -> true;
        };
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
