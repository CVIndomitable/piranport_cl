package com.piranport.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 水平轰炸机投弹提前量纯函数测试。
 *
 * <p>核心保证：{@link LevelBombLead#releaseDistance} 复刻
 * {@code ThrowableProjectile#tick} 的逐行积分顺序，返回值 = 航弹落地时的水平前进量。
 */
class LevelBombLeadTest {

    private static final double GRAVITY = 0.06;   // 与 AerialBombEntity.GRAVITY 一致
    private static final double VERTICAL = -0.3;  // 投弹垂直初速

    /** 典型高度（+32 投弹高度 - 0.5 出膛偏移）下与逐 tick 飞行模拟逐位对齐。 */
    @Test
    void matchesTickByTickFlightSimulation() {
        double dropHeight = 31.5;
        for (double hs : new double[]{0.0, 0.078, 0.2, 0.39, 0.42, 1.0, 2.5}) {
            double expected = flyBombAndMeasureForward(dropHeight, GRAVITY, hs, VERTICAL);
            assertEquals(expected, LevelBombLead.releaseDistance(dropHeight, GRAVITY, hs, VERTICAL), 1e-9,
                    "horizontalSpeed=" + hs);
        }
    }

    /** 逐 tick 独立参考实现：直接搬位置的炸弹会飞多远。 */
    private static double flyBombAndMeasureForward(double dropHeight, double gravity,
                                                   double horizontalSpeed, double verticalSpeed) {
        double y = 0.0;
        double x = 0.0;
        double vx = horizontalSpeed;
        double vy = verticalSpeed;
        for (int tick = 0; tick < 100_000 && y > -dropHeight; tick++) {
            x += vx;
            y += vy;
            vx *= 0.99;
            vy = vy * 0.99 - gravity;
        }
        return x;
    }

    /** 提前距离与水平初速严格成正比（垂直运动与水平解耦）。 */
    @Test
    void scalesLinearlyWithHorizontalSpeed() {
        double base = LevelBombLead.releaseDistance(31.5, GRAVITY, 0.42, VERTICAL);
        assertEquals(2.0 * base, LevelBombLead.releaseDistance(31.5, GRAVITY, 0.84, VERTICAL), 1e-9);
        assertEquals(0.5 * base, LevelBombLead.releaseDistance(31.5, GRAVITY, 0.21, VERTICAL), 1e-9);
    }

    /** 黄金值：31.5 格落距、0.42 格/tick 前抛水平初速下，提前约 10.93 格。 */
    @Test
    void goldenValueForTypicalLevelBomber() {
        assertEquals(10.93258431769222,
                LevelBombLead.releaseDistance(31.5, GRAVITY, 0.42, VERTICAL), 1e-9);
    }

    /** B-25 典型值：panelSpeed 1.3 × 0.6 = 0.78 载机速度，×0.5 前抛 = 0.39 水平初速。 */
    @Test
    void realisticB25LeadIsAboutTenBlocks() {
        double lead = LevelBombLead.releaseDistance(31.5, GRAVITY, 0.78 * 0.5, VERTICAL);
        assertTrue(lead > 10.0 && lead < 10.4, "lead=" + lead);
    }

    /** 退化输入：下落高度非正、重力非正、零水平速度、非有限值都返回 0，不得死循环。 */
    @Test
    void degenerateInputsReturnZero() {
        assertEquals(0.0, LevelBombLead.releaseDistance(0.0, GRAVITY, 1.0, VERTICAL));
        assertEquals(0.0, LevelBombLead.releaseDistance(-5.0, GRAVITY, 1.0, VERTICAL));
        assertEquals(0.0, LevelBombLead.releaseDistance(31.5, 0.0, 1.0, VERTICAL));
        assertEquals(0.0, LevelBombLead.releaseDistance(31.5, GRAVITY, 0.0, VERTICAL));
        assertEquals(0.0, LevelBombLead.releaseDistance(Double.NaN, GRAVITY, 1.0, VERTICAL));
        assertEquals(0.0, LevelBombLead.releaseDistance(31.5, GRAVITY, Double.POSITIVE_INFINITY, VERTICAL));
    }

    /** 恢复 -0.3 垂直初速会比 -0.1 落得略近（下落快、暴露在水平风中的时间短）。 */
    @Test
    void verticalSpeedMinus03FallsFasterThanMinus01() {
        double fast = LevelBombLead.releaseDistance(31.5, GRAVITY, 0.42, -0.3);
        double slow = LevelBombLead.releaseDistance(31.5, GRAVITY, 0.42, -0.1);
        assertTrue(fast < slow, "fast=" + fast + " slow=" + slow);
    }
}
