package com.piranport.artillery.config.override;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigOverrideManagerTest {

    @Test
    void cannonFloatOverridesRejectNonFiniteValues() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigOverrideManager.validateValue("damage", Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigOverrideManager.validateValue("initialSpeed", Float.POSITIVE_INFINITY));
    }

    @Test
    void projectileDoubleOverridesRejectNonFiniteValues() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigOverrideManager.validateProjectileValue(
                        "AP_DAMAGE_MULTIPLIER", Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigOverrideManager.validateProjectileValue(
                        "HE_ARMOR_PENETRATION", Double.NEGATIVE_INFINITY));
    }

    @Test
    void muzzleOverridesRejectNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigOverrideManager.validateValue("muzzles", "NaN:0:1.5"));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigOverrideManager.validateValue("muzzles", "0:Infinity:1.5"));
        assertEquals("0:0:1.5",
                ConfigOverrideManager.validateValue("muzzles", "0:0:1.5"));
    }
}
