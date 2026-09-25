package com.piranport.combat.cannon.ammo;

import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/** Stable definition snapshot for one cannon ammunition item. */
public record AmmoDefinition(
        ResourceLocation itemId,
        AmmoBehavior behavior,
        Optional<CannonAmmoRules.CaliberFamily> caliberFamily,
        float damageMultiplier,
        float explosionMultiplier,
        float armorIgnore,
        boolean underwaterExplosion,
        Optional<AmmoBehaviorStrategy.ImpactKind> impactKindOverride) {

    public AmmoDefinition {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(caliberFamily, "caliberFamily");
        Objects.requireNonNull(impactKindOverride, "impactKindOverride");
        if (itemId.getPath().isEmpty()) throw new IllegalArgumentException("itemId path must not be empty");
        if (!Float.isFinite(damageMultiplier) || damageMultiplier < 0f) throw new IllegalArgumentException("damageMultiplier must be finite and non-negative");
        if (!Float.isFinite(explosionMultiplier) || explosionMultiplier < 0f) throw new IllegalArgumentException("explosionMultiplier must be finite and non-negative");
        if (!Float.isFinite(armorIgnore) || armorIgnore < 0f || armorIgnore > 1f) throw new IllegalArgumentException("armorIgnore must be between 0 and 1");
    }

    public AmmoDefinition(ResourceLocation itemId, AmmoBehavior behavior) {
        this(itemId, behavior, Optional.empty(), 1f, 1f, defaultArmorIgnore(behavior),
                AmmoBehaviorStrategy.require(behavior).explodesUnderwater(), Optional.empty());
    }

    public AmmoDefinition(ResourceLocation itemId, AmmoBehavior behavior,
                          CannonAmmoRules.CaliberFamily caliberFamily) {
        this(itemId, behavior, Optional.of(Objects.requireNonNull(caliberFamily, "caliberFamily")),
                1f, 1f, defaultArmorIgnore(behavior),
                AmmoBehaviorStrategy.require(behavior).explodesUnderwater(), Optional.empty());
    }

    public AmmoDefinition(ResourceLocation itemId, AmmoBehavior behavior,
                          CannonAmmoRules.CaliberFamily caliberFamily, float damageMultiplier,
                          float explosionMultiplier, float armorIgnore) {
        this(itemId, behavior, Optional.of(Objects.requireNonNull(caliberFamily, "caliberFamily")),
                damageMultiplier, explosionMultiplier, armorIgnore,
                AmmoBehaviorStrategy.require(behavior).explodesUnderwater(), Optional.empty());
    }

    private static float defaultArmorIgnore(AmmoBehavior behavior) {
        return behavior == AmmoBehavior.AP ? 0.5f : 0f;
    }

    public boolean isCompatibleWith(CannonAmmoRules.CaliberFamily cannonFamily) {
        Objects.requireNonNull(cannonFamily, "cannonFamily");
        return caliberFamily.isEmpty() || caliberFamily.get() == cannonFamily;
    }

    public boolean isHighExplosive() { return AmmoBehaviorStrategy.require(behavior).highExplosive(); }
    public boolean isArmorPiercing() { return AmmoBehaviorStrategy.require(behavior).isArmorPiercing(); }
    public boolean isProximityFuse() { return AmmoBehaviorStrategy.require(behavior).proximityFuse(); }
    public AmmoBehaviorStrategy.ImpactKind impactKind() {
        return impactKindOverride.orElseGet(() -> AmmoBehaviorStrategy.require(behavior).impactKind());
    }
}
