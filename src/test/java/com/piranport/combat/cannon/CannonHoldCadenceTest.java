package com.piranport.combat.cannon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CannonHoldCadenceTest {
    @Test
    void pressAloneNeverFiresASecondGun() {
        CannonHoldCadence cadence = new CannonHoldCadence(100, 3);
        assertFalse(cadence.canFire(103));
        cadence.confirmHold();
        assertFalse(cadence.canFire(102));
        assertTrue(cadence.canFire(103));
        cadence.fired(103);
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

    @Test
    void exhaustedSequenceCannotRestartWhileHeld() {
        CannonHoldCadence cadence = new CannonHoldCadence(100, 1);
        cadence.confirmHold();
        cadence.stop();
        cadence.confirmHold();
        assertFalse(cadence.canFire(200));
    }
}
