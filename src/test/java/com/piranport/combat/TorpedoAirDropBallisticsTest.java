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

    /** 空投段只在脚部方块真正是水时结束；固体/超时是兜底，空中继续下落。 */
    @Test
    void airDropEndsOnlyOnRealWaterEntryOrFallbacks() {
        assertTrue(TorpedoAirDropBallistics.shouldEndAirDrop(true, false, 1),
                "脚部真正入水 → 结束空投");
        assertTrue(TorpedoAirDropBallistics.shouldEndAirDrop(false, true, 1),
                "投到陆地/建筑（脚部方块固体）→ 兜底结束");
        assertFalse(TorpedoAirDropBallistics.shouldEndAirDrop(
                        false, false, TorpedoAirDropBallistics.MAX_AIR_DROP_TICKS),
                "未到超时阈值 → 继续 45° 下落");
        assertTrue(TorpedoAirDropBallistics.shouldEndAirDrop(
                        false, false, TorpedoAirDropBallistics.MAX_AIR_DROP_TICKS + 1),
                "超过最长空投 tick → 兜底结束");
        assertFalse(TorpedoAirDropBallistics.shouldEndAirDrop(false, false, 8),
                "水面正上方仍在 45° 下落，不得提前结束");
    }

    /**
     * 从出生到脚部真正进入水方块的整段下落（逐 tick 应用固定角度速度），
     * 最后一步（扎进水里的那一步）仍是 45°，且横向前进约 2 格。
     */
    @Test
    void lastStepIntoWaterIsStillAtEntryAngle() {
        double waterBlockBottom = 0.0;
        double surfaceTop = waterBlockBottom + 0.875;
        // run_altitude 3 − drop_height_offset 1 = 水面上方 2 格
        double spawnY = surfaceTop + 2.0;
        Vec3 direction = new Vec3(0, 0, 1);
        for (double speed : new double[] { 0.125, 0.14 }) {
            Vec3 v = TorpedoAirDropBallistics.entryVelocity(direction, speed, 45.0);
            double y = spawnY;
            double horizontal = 0.0;
            int ticks = 0;
            Vec3 stepVelocity = v;
            // 脚部方块 = floor(y)；进入值为 0 的水方块即真正入水（与 tickAirDrop 判定一致）
            while (Math.floor(y) > waterBlockBottom && ticks < 200) {
                stepVelocity = TorpedoAirDropBallistics.entryVelocity(direction, speed, 45.0);
                y += stepVelocity.y;
                horizontal += Math.hypot(stepVelocity.x, stepVelocity.z);
                ticks++;
            }
            assertTrue(ticks < 200, "必须在兜底超时前入水");
            assertEquals(45.0, angleDegrees(stepVelocity), 1.0e-6,
                    "入水那一步的角度，speed=" + speed);
            assertEquals(2.0, horizontal, 0.1,
                    "出生高度 2 格 + 45° → 入水前横向前进约 2 格，speed=" + speed);
        }
    }

    /** 实体侧必须把结束判定接到纯函数上，且不能再保留“脚下方块是水就提前结束”。 */
    @Test
    void torpedoEntityUsesThePureEndCondition() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/piranport/entity/TorpedoEntity.java"));
        assertTrue(source.contains("shouldEndAirDrop("));
        assertFalse(source.contains("waterBelow"), "旧的提前结束条件必须删除");
    }
}
