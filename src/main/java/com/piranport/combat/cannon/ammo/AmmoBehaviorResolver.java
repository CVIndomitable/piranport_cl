package com.piranport.combat.cannon.ammo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Resolves a projectile behavior once at its creation or legacy NBT migration boundary. */
public final class AmmoBehaviorResolver {
    private AmmoBehaviorResolver() {}

    public static AmmoBehavior resolve(ItemStack stack, boolean legacyHighExplosive, boolean legacyProximityFuse) {
        if (legacyProximityFuse) return AmmoBehavior.VT;
        ResourceLocation itemId = stack == null || stack.isEmpty()
                ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        return resolve(itemId, legacyHighExplosive, legacyProximityFuse);
    }

    /** Pure registry-id entry point for reload tests and non-item data loaders. */
    public static AmmoBehavior resolve(ResourceLocation itemId, boolean legacyHighExplosive,
                                       boolean legacyProximityFuse) {
        if (legacyProximityFuse) return AmmoBehavior.VT;
        if (itemId != null) {
            var definition = AmmoDefinitionService.find(itemId);
            if (definition.isPresent()) return definition.get().behavior();
        }
        return legacyHighExplosive ? AmmoBehavior.HE : AmmoBehavior.AP;
    }

    public static AmmoBehavior fromSerialized(String value, boolean legacyHighExplosive,
                                               boolean legacyProximityFuse) {
        if (value != null && !value.isBlank()) {
            try {
                return AmmoBehavior.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the legacy booleans for old or unknown saves.
            }
        }
        return legacyProximityFuse ? AmmoBehavior.VT
                : (legacyHighExplosive ? AmmoBehavior.HE : AmmoBehavior.AP);
    }

    public static boolean isHighExplosive(AmmoBehavior behavior) {
        return behavior == AmmoBehavior.HE
                || behavior == AmmoBehavior.MK23
                || behavior == AmmoBehavior.VT;
    }

    public static boolean isProximityFuse(AmmoBehavior behavior) {
        return behavior == AmmoBehavior.VT;
    }
}
