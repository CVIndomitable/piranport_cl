package com.piranport.npc.ai.goal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 《深海/02》锁定航向：提前点 = 目标位置 + 目标水平速度 × (水平距离 ÷ 鱼雷速度 + 40 tick)。 */
class TorpedoAttackGoalTest {

    private static final double EPS = 1e-9;

    @Test
    void stationaryTargetGivesDirectHeading() {
        double angle = TorpedoAttackGoal.leadAngle(0, 0, 10, 0, 0, 0, 0.3, 40);
        assertEquals(0.0, angle, EPS);
    }

    @Test
    void targetMovingAlongLineOfFireKeepsDirectHeading() {
        double angle = TorpedoAttackGoal.leadAngle(0, 0, 10, 0, 0.2, 0, 0.3, 40);
        assertEquals(0.0, angle, EPS);
    }

    @Test
    void crossingTargetLeadsAheadOfItsPosition() {
        double flightTicks = 10 / 0.3 + 40;
        double angle = TorpedoAttackGoal.leadAngle(0, 0, 10, 0, 0, 0.2, 0.3, 40);
        assertEquals(Math.atan2(0.2 * flightTicks, 10), angle, EPS);
    }
}
