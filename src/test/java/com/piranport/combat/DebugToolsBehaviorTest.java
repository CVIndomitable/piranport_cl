package com.piranport.combat;

import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DebugToolsBehaviorTest {
    @Test
    void cooldownsKeepTheirRequestedDuration() {
        UUID player = UUID.randomUUID();
        WeaponCooldown weapon = WeaponCooldown.of(player, 10, 240);
        assertEquals(250, weapon.endTick());
        assertEquals(240, weapon.totalTick());
        SlotCooldowns slots = SlotCooldowns.EMPTY.withSlotCooldown(player, 3, 240, 10);
        assertTrue(slots.isOnCooldown(3, 249));
        assertFalse(slots.isOnCooldown(3, 250));
    }

    @Test
    void hitNotificationsRemainIndependentPerPlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        try {
            assertTrue(HitNotifier.isEnabled(first));
            HitNotifier.setEnabled(first, false);
            assertFalse(HitNotifier.isEnabled(first));
            assertTrue(HitNotifier.isEnabled(second));
            HitNotifier.setEnabled(first, true);
            assertTrue(HitNotifier.isEnabled(first));
        } finally {
            HitNotifier.onPlayerLogout(first);
            HitNotifier.onPlayerLogout(second);
        }
    }
}
