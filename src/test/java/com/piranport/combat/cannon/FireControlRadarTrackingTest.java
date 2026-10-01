package com.piranport.combat.cannon;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireControlRadarTrackingTest {
    private static final FireControlRadarTracking.Params L1 = new FireControlRadarTracking.Params(8.0, 0.02);

    @Test
    void outsideRangeLeavesVelocityUnchanged() {
        Vec3 v = new Vec3(2, 0, 0);
        Vec3 out = FireControlRadarTracking.steer(Vec3.ZERO, v, new Vec3(20, 0, 5), L1);
        assertSame(v, out);
    }

    @Test
    void insideRangeTurnsTowardTargetAndKeepsSpeed() {
        Vec3 v = new Vec3(2, 0, 0);
        Vec3 out = FireControlRadarTracking.steer(Vec3.ZERO, v, new Vec3(5, 0, 5), L1);
        assertEquals(v.length(), out.length(), 1e-9);
        assertTrue(out.z > 0, "should bend toward +z target");
        assertTrue(out.z < 0.1, "turn should be slight at coefficient 0.02");
    }

    @Test
    void targetBehindIsNotChased() {
        Vec3 v = new Vec3(2, 0, 0);
        Vec3 out = FireControlRadarTracking.steer(Vec3.ZERO, v, new Vec3(-3, 0, 1), L1);
        assertSame(v, out);
    }

    @Test
    void inactiveParamsDoNothing() {
        Vec3 v = new Vec3(2, 0, 0);
        assertSame(v, FireControlRadarTracking.steer(Vec3.ZERO, v, new Vec3(3, 0, 1),
                new FireControlRadarTracking.Params(0, 0.05)));
        assertSame(v, FireControlRadarTracking.steer(Vec3.ZERO, v, new Vec3(3, 0, 1), null));
    }

    @Test
    void higherCoefficientTurnsMore() {
        Vec3 v = new Vec3(2, 0, 0);
        Vec3 aim = new Vec3(5, 0, 5);
        Vec3 l1 = FireControlRadarTracking.steer(Vec3.ZERO, v, aim, L1);
        Vec3 l3 = FireControlRadarTracking.steer(Vec3.ZERO, v, aim, new FireControlRadarTracking.Params(16, 0.06));
        assertTrue(l3.z > l1.z);
    }
}
