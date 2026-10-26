package com.piranport.combat;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 航空鱼雷空投段速度方向的纯函数测试。
 *
 * <p>用户口径：鱼雷机投雷时，鱼雷出生与入水都要保持固定 45° 角，不能越落越陡。
 * 这里验证 {@link TorpedoAirDropBallistics#entryVelocity} 的角度解算，以及入水后
 * {@link TorpedoAirDropBallistics#cruiseVelocity} 不残留俯角。
 */
class TorpedoAirDropBallisticsTest {

    private static final double EPS = 1.0e-9;

    /** 速度矢量相对水平面的俯角（度），与 TorpedoRenderer 的 xRot 同一公式。 */
    private static double angleDegrees(Vec3 v) {
        double horizontal = Math.hypot(v.x, v.z);
        return Math.toDegrees(Math.atan2(-v.y, horizontal));
    }

    @Test
    void spawnVelocityMatchesConfiguredEntryAngle() {
        Vec3 direction = new Vec3(0.6, 0, 0.8);
        for (double speed : new double[] { 0.125, 0.14 }) {
            Vec3 v = TorpedoAirDropBallistics.entryVelocity(direction, speed, 45.0);
            assertEquals(45.0, angleDegrees(v), 1.0e-6, "speed=" + speed);
            // 入水角只改变垂直分量，水平航速仍是鱼雷航速
            assertEquals(speed, Math.hypot(v.x, v.z), EPS, "horizontal speed=" + speed);
            assertTrue(v.y < 0, "向下");
        }
    }

    @Test
    void angleStaysConstantThroughTheWholeDescent() {
        for (double angle : new double[] { 15.0, 45.0, 60.0, 89.0 }) {
            Vec3 direction = new Vec3(0, 0, 1);
            Vec3 v = TorpedoAirDropBallistics.entryVelocity(direction, 0.14, angle);
            // 模拟 tickAirDrop：每 tick 按当前水平分量与角度重算，不累加垂直加速度。
            for (int tick = 0; tick < 200; tick++) {
                double horizontal = Math.hypot(v.x, v.z);
                Vec3 currentDirection = new Vec3(v.x / horizontal, 0, v.z / horizontal);
                v = TorpedoAirDropBallistics.entryVelocity(currentDirection, horizontal, angle);
                assertEquals(angle, angleDegrees(v), EPS, "tick=" + tick + " angle=" + angle);
            }
        }
    }

    @Test
    void entryAngleIsClampedAndFinite() {
        // 90° 会让 tan 发散，解算必须夹到 89° 以内
        Vec3 v = TorpedoAirDropBallistics.entryVelocity(new Vec3(0, 0, 1), 0.14, 90.0);
        assertTrue(Double.isFinite(v.y));
        assertEquals(89.0, angleDegrees(v), 1.0e-6);
    }

    @Test
    void cruiseVelocityAfterEntryHasNoPitch() {
        Vec3 direction = new Vec3(0.6, 0, 0.8);
        Vec3 v = TorpedoAirDropBallistics.cruiseVelocity(direction, 0.14);
        assertEquals(0.0, v.y, 0.0, "入水后垂直速度必须归零");
        assertEquals(0.14, Math.hypot(v.x, v.z), EPS);
        assertEquals(0.0, angleDegrees(v), EPS);
    }
}
