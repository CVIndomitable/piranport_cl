package com.piranport.combat.cannon;

/** Timing for one press; no new shot is permitted until the client confirms a hold. */
final class CannonHoldCadence {
    enum ShotResult { FIRED, WAITING_FOR_RELOAD, UNAVAILABLE }

    private final int interval;
    private long nextTick;
    private boolean confirmed;
    private boolean stopped;

    CannonHoldCadence(long firstTick, float intervalTicks) {
        if (!Float.isFinite(intervalTicks) || intervalTicks < 0) {
            throw new IllegalArgumentException("invalid salvo interval");
        }
        interval = intervalTicks == 0 ? 0 : Math.max(1, (int) Math.ceil(intervalTicks));
        nextTick = firstTick + interval;
    }

    void confirmHold() { confirmed = true; }
    boolean canFire(long now) { return confirmed && !stopped && now >= nextTick; }
    boolean simultaneous() { return interval == 0; }

    /** Returns true only if a shot was fired and the same-tick loop may continue. */
    boolean onAttempt(long now, ShotResult result) {
        if (result == ShotResult.UNAVAILABLE) stopped = true;
        if (result != ShotResult.FIRED) return false;
        nextTick = now + interval;
        return true;
    }
}
