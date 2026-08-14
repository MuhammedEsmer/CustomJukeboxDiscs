package dev.muhammedesmer.customjukeboxdiscs.permission;

public record AccessDecision(boolean allowed, Reason reason) {
    public enum Reason {
        ALLOWED,
        DENIED,
        NOT_GRANTED
    }

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
