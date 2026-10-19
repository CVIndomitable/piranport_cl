package com.piranport.aviation;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftStatsServiceTest {
    @AfterEach
    void clear() {
        AircraftStatsService.clear();
        AircraftDefinitionService.clear();
    }

    @Test
    void resolvesDefinitionValues() {
        AircraftDefinition definition = new AircraftDefinition(
                ResourceLocation.parse("piranport:test_stats"), AircraftClass.FIGHTER,
                AircraftDefinition.AttackProfile.GUN, null, "fighter",
                900, 7, 11.5F, 1.25F, 19, 20, 5, null);

        ResolvedAircraftStats stats = AircraftStatsService.resolve(definition);
        assertEquals(definition.id(), stats.definitionId());
        assertEquals(11.5F, stats.damage());
        assertEquals(1.25F, stats.speed());
        assertEquals(900, stats.fuelCapacity());
        assertEquals(7, stats.ammoCapacity());
        assertEquals(19, stats.weight());
        assertEquals(20, stats.health());
        assertEquals(5, stats.cooldown());
    }

    @Test
    void sameDefinitionUsesOneImmutableSnapshot() {
        AircraftDefinition definition = new AircraftDefinition(
                ResourceLocation.parse("piranport:test_recon"), AircraftClass.RECON,
                null, null, "recon",
                400, 0, 2.0F, 1.0F, 4, 10, 1, null);
        ResolvedAircraftStats first = AircraftStatsService.resolve(definition);
        ResolvedAircraftStats second = AircraftStatsService.resolve(definition);
        assertSame(first, second);
        assertThrows(UnsupportedOperationException.class, () -> AircraftStatsService.snapshot().clear());
    }

    @Test
    void invalidSnapshotValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new ResolvedAircraftStats(ResourceLocation.parse("piranport:bad"), 1, 1, 0, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new ResolvedAircraftStats(ResourceLocation.parse("piranport:bad"), 1, 0, 1, 1, 1, 0, 0));
    }
}
