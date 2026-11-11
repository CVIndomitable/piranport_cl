package com.piranport.combat.cannon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CannonSmokeLimiterTest {
    @Test
    void limitsEachWeaponIndependentlyAndEmitsAtTheBoundary() {
        CannonSmokeLimiter<Object> limiter = new CannonSmokeLimiter<>(CannonProjectiles.SMALL_CALIBER_SMOKE_INTERVAL_TICKS);
        Object first = new Object();
        Object second = new Object();

        assertTrue(limiter.shouldEmit(first, 100));
        assertFalse(limiter.shouldEmit(first, 101));
        assertTrue(limiter.shouldEmit(second, 101));
        assertFalse(limiter.shouldEmit(first, 139));
        assertTrue(limiter.shouldEmit(first, 140));
        assertFalse(limiter.shouldEmit(second, 140));
        assertTrue(limiter.shouldEmit(second, 141));
    }

    @Test
    void resetsWhenClockMovesBackwards() {
        CannonSmokeLimiter<Object> limiter = new CannonSmokeLimiter<>(20);
        Object weapon = new Object();

        assertTrue(limiter.shouldEmit(weapon, 100));
        assertTrue(limiter.shouldEmit(weapon, 0));
        assertFalse(limiter.shouldEmit(weapon, 1));
    }
}
