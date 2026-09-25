package com.piranport.combat.cannon;

import com.piranport.combat.cannon.ammo.AmmoBehavior;
import com.piranport.combat.cannon.ammo.AmmoBehaviorResolver;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmmoBehaviorResolverTest {
    @Test
    void registeredItemDefinitionWinsOverLegacyFlags() {
        assertEquals(AmmoBehavior.MK23,
                AmmoBehaviorResolver.resolve(id("mk23_nuclear_shell"), false, false));
        assertEquals(AmmoBehavior.VT,
                AmmoBehaviorResolver.resolve(id("small_vt_shell"), true, false));
    }

    @Test
    void legacyFlagsRemainReadableForUnknownItems() {
        assertEquals(AmmoBehavior.HE, AmmoBehaviorResolver.fromSerialized("", true, false));
        assertEquals(AmmoBehavior.AP, AmmoBehaviorResolver.fromSerialized("old_value", false, false));
        assertEquals(AmmoBehavior.VT, AmmoBehaviorResolver.fromSerialized("", false, true));
    }

    @Test
    void behaviorPredicatesKeepExistingSemantics() {
        assertEquals(true, AmmoBehaviorResolver.isHighExplosive(AmmoBehavior.MK23));
        assertEquals(true, AmmoBehaviorResolver.isHighExplosive(AmmoBehavior.VT));
        assertEquals(false, AmmoBehaviorResolver.isHighExplosive(AmmoBehavior.AP));
        assertEquals(true, AmmoBehaviorResolver.isProximityFuse(AmmoBehavior.VT));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("piranport", path);
    }
}
