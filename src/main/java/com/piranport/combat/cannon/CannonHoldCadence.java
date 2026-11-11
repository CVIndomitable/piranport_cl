package com.piranport.combat.cannon;

/** Timing for one press; no new shot is permitted until the client confirms a hold. */
final class CannonHoldCadence {
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
    void fired(long now) { nextTick = now + interval; }
    void stop() { stopped = true; }
    boolean simultaneous() { return interval == 0; }
}
