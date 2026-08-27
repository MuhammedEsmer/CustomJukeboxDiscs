package dev.hoodoo.customjukeboxdiscs.permission;

public final class AccessDecision {
    public enum Reason {
        ALLOWED,
        DENIED,
        NOT_GRANTED
    }

    private final boolean allowed;
    private final Reason reason;

    public AccessDecision(boolean allowed, Reason reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    public boolean allowed() { return allowed; }
    public boolean isAllowed() { return allowed; }
    public Reason reason() { return reason; }
    public Reason getReason() { return reason; }

    public static AccessDecision allowedDecision() {
        return new AccessDecision(true, Reason.ALLOWED);
    }

    public static AccessDecision denied(Reason reason) {
        if (reason == Reason.ALLOWED) {
            throw new IllegalArgumentException("A denied decision requires a denial reason");
        }
        return new AccessDecision(false, reason);
    }
}
