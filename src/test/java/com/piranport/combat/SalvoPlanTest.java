package com.piranport.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalvoPlanTest {
    @Test
    void zeroIntervalKeepsAllRemainingGunsOnTheCurrentTick() {
        assertEquals(List.of(0, 0, 0), SalvoPlan.remaining(3, 0).delays());
    }

    @Test
    void positiveIntervalStartsAfterTheImmediateFirstShot() {
        assertEquals(List.of(3, 6, 9), SalvoPlan.remaining(3, 3).delays());
    }

    @Test
    void fractionalIntervalsAreRoundedUpToMinecraftTicks() {
        assertEquals(List.of(2, 4), SalvoPlan.remaining(2, 1.6f).delays());
        assertEquals(List.of(1), SalvoPlan.remaining(1, 0.2f).delays());
    }

    @Test
    void invalidPlansAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SalvoPlan.remaining(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> SalvoPlan.remaining(1, -1));
        assertThrows(IllegalArgumentException.class, () -> SalvoPlan.remaining(1, Float.NaN));
    }
}
