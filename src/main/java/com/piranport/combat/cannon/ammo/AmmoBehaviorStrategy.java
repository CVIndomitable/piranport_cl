package com.piranport.combat.cannon.ammo;

import java.util.Objects;

/** Runtime policy for one projectile behavior; implementations are stateless. */
public interface AmmoBehaviorStrategy {
    AmmoBehavior behavior();

    boolean highExplosive();

    boolean proximityFuse();

    boolean explodesUnderwater();

    ImpactKind impactKind();

    default boolean isArmorPiercing() {
        return behavior() == AmmoBehavior.AP;
    }

    enum ImpactKind {
        HE,
        AP,
        VT
    }

    static AmmoBehaviorStrategy require(AmmoBehavior behavior) {
        return AmmoBehaviorRegistry.forBehavior(Objects.requireNonNull(behavior, "behavior"));
    }
}
