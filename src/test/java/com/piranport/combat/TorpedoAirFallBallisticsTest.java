package com.piranport.combat;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 非空投鱼雷空中段弹道测试。
 *
 * <p>用户口径（2026-10-28）：手持鱼雷发射器打出的鱼雷要「平着飞出去再落水」。
 * 旧默认每 tick 水平 ×0.70、垂直 +0.25，三四 tick 就吃光水平速度，入水角 82°~90°。
 * 这里验证新默认（不衰减 + 原版重力 0.03 + 下坠限速 0.3）下，从玩家眼高平射时
 * 鱼雷有数格水平位移、入水角明显变浅。
 */
class TorpedoAirFallBallisticsTest {

    private static final double EPS = 1.0e-9;

    // ---- 贴水面巡航的物理量（与 TorpedoEntity / ModEquipmentConfig 的当前默认一致）----
    /** 原版静止水面顶面相对水方块底边的高度。 */
    private static final double SURFACE_IN_BLOCK = 0.875;
    /** global.torpedo.surface_depth。 */
    private static final double SURFACE_DEPTH = 0.5;
    /** torpedo_entity 碰撞箱高度。 */
    private static final double BB_HEIGHT = 0.75;
    /** global.torpedo.surface_capture_range。 */
    private static final double CAPTURE = 1.0;
    /** global.torpedo.surface_vertical_adjust。 */
    private static final double VERTICAL_ADJUST = 0.3;
    /** global.torpedo.surface_vertical_max_speed。 */
    private static final double SURFACE_MAX_VY = 0.3;
    /** 玩家鱼雷航速（格/tick）。 */
    private static final double SPEED = 0.3;

    private static final double WATER_TOP_Y = 62.0;
    private static final double WATER_SURFACE = WATER_TOP_Y + SURFACE_IN_BLOCK;
    /** 贴水面目标脚部 Y（水面顶面 − 目标深度 − 半高）。 */
    private static final double TARGET_FEET = WATER_SURFACE - SURFACE_DEPTH - BB_HEIGHT * 0.5;

    /** 一次「发射 → 雷体底部入水」的模拟结果。 */
    private record Flight(int ticks, double horizontalBlocks, double entryAngleDegrees, double entryVy) {}

    /**
     * 逐 tick 复刻 {@code TorpedoEntity.tick()} 的顺序：先由 {@code ThrowableProjectile} 移动
     * 并把整条速度乘 0.99（空中阻力），再跑 {@code tickSurfaceAI}（捕获窗口内贴水面、
     * 窗口外走空中回退）。返回雷体底部穿过水面那一 tick 的状态。
     *
     * @param launchHeight 出生点相对贴水面目标脚部的高度（格）
     */
    private static Flight flight(double launchHeight, double decay, double accel, double terminal) {
        double y = TARGET_FEET + launchHeight;
        double x = 0.0;
        double vy = 0.0;
        double vx = SPEED;
        for (int tick = 1; tick <= 2000; tick++) {
            y += vy;
            x += vx;
            vy *= 0.99;
            vx *= 0.99;
            if (y <= WATER_SURFACE) {
                double angle = Math.toDegrees(Math.atan2(-vy, Math.abs(vx)));
                return new Flight(tick, x, angle, vy);
            }
            double err = TARGET_FEET - y;
            if (Math.abs(err) <= CAPTURE) {
                vy = Math.max(-SURFACE_MAX_VY, Math.min(SURFACE_MAX_VY, err * VERTICAL_ADJUST));
                vx = Math.copySign(SPEED, vx == 0 ? 1 : vx);
            } else if (err < 0) {
                Vec3 next = TorpedoAirFallBallistics.nextMotion(new Vec3(vx, vy, 0), decay, accel, terminal);
                vx = next.x;
                vy = next.y;
            }
        }
        fail("2000 tick 内必须入水");
        return null;
    }

