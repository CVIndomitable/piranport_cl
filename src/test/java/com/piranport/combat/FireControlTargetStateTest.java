package com.piranport.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 火控目标存活判定：死亡下沉中的实体仍在世界里，不能被继续当作目标。
 */
class FireControlTargetStateTest {

    @Test
    void healthyEntityIsTrackable() {
        assertTrue(FireControlTargetState.trackable(true, false, true, false));
    }

    @Test
    void missingOrRemovedEntityIsLost() {
        assertFalse(FireControlTargetState.trackable(false, false, true, false));
        assertFalse(FireControlTargetState.trackable(true, true, true, false));
        assertTrue(FireControlTargetState.lost(null));
    }

    @Test
    void deadOrDyingEntityIsLostEvenIfAliveFlagLags() {
        // 直接调用 die() / 脚本置死的路径里，「死亡」标志与血量归零不一定同时到；
        // 只要任一条件表明已死，就必须停止显示与追踪。
        assertFalse(FireControlTargetState.trackable(true, false, true, true));
        assertFalse(FireControlTargetState.trackable(true, false, false, false));
    }
}
