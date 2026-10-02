package com.piranport.combat;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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

    // ===== 预瞄点 HUD 投影（火控/06：落点画在 HUD 而非世界空间）=====

    /** 与 GameRenderer 同构的透视矩阵：垂直 FOV(度)、aspect、近远平面。 */
    private static Matrix4f perspective(float fovDeg, float aspect) {
        return new Matrix4f().setPerspective((float) Math.toRadians(fovDeg), aspect, 0.05f, 1000f);
    }

    /**
     * JOML 右手系绕 +Y 旋转：rotateY(+45°) 把相机朝向从正北（-Z）转到西北，
     * 等价于 MC 里向左转头 45°。
     */
    private static org.joml.Quaternionf yaw(float deg) {
        return new org.joml.Quaternionf().rotateY((float) Math.toRadians(deg));
    }

    @Test
    void pointStraightAheadLandsAtScreenCentre() {
        // 相机在原点、朝向 -Z，正前方 10 格的点应落在屏幕正中
        var p = FireControlPrediction.projectToScreen(new Vec3(0, 0, -10), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        assertNotNull(p);
        assertTrue(p.onScreen());
        assertEquals(960f, p.x(), 0.01f);
        assertEquals(540f, p.y(), 0.01f);
    }

    @Test
    void pointToTheRightLandsOnTheRightHalf() {
        var p = FireControlPrediction.projectToScreen(new Vec3(4, 0, -10), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        assertNotNull(p);
        assertTrue(p.x() > 960f, "偏右的目标必须画在屏幕右半边");
        assertEquals(540f, p.y(), 0.01f);
    }

    @Test
    void yawedCameraMovesMarkerInsteadOfWorld() {
        // 相机向左转 45°后，正北的目标落到了视野右侧 —— 标记必须跟着相机转，而不是钉在屏幕上
        var p = FireControlPrediction.projectToScreen(new Vec3(0, 0, -10), Vec3.ZERO,
                yaw(45f), perspective(70f, 16f / 9f), 1920, 1080, 12);
        assertNotNull(p);
        assertTrue(p.onScreen());
        assertTrue(p.x() > 960f, "相机左转后正北目标应在屏幕右半边，实际 x=" + p.x());
    }

    @Test
    void cameraDoesNotNeedToSitAtOrigin() {
        // 相机位移不应改变投影结果（只用相对位移）
        var atOrigin = FireControlPrediction.projectToScreen(new Vec3(0, 0, -10), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        var farAway = FireControlPrediction.projectToScreen(new Vec3(1000, 64, 990), new Vec3(1000, 64, 1000),
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        assertNotNull(atOrigin);
        assertNotNull(farAway);
        assertEquals(atOrigin.x(), farAway.x(), 0.01f);
        assertEquals(atOrigin.y(), farAway.y(), 0.01f);
    }

    @Test
    void behindCameraIsDroppedRatherThanClamped() {
        // 相机背后的点投影是镜像假点：必须整帧丢弃，否则会把标记钳到相反方向
        assertNull(FireControlPrediction.projectToScreen(new Vec3(0, 0, 10), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12));
    }

    @Test
    void farOffAxisIsClampedToScreenEdgeAndFlaggedOffScreen() {
        // 视野外但仍在相机前方的点：钳到边缘并标记 offScreen，供 HUD 画方向指示
        var p = FireControlPrediction.projectToScreen(new Vec3(500, 0, -10), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        assertNotNull(p);
        assertFalse(p.onScreen());
        assertEquals(1920 - 12, p.x(), 0.01f);
        assertEquals(540f, p.y(), 0.01f);
    }

    @Test
    void markerPositionIsIndependentOfDistance() {
        // 正前方的点无论多远都画在屏幕正中 —— 这正是从世界空间搬到 HUD 的目的
        var near = FireControlPrediction.projectToScreen(new Vec3(0, 0, -8), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        var far = FireControlPrediction.projectToScreen(new Vec3(0, 0, -400), Vec3.ZERO,
                new org.joml.Quaternionf(), perspective(70f, 16f / 9f), 1920, 1080, 12);
        assertNotNull(near);
        assertNotNull(far);
        assertEquals(near.x(), far.x(), 0.01f);
        assertEquals(near.y(), far.y(), 0.01f);
    }
}
