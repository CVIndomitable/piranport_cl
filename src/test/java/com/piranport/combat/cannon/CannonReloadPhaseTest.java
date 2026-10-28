package com.piranport.combat.cannon;

import org.junit.jupiter.api.Test;

import static com.piranport.combat.cannon.CannonReloadPhase.*;
import static org.junit.jupiter.api.Assertions.*;

/** 保护自动/手动交互、到期扣弹和切弹边界。 */
class CannonReloadPhaseTest {
    @Test
    void acceptsLoadedResidueWithoutAllowingOverfilledMagazine() {
        assertTrue(CannonAmmoRules.validLoadedCount(1, 3));
        assertTrue(CannonAmmoRules.validLoadedCount(3, 3));
        assertFalse(CannonAmmoRules.validLoadedCount(0, 3));
        assertFalse(CannonAmmoRules.validLoadedCount(4, 3));
    }

    @Test
    void emptyAutomaticCannonStartsButManualWaitsForInput() {
        // 只有数据声明 AUTO 的炮才会在空膛时得到 START；手动炮的空膛是稳定状态。
        assertEquals(START, resolve(false, true, null, 100));
        assertEquals(IDLE, resolve(false, false, null, 100));
    }

    @Test
    void automaticCannonRestartsAfterItsBarrelIsFiredEmpty() {
        // 开火打空后武器上不残留读条计时：下一 tick 自动炮重新 START，手动炮回到等 R 的 IDLE。
        assertEquals(START, resolve(false, true, null, 201));
        assertEquals(IDLE, resolve(false, false, null, 201));
    }

    @Test
    void bothModesWaitUntilTheExactCompletionTick() {
        for (boolean automatic : new boolean[]{false, true}) {
            assertEquals(WAIT, resolve(false, automatic, 200L, 199));
            assertEquals(COMPLETE, resolve(false, automatic, 200L, 200));
            assertEquals(COMPLETE, resolve(false, automatic, 200L, 201));
        }
    }

    @Test
    void loadedAmmoWinsOverAnyLeftoverTimer() {
        assertEquals(LOADED, resolve(true, true, 200L, 199));
        assertEquals(LOADED, resolve(true, false, 200L, 201));
    }

    @Test
    void emptyCooldownMarkerNeverCompletesAFreeReload() {
        // endTick=0 视为没有读条：自动炮会当作空膛重新 START，手动炮仍 IDLE，都不会直接 COMPLETE。
        assertEquals(START, resolve(false, true, 0L, 100));
        assertEquals(IDLE, resolve(false, false, 0L, 100));
    }

    @Test
    void movingSlotsDoesNotChangeItemTimerDecision() {
        // 装填判定没有槽位计时参数：换到空槽仍等待同一武器的到期时间。
        assertEquals(WAIT, resolve(false, false, 240L, 150));
        assertEquals(COMPLETE, resolve(false, false, 240L, 240));
    }

    @Test
    void selectionChangeRestartsOnlyAnUnfinishedReload() {
        assertTrue(shouldRestartAfterSelection(false, 200L));
        assertFalse(shouldRestartAfterSelection(true, 200L));
        assertFalse(shouldRestartAfterSelection(false, null));
        assertFalse(shouldRestartAfterSelection(false, 0L));
    }
}
