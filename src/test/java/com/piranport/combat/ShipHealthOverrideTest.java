package com.piranport.combat;

import com.piranport.item.ShipType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipHealthOverrideTest {
    private static AttributeInstance health() {
        return new AttributeInstance(Holder.direct(new RangedAttribute("test.health", 20, 1, 1024)), unused -> {});
    }

    @Test
    void allCoreHealthLimitsFollowTheLatestCoreDecision() {
        assertEquals(20, ShipType.SMALL.maxHealth());
        assertEquals(30, ShipType.MEDIUM.maxHealth());
        assertEquals(40, ShipType.LARGE.maxHealth());
        assertEquals(12, ShipType.SUBMARINE.maxHealth());
    }

    @Test
    void preexistingHealthBoostIsSuppressedAndRestoredOnRemoval() {
        AttributeInstance health = health();
        var boost = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "boost"),
                10, AttributeModifier.Operation.ADD_VALUE);
        health.addTransientModifier(boost);
        ShipHealthOverride.apply(health, 20);
        assertEquals(20, health.getValue(), 0.00001);
        health.removeModifier(TransformationManager.HEALTH_MODIFIER_ID);
        assertEquals(30, health.getValue(), 0.00001);
        assertTrue(health.hasModifier(boost.id()));
    }

    @Test
    void mixedMultipliersAndChangingEffectsCannotRaiseTheCoreLimit() {
        AttributeInstance health = health();
        var base = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "base"),
                0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        var total = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "total"),
                1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        health.addTransientModifier(base);
        health.addTransientModifier(total);
        ShipHealthOverride.apply(health, 12);
        assertEquals(12, health.getValue(), 0.00001);
        health.removeModifier(base.id());
        ShipHealthOverride.apply(health, 12);
        assertEquals(12, health.getValue(), 0.00001);
        health.removeModifier(TransformationManager.HEALTH_MODIFIER_ID);
        assertEquals(40, health.getValue(), 0.00001);
    }
}
