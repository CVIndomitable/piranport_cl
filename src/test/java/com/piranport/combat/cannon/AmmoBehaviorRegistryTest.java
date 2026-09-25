package com.piranport.combat.cannon;

import com.piranport.combat.cannon.ammo.AmmoBehavior;
import com.piranport.combat.cannon.ammo.AmmoBehaviorRegistry;
import com.piranport.combat.cannon.ammo.AmmoBehaviorStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmmoBehaviorRegistryTest {
    @AfterEach
    void resetRegistry() {
        AmmoBehaviorRegistry.resetToBuiltIns();
    }

    @Test
    void builtInsEncodeExistingProjectileSemantics() {
        var he = AmmoBehaviorRegistry.forBehavior(AmmoBehavior.HE);
        var ap = AmmoBehaviorRegistry.forBehavior(AmmoBehavior.AP);
        var vt = AmmoBehaviorRegistry.forBehavior(AmmoBehavior.VT);
        assertTrue(he.highExplosive());
        assertTrue(he.explodesUnderwater());
        assertEquals(AmmoBehaviorStrategy.ImpactKind.AP, ap.impactKind());
        assertTrue(vt.proximityFuse());
        assertEquals(AmmoBehaviorStrategy.ImpactKind.VT, vt.impactKind());
    }

    @Test
    void registeredStrategyReplacesOnlyItsBehavior() {
        AmmoBehaviorRegistry.register(new AmmoBehaviorStrategy() {
            @Override public AmmoBehavior behavior() { return AmmoBehavior.AP; }
            @Override public boolean highExplosive() { return true; }
            @Override public boolean proximityFuse() { return false; }
            @Override public boolean explodesUnderwater() { return true; }
            @Override public ImpactKind impactKind() { return ImpactKind.HE; }
        });
        assertTrue(AmmoBehaviorRegistry.forBehavior(AmmoBehavior.AP).highExplosive());
        assertTrue(AmmoBehaviorRegistry.forBehavior(AmmoBehavior.HE).highExplosive());
    }
}
