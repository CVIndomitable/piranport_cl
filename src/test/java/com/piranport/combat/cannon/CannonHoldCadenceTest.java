package com.piranport.combat.cannon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.piranport.combat.cannon.CannonHoldCadence.ShotResult.*;
import static org.junit.jupiter.api.Assertions.*;

class CannonHoldCadenceTest {
    @Test
    void pressAloneNeverFiresASecondGun() {
        CannonHoldCadence cadence = new CannonHoldCadence(100, 3);
        assertFalse(cadence.canFire(103));
        cadence.confirmHold();
        assertFalse(cadence.canFire(102));
        assertTrue(cadence.canFire(103));
        assertTrue(cadence.onAttempt(103, FIRED));
        assertFalse(cadence.canFire(105));
        assertTrue(cadence.canFire(106));
    }

    @Test
    void fractionalAndZeroIntervalsMatchSalvoRounding() {
        CannonHoldCadence fractional = new CannonHoldCadence(100, 1.5f);
        fractional.confirmHold();
        assertFalse(fractional.canFire(101));
        assertTrue(fractional.canFire(102));
        CannonHoldCadence simultaneous = new CannonHoldCadence(100, 0);
        simultaneous.confirmHold();
        assertTrue(simultaneous.canFire(100));
        assertTrue(simultaneous.simultaneous());
        assertThrows(IllegalArgumentException.class, () -> new CannonHoldCadence(0, Float.NaN));
    }

    @ParameterizedTest
    @ValueSource(floats = {0, 3})
    void singleGunResumesAfterEachReloadWithoutAnotherPress(float interval) {
        CannonHoldCadence cadence = new CannonHoldCadence(100, interval);
        assertTrue(cadence.onAttempt(100, FIRED));
        cadence.confirmHold();

        // For a zero interval this first attempt can happen in the same tick as the shot.
        long nextAttempt = 100 + (long) interval;
        assertTrue(cadence.canFire(nextAttempt));
        assertFalse(cadence.onAttempt(nextAttempt, WAITING_FOR_RELOAD));
        assertTrue(cadence.canFire(nextAttempt + 1));
        assertFalse(cadence.onAttempt(nextAttempt + 1, WAITING_FOR_RELOAD));
        assertTrue(cadence.canFire(140));
        assertTrue(cadence.onAttempt(140, FIRED));

        assertTrue(cadence.canFire(140 + (long) interval));
        assertFalse(cadence.onAttempt(140 + (long) interval, WAITING_FOR_RELOAD));
        assertTrue(cadence.canFire(180));
        assertTrue(cadence.onAttempt(180, FIRED));
    }

    @Test
    void pressingDuringReloadStillRequiresHoldConfirmation() {
        CannonHoldCadence cadence = new CannonHoldCadence(100, 0);
        assertFalse(cadence.onAttempt(100, WAITING_FOR_RELOAD));
        assertFalse(cadence.canFire(110));
        cadence.confirmHold();
        assertTrue(cadence.canFire(110));
    }

    @Test
    void exhaustedSequenceCannotRestartWhileHeld() {
        CannonHoldCadence cadence = new CannonHoldCadence(100, 1);
        cadence.confirmHold();
        assertFalse(cadence.onAttempt(101, UNAVAILABLE));
        cadence.confirmHold();
        assertFalse(cadence.canFire(200));
    }
}
