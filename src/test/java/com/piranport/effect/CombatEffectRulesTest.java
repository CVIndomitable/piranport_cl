package com.piranport.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatEffectRulesTest {
    @Test
    void evasionHasThreeLevelsAndNeverExceedsThirtyPercent() {
        assertEquals(0.1f, CombatEffectRules.evasionChance(0), 0.00001f);
        assertEquals(0.2f, CombatEffectRules.evasionChance(1), 0.00001f);
        assertEquals(0.3f, CombatEffectRules.evasionChance(2), 0.00001f);
        assertEquals(0.3f, CombatEffectRules.evasionChance(255), 0.00001f);
    }

    @Test
    void experienceAndReloadRemainAtTheirThirdLevelCaps() {
        for (int amplifier = 0; amplifier < 3; amplifier++) {
            assertEquals(1.2 + amplifier * 0.2, CombatEffectRules.experienceMultiplier(amplifier), 0.00001);
            assertEquals(0.9 - amplifier * 0.1, CombatEffectRules.reloadMultiplier(amplifier), 0.00001);
        }
        assertEquals(1.6, CombatEffectRules.experienceMultiplier(255), 0.00001);
        assertEquals(0.7, CombatEffectRules.reloadMultiplier(255), 0.00001);
    }

    @Test
    void burningDealsDamageEveryThreeSecondsDividedByLevel() {
        assertEquals(60, CombatEffectRules.burningInterval(0));
        assertEquals(30, CombatEffectRules.burningInterval(1));
        assertEquals(20, CombatEffectRules.burningInterval(2));
        assertEquals(15, CombatEffectRules.burningInterval(3));
        assertEquals(15, CombatEffectRules.burningInterval(255));
    }
}