    /** 站水面高度平射：出生点约在目标深度上方 2.32 格 → 平飞数格后以约 45° 入水。 */
    @Test
    void handheldLaunchFromWaterLevelFliesFlatThenSplashesDownShallowly() {
        Flight flight = flight(2.32, 1.0, 0.03, 0.3);
        assertTrue(flight.horizontalBlocks() >= 3.0,
                "水平位移应达数格，实际 " + flight.horizontalBlocks());
        assertTrue(flight.entryAngleDegrees() <= 50.0,
                "入水角应接近 45°，实际 " + flight.entryAngleDegrees());
    }

    /** 站 2 格高甲板平射：水平位移更多，入水角依然平缓。 */
    @Test
    void handheldLaunchFromADeckStillEntersAtAShallowAngle() {
        Flight flight = flight(4.32, 1.0, 0.03, 0.3);
        assertTrue(flight.horizontalBlocks() >= 4.5,
                "水平位移实际 " + flight.horizontalBlocks());
        assertTrue(flight.entryAngleDegrees() <= 50.0,
                "入水角实际 " + flight.entryAngleDegrees());
    }

    /** 旧默认（0.70 / 0.25）必须复现「垂直落水」，说明这次改的是根因而不是观感微调。 */
    @Test
    void oldDefaultsReproduceTheVerticalSplashdown() {
        for (double height : new double[] { 2.32, 4.32, 12.32 }) {
            // 旧实现没有下坠上限，用一个足够大的 terminal 还原
            Flight flight = flight(height, 0.70, 0.25, 99.0);
            assertTrue(flight.horizontalBlocks() < 1.0,
                    "旧参数水平位移应不足 1 格，实际 " + flight.horizontalBlocks());
            assertTrue(flight.entryAngleDegrees() > 80.0,
                    "旧参数应近似垂直入水，实际 " + flight.entryAngleDegrees());
        }
    }

    /** 再高的发射点也靠下坠限速压住入水角；打到陆地时同样不会再垂直下坠。 */
    @Test
    void terminalSpeedKeepsTallLaunchesFromGoingVertical() {
        for (double height : new double[] { 6.32, 8.32, 12.32 }) {
            Flight flight = flight(height, 1.0, 0.03, 0.3);
            assertTrue(flight.entryAngleDegrees() < 65.0,
                    "height=" + height + " 入水角 " + flight.entryAngleDegrees());
            assertTrue(flight.horizontalBlocks() > 5.0,
                    "height=" + height + " 水平位移 " + flight.horizontalBlocks());
        }
    }

    @Test
    void verticalSpeedAcceleratesDownwardButStopsAtTheCap() {
        assertEquals(-0.03, TorpedoAirFallBallistics.nextVerticalSpeed(0.0, 0.03, 0.3), EPS);
        assertEquals(-0.3, TorpedoAirFallBallistics.nextVerticalSpeed(-0.29, 0.03, 0.3), EPS);
        assertEquals(-0.3, TorpedoAirFallBallistics.nextVerticalSpeed(-1.2, 0.03, 0.3), EPS);
        // 向上（例如被爆炸抛起）只吃重力，不被限速
        assertEquals(0.17, TorpedoAirFallBallistics.nextVerticalSpeed(0.2, 0.03, 0.3), EPS);
    }

    @Test
    void horizontalSpeedIsKeptUnlessDecayIsConfigured() {
        Vec3 level = TorpedoAirFallBallistics.nextMotion(new Vec3(0.18, -0.12, 0.24), 1.0, 0.03, 0.3);
        assertEquals(0.18, level.x, EPS);
        assertEquals(0.24, level.z, EPS);
        assertEquals(-0.15, level.y, EPS);

        // 衰减参数仍然有效：调小即恢复「空中越飞越慢」的手感
        Vec3 decayed = TorpedoAirFallBallistics.nextMotion(new Vec3(0.3, 0.0, 0.0), 0.5, 0.03, 0.3);
        assertEquals(0.15, decayed.x, EPS);
        assertEquals(-0.03, decayed.y, EPS);
    }
}
