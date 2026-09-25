package com.piranport.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AircraftVisualRegistryTest {
    @Test
    void knownVisualIdsSelectTheMatchingModelAndTexture() {
        AircraftVisualRegistry.AircraftVisual b25 = AircraftVisualRegistry.resolve("b25");
        AircraftVisualRegistry.AircraftVisual f4f = AircraftVisualRegistry.resolve("f4f");

        assertEquals(AircraftVisualRegistry.ModelKind.B25, b25.modelKind());
        assertEquals("piranport:textures/entity/b25.png", b25.texture().toString());
        assertEquals(AircraftVisualRegistry.ModelKind.F4F, f4f.modelKind());
        assertEquals("piranport:textures/entity/f4f.png", f4f.texture().toString());
    }

    @Test
    void legacyAndNamespacedAliasesRemainStable() {
        assertEquals(AircraftVisualRegistry.ModelKind.B25,
                AircraftVisualRegistry.resolve("level_bomber").modelKind());
        assertEquals(AircraftVisualRegistry.ModelKind.F4F,
                AircraftVisualRegistry.resolve("fighter").modelKind());
        assertEquals(AircraftVisualRegistry.ModelKind.B25,
                AircraftVisualRegistry.resolve("piranport:b25").modelKind());
    }

    @Test
    void everyDataDefinedVisualUsesAnExplicitPlaceholderOrModel() {
        String[] definedVisuals = {
                "fighter", "rocket_fighter", "dive_bomber", "level_bomber",
                "torpedo_bomber", "asw", "recon"
        };

        for (String visualId : definedVisuals) {
            assertEquals(true, AircraftVisualRegistry.isRegistered(visualId), visualId);
            assertEquals(false, AircraftVisualRegistry.resolve(visualId).id().isBlank(), visualId);
        }
    }

    @Test
    void unknownVisualIdsUseTheExplicitF4FFallback() {
        AircraftVisualRegistry.AircraftVisual fallback = AircraftVisualRegistry.resolve("swordfish");

        assertEquals(AircraftVisualRegistry.ModelKind.F4F, fallback.modelKind());
        assertEquals("f4f", fallback.id());
        assertEquals("piranport:textures/entity/f4f.png", fallback.texture().toString());
        assertEquals(AircraftVisualRegistry.ModelKind.F4F,
                AircraftVisualRegistry.resolve((String) null).modelKind());
    }
}
