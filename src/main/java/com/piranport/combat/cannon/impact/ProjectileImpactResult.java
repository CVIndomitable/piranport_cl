package com.piranport.combat.cannon.impact;

import com.piranport.combat.cannon.ammo.AmmoBehaviorStrategy;

/** Pure result of resolving a projectile impact before Minecraft side effects. */
public record ProjectileImpactResult(
        AmmoBehaviorStrategy.ImpactKind impactKind,
        float damage,
        float armorIgnore) {

    public ProjectileImpactResult {
        if (impactKind == null) throw new NullPointerException("impactKind");
        if (!Float.isFinite(damage) || damage < 0f) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
        if (!Float.isFinite(armorIgnore) || armorIgnore < 0f || armorIgnore > 1f) {
            throw new IllegalArgumentException("armorIgnore must be between 0 and 1");
        }
    }
}
