package com.piranport.combat.cannon.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CannonFireRequestTest {
    @Test
    void rejectsInvalidPhysics() {
        assertEquals("velocity must be positive", CannonFireService.validatePhysics(1, 1, 0, 1, 0, 0, 0, 8));
        assertEquals("drag must be positive", CannonFireService.validatePhysics(1, 1, 2, 0, 0, 0, 0, 8));
        assertEquals("damage must be finite and non-negative", CannonFireService.validatePhysics(Float.NaN, 1, 2, 1, 0, 0, 0, 8));
    }

}
