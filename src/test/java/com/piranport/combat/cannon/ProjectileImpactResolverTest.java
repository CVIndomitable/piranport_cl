package com.piranport.combat.cannon;

import com.piranport.combat.cannon.ammo.AmmoBehavior;
import com.piranport.combat.cannon.ammo.AmmoBehaviorStrategy;
import com.piranport.combat.cannon.impact.ProjectileImpactResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectileImpactResolverTest {
    @Test
    void apDamageIncludesSpeedDecayAndMultiplier() {
        var result = ProjectileImpactResolver.resolve(AmmoBehavior.AP, 10f, 4f, 2f, 1.3f, 8, false, 20f, .2f);
        assertEquals(6.5f, result.damage(), 1e-5f);
        assertEquals(.2f, result.armorIgnore(), 1e-5f);
        assertEquals(AmmoBehaviorStrategy.ImpactKind.AP, result.impactKind());
    }

    @Test
    void largeCaliberOverpenAndSmallShipCapAreAppliedInOrder() {
        var result = ProjectileImpactResolver.resolve(AmmoBehavior.AP, 100f, 1f, 1f, 1f, 16, true, 20f, .5f);
        assertEquals(5f, result.damage(), 1e-5f);
    }

    @Test
    void nonApUsesBehaviorImpactKindAndBaseDamage() {
        var result = ProjectileImpactResolver.resolve(AmmoBehavior.VT, 7f, 1f, 0f, 99f, 16, true, 20f, .4f);
        assertEquals(7f, result.damage(), 1e-5f);
        assertEquals(AmmoBehaviorStrategy.ImpactKind.VT, result.impactKind());
        assertEquals(0f, result.armorIgnore(), 1e-5f);
    }
}
