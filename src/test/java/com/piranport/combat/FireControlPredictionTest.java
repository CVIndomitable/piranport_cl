package com.piranport.combat;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireControlPredictionTest {

    @Test
    void samplerUsesFiveTickWindow() {
        var s = new FireControlPrediction.PositionSampler();
        assertEquals(Vec3.ZERO, s.velocity());
        for (int i = 0; i < 8; i++) s.push(new Vec3(i * 0.5, 0, 0));
        assertEquals(FireControlPrediction.SAMPLE_WINDOW, s.size());
        assertEquals(0.5, s.velocity().x, 1e-9);
    }

    @Test
    void samplerSmoothsJitter() {
        var s = new FireControlPrediction.PositionSampler();
        double[] xs = {0, 0.6, 0.8, 1.6, 2.0};
        for (double x : xs) s.push(new Vec3(x, 0, 0));
        assertEquals(0.5, s.velocity().x, 1e-9); // (2.0 - 0) / 4
    }

    @Test
    void samplerClampsTeleport() {
        var s = new FireControlPrediction.PositionSampler();
        s.push(Vec3.ZERO);
        s.push(new Vec3(1000, 0, 0));
        assertEquals(FireControlPrediction.MAX_SAMPLED_SPEED, s.velocity().length(), 1e-9);
    }

    @Test
    void interceptStationaryTargetIsTargetPosition() {
        Vec3 hit = FireControlPrediction.torpedoIntercept(Vec3.ZERO, new Vec3(30, 0, 40), Vec3.ZERO, 1.2);
        assertNotNull(hit);
        assertEquals(30, hit.x, 1e-9);
        assertEquals(40, hit.z, 1e-9);
    }

    @Test
    void interceptCrossingTargetSatisfiesEquation() {
        Vec3 origin = Vec3.ZERO;
        Vec3 target = new Vec3(0, 0, 50);
        Vec3 vel = new Vec3(0.4, 0, 0);
        double speed = 1.2;
        Vec3 hit = FireControlPrediction.torpedoIntercept(origin, target, vel, speed);
        assertNotNull(hit);
        double t = (hit.x - target.x) / vel.x;
        assertTrue(t > 0);
        double dist = Math.hypot(hit.x - origin.x, hit.z - origin.z);
        assertEquals(speed * t, dist, 1e-6);
    }

    @Test
    void interceptFasterFleeingTargetHasNoSolution() {
        assertNull(FireControlPrediction.torpedoIntercept(Vec3.ZERO, new Vec3(0, 0, 50), new Vec3(0, 0, 2.0), 1.2));
    }

    @Test
    void interceptEqualSpeedApproachingTargetUsesLinearRoot() {
        Vec3 hit = FireControlPrediction.torpedoIntercept(Vec3.ZERO, new Vec3(0, 0, 50), new Vec3(0, 0, -1.2), 1.2);
        assertNotNull(hit);
        assertEquals(25, hit.z, 1e-9);
    }

    @Test
    void rayHitsSphereWithinRadius() {
        Vec3 eye = Vec3.ZERO;
        Vec3 center = new Vec3(10, 0, 0);
        assertTrue(FireControlPrediction.rayHitsSphere(eye, new Vec3(1, 0, 0), center, 0.5));
        assertTrue(FireControlPrediction.rayHitsSphere(eye, new Vec3(10, 0.4, 0), center, 0.5));
        assertFalse(FireControlPrediction.rayHitsSphere(eye, new Vec3(10, 0.6, 0), center, 0.5));
        assertFalse(FireControlPrediction.rayHitsSphere(eye, new Vec3(-1, 0, 0), center, 0.5));
    }

    @Test
    void fanAngleIsTwiceMaxAbsSpread() {
        assertEquals(12.0, FireControlPrediction.fanAngleDegrees(new float[]{-6f, -2f, 2f, 6f}), 1e-9);
        assertEquals(0.0, FireControlPrediction.fanAngleDegrees(new float[]{0f}), 1e-9);
        assertEquals(0.0, FireControlPrediction.fanAngleDegrees(null), 1e-9);
    }

    @Test
    void predictImpactLeadsMovingTarget() {
        // 同 BallisticSolverTest：注入常数 supplier，避免拉起配置类静态初始化
        var original = BallisticSolver.setConfigSuppliers(100, 4000, 0.01, 5.0, 32, false);
        try {
            assertLeads();
        } finally {
            original.restore();
            BallisticSolver.clearCache();
        }
    }

    private static void assertLeads() {
        Vec3 origin = Vec3.ZERO;
        Vec3 aim = new Vec3(60, 0, 0);
        Vec3 still = BallisticSolver.predictImpactPoint(origin, aim, Vec3.ZERO, 3.0, 0.01, 0.05,
                BallisticSolver.UNRESTRICTED_MIN_ANGLE, Math.toRadians(60), 2);
        assertEquals(aim.x, still.x, 1e-9);
        Vec3 moving = BallisticSolver.predictImpactPoint(origin, aim, new Vec3(0, 0, 0.3), 3.0, 0.01, 0.05,
                BallisticSolver.UNRESTRICTED_MIN_ANGLE, Math.toRadians(60), 2);
        assertTrue(moving.z > 0, "should lead target along its velocity");
    }
}
