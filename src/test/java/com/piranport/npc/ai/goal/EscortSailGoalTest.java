package com.piranport.npc.ai.goal;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscortSailGoalTest {

    private static final AABB HULL = new AABB(-1, 64, -1, 1, 66, 1);

    @Test
    void pointInsideHullFootprintHasArrived() {
        assertTrue(EscortSailGoal.containsXZ(HULL, new Vec3(0.5, 200, -0.5)));
    }

    @Test
    void pointOutsideHullFootprintHasNotArrived() {
        assertFalse(EscortSailGoal.containsXZ(HULL, new Vec3(1.5, 64, 0)));
    }
}
