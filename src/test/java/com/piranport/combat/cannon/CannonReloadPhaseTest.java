package com.piranport.combat.cannon;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.piranport.combat.cannon.CannonReloadPhase.*;
import static org.junit.jupiter.api.Assertions.*;

/** 保护自动装填、到期结算和切弹边界。 */
class CannonReloadPhaseTest {
    @Test
    void legacyModeDoesNotControlReloadOrRKey() throws Exception {
        String reload = Files.readString(Path.of("src/main/java/com/piranport/combat/cannon/CannonReloading.java"));
        String rKey = Files.readString(Path.of("src/main/java/com/piranport/network/ManualReloadPayload.java"));
        assertFalse(reload.contains("isAutoLoading("));
        assertFalse(reload.contains("loadingMode("));
        assertFalse(rKey.contains("tryManualCannonReload"));
        assertTrue(rKey.contains("ReloadHelper.reloadTorpedoLauncher"));
    }
    @Test
    void acceptsLoadedResidueWithoutAllowingOverfilledMagazine() {
        assertTrue(CannonAmmoRules.validLoadedCount(1, 3));
        assertTrue(CannonAmmoRules.validLoadedCount(3, 3));
        assertFalse(CannonAmmoRules.validLoadedCount(0, 3));
        assertFalse(CannonAmmoRules.validLoadedCount(4, 3));
    }

    @Test
    void emptyCannonStartsWithoutInput() {
        assertEquals(START, resolve(false, null, 100));
    }

    @Test
    void cannonRestartsAfterItsBarrelIsFiredEmpty() {
        assertEquals(START, resolve(false, null, 201));
    }

    @Test
    void waitsUntilTheExactCompletionTick() {
        assertEquals(WAIT, resolve(false, 200L, 199));
        assertEquals(COMPLETE, resolve(false, 200L, 200));
        assertEquals(COMPLETE, resolve(false, 200L, 201));
    }

    @Test
    void loadedAmmoWinsOverAnyLeftoverTimer() {
        assertEquals(LOADED, resolve(true, 200L, 199));
        assertEquals(LOADED, resolve(true, 200L, 201));
    }

    @Test
    void emptyCooldownMarkerNeverCompletesAFreeReload() {
        assertEquals(START, resolve(false, 0L, 100));
    }

    @Test
    void movingSlotsDoesNotChangeItemTimerDecision() {
        // 装填判定没有槽位计时参数：换到空槽仍等待同一武器的到期时间。
        assertEquals(WAIT, resolve(false, 240L, 150));
        assertEquals(COMPLETE, resolve(false, 240L, 240));
    }

    @Test
    void selectionChangeRestartsOnlyAnUnfinishedReload() {
        assertTrue(shouldRestartAfterSelection(false, 200L));
        assertFalse(shouldRestartAfterSelection(true, 200L));
        assertFalse(shouldRestartAfterSelection(false, null));
        assertFalse(shouldRestartAfterSelection(false, 0L));
    }
}
