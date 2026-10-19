package com.piranport.npc.ai.goal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 《深海/01》§七 护航仇恨判定：无运输船、玩家攻击、玩家进范围任一成立都不仇恨运输船。 */
class EscortTransportTargetGoalTest {

    @Test
    void noTransportMeansNoTarget() {
        assertFalse(EscortTransportTargetGoal.shouldTargetTransport(false, false, false));
    }

    @Test
    void playerAttackEndsTransportAggro() {
        assertFalse(EscortTransportTargetGoal.shouldTargetTransport(true, true, false));
    }

    @Test
    void playerWithinRangeEndsTransportAggro() {
        assertFalse(EscortTransportTargetGoal.shouldTargetTransport(true, false, true));
    }

    @Test
    void transportTargetedWhenNoPlayerInterferes() {
        assertTrue(EscortTransportTargetGoal.shouldTargetTransport(true, false, false));
    }
}
