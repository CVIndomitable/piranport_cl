package com.piranport.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipTypeLoadTest {
    @Test
    void coreLoadDefaultsFollowTheLatestDecision() {
        assertEquals(72, ShipType.SUBMARINE.maxLoad);
        assertEquals(80, ShipType.SMALL.maxLoad);
        assertEquals(96, ShipType.MEDIUM.maxLoad);
        assertEquals(112, ShipType.LARGE.maxLoad);
    }
}
