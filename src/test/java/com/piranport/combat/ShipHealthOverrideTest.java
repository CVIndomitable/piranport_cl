package com.piranport.combat;

import com.piranport.item.ShipType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ShipHealthOverrideTest {

    @Test
    void allCoreHealthLimitsFollowTheLatestCoreDecision() {
        assertEquals(20, ShipType.SMALL.maxHealth());
        assertEquals(30, ShipType.MEDIUM.maxHealth());
        assertEquals(40, ShipType.LARGE.maxHealth());
        assertEquals(12, ShipType.SUBMARINE.maxHealth());
    }

    @Test
    void preexistingHealthBoostIsSuppressedAndRestoredOnRemoval() {
        var boost = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "boost"),
                10, AttributeModifier.Operation.ADD_VALUE);
        double correction = ShipHealthOverride.computeCorrection(20, List.of(boost), 20);
        assertEquals(-10.0, correction, 1.0E-6);
    }

    @Test
    void mixedMultipliersAndChangingEffectsCannotRaiseTheCoreLimit() {
        var base = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "base"),
                0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        var total = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("test", "total"),
                1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        // base=20, additive=0, baseMultiplier=1.5, totalMultiplier=2.0, max=12
        // multiplier = 1.5 * 2.0 = 3.0
        // correction = 12 / 3.0 - 20 = -16.0
        double correction = ShipHealthOverride.computeCorrection(20, List.of(base, total), 12);
        assertEquals(-16.0, correction, 1.0E-6);
    }
}
