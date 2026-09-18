package com.piranport.combat.cannon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 延迟射击必须仍属于原来的存活玩家、世界和手持武器。 */
class SalvoContextTest {
    private final Object player = new Object();
    private final Object level = new Object();
    private final Object weapon = new Object();
    private final SalvoContext context = new SalvoContext(player, level, weapon);

    @Test
    void unchangedLivingPlayerCanContinue() {
        assertTrue(context.isValid(player, level, weapon, true, false));
    }

    @Test
    void changedDimensionCancelsQueuedShots() {
        assertFalse(context.isValid(player, new Object(), weapon, true, false));
    }

    @Test
    void respawnedPlayerCannotInheritTheOldQueue() {
        assertFalse(context.isValid(new Object(), level, weapon, true, false));
    }

    @Test
    void replacingOrSwitchingHeldWeaponCancelsQueuedShots() {
        assertFalse(context.isValid(player, level, new Object(), true, false));
    }

    @Test
    void deadOrSpectatorPlayersCannotFire() {
        assertFalse(context.isValid(player, level, weapon, false, false));
        assertFalse(context.isValid(player, level, weapon, true, true));
    }
}
