package com.piranport.artillery.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CannonDefinitionTest {
    private static ArtilleryCannonData data(int barrels, List<MuzzlePos> muzzles) {
        return new ArtilleryCannonData(8, barrels, 12f, 50, 1000, 3f, muzzles,
                3f, 0.01f, 9.8f, 1.5f, 1f, 15, barrels, 0f,
                200f, 0.8f, 0.8f, 50f, -5f, 3f, "MANUAL");
    }

    @Test
    void validDefinitionRequiresOneMuzzlePerBarrel() {
        CannonDefinition definition = new CannonDefinition(
                ResourceLocation.fromNamespaceAndPath("piranport", "test_cannon"),
                data(2, List.of(new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0))));

        assertTrue(definition.isValid());
        assertTrue(definition.validationErrors().isEmpty());
    }

    @Test
    void invalidMuzzleLayoutIsRejected() {
        CannonDefinition definition = new CannonDefinition(
                ResourceLocation.fromNamespaceAndPath("piranport", "broken_cannon"),
                data(2, List.of(new MuzzlePos(0, 0, 0))));

        assertFalse(definition.isValid());
        assertTrue(definition.validationErrors().stream()
                .anyMatch(error -> error.contains("muzzles count")));
    }
}
