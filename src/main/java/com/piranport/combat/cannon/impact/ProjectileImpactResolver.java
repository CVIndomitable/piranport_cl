package com.piranport.combat.cannon.impact;

import com.piranport.combat.cannon.ammo.AmmoBehavior;
import com.piranport.combat.cannon.ammo.AmmoBehaviorStrategy;

/** Stateless projectile impact rules. */
public final class ProjectileImpactResolver {
    private ProjectileImpactResolver() {}

    public static AmmoBehaviorStrategy.ImpactKind impactKind(AmmoBehavior behavior) {
        return AmmoBehaviorStrategy.require(behavior).impactKind();
    }

    public static ProjectileImpactResult resolve(AmmoBehavior behavior, float baseDamage,
                                                  float initialSpeed, float currentSpeed,
                                                  float apMultiplier, int sourceCaliber,
                                                  boolean smallShipTarget, float maxHealth,
                                                  float armorIgnore) {
        if (behavior != AmmoBehavior.AP) {
            return new ProjectileImpactResult(impactKind(behavior), sanitize(baseDamage), 0f);
        }
        float speedRatio = initialSpeed > 0f && Float.isFinite(initialSpeed)
                ? Math.max(0f, currentSpeed / initialSpeed) : 1f;
        if (!Float.isFinite(speedRatio)) speedRatio = 0f;
        float multiplier = Float.isFinite(apMultiplier) ? Math.max(0f, apMultiplier) : 1f;
        float damage = sanitize(baseDamage) * multiplier * speedRatio;
        if (smallShipTarget && sourceCaliber > 8) damage *= 0.05f;
        if (smallShipTarget && Float.isFinite(maxHealth) && maxHealth > 0f) {
            damage = Math.min(damage, maxHealth * 0.25f);
        }
        float clampedArmor = Float.isFinite(armorIgnore)
                ? Math.max(0f, Math.min(1f, armorIgnore)) : 0f;
        return new ProjectileImpactResult(AmmoBehaviorStrategy.ImpactKind.AP, damage, clampedArmor);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
