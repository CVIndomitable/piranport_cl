package com.piranport.aviation;

import com.piranport.component.AircraftInfo;
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
    void resolvesDefinitionValuesAndLegacyConfigValues() {
        AircraftDefinition definition = new AircraftDefinition(
                "piranport:aircraft/test_stats", AircraftInfo.AircraftType.FIGHTER,
                AircraftDefinition.AttackProfile.GUN, AircraftDefinition.PayloadType.NONE,
                "test", 900, 7, 11.5F, 1.25F, 19, AircraftInfo.BombingMode.DIVE);

        ResolvedAircraftStats stats = AircraftStatsService.resolve(definition);
        assertEquals(definition.id(), stats.definitionId());
        assertEquals(11.5F, stats.damage());
        assertEquals(1.25F, stats.speed());
        assertEquals(900, stats.fuelCapacity());
        assertEquals(7, stats.ammoCapacity());
        assertEquals(19, stats.weight());
        assertEquals(20, stats.health());
        assertEquals(40, stats.cooldown());
    }

    @Test
    void sameDefinitionUsesOneImmutableSnapshot() {
        AircraftInfo info = new AircraftInfo(AircraftInfo.AircraftType.RECON,
                400, 0, 100, 2.0F, 1.0F, 4, AircraftInfo.BombingMode.LEVEL, true);
        ResolvedAircraftStats first = AircraftStatsService.resolve(info);
        ResolvedAircraftStats second = AircraftStatsService.resolve(AircraftDefinitionService.resolve(info));
        assertSame(first, second);
        assertThrows(UnsupportedOperationException.class, () -> AircraftStatsService.snapshot().clear());
    }

    @Test
    void invalidSnapshotValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new ResolvedAircraftStats("piranport:aircraft/bad", 1, 1, 0, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new ResolvedAircraftStats("piranport:aircraft/bad", 1, 0, 1, 1, 1, 0, 0));
    }
}
