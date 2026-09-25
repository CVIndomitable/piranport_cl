package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.component.AircraftInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftCombatServiceTest {
    @AfterEach
    void restoreRegistry() { AttackStrategyRegistry.resetToBuiltIns(); }

    @Test
    void mapsEveryProfileToStableStrategyId() {
        for (AircraftDefinition.AttackProfile profile : AircraftDefinition.AttackProfile.values()) {
            AircraftDefinition definition = definition(profile);
            assertEquals(profile.id(), AircraftCombatService.strategyIdFor(definition));
            assertTrue(AircraftCombatService.supports(definition, profile.id().toUpperCase()));
        }
    }

    @Test
    void customStrategyCanReplaceStableId() {
        AttackStrategy custom = new AttackStrategy() {
            @Override public String id() { return "gun"; }
            @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.GUN; }
        };
        AttackStrategyRegistry.register(custom);
        assertSame(custom, AttackStrategyRegistry.byId(" GUN " ).orElseThrow());
        assertSame(custom, AircraftCombatService.strategyFor(definition(AircraftDefinition.AttackProfile.GUN)));
    }

    @Test
    void unknownStrategyIsRejectedWithoutChangingRuntimePath() {
        AircraftDefinition definition = definition(AircraftDefinition.AttackProfile.TORPEDO);
        assertFalse(AircraftCombatService.supports(definition, "missing"));
        assertThrows(IllegalArgumentException.class, () -> new AttackStrategy() {
            @Override public String id() { return "missing"; }
        }.profile());
    }

    private static AircraftDefinition definition(AircraftDefinition.AttackProfile profile) {
        AircraftInfo.AircraftType type = switch (profile) {
            case GUN -> AircraftInfo.AircraftType.FIGHTER;
            case ROCKET -> AircraftInfo.AircraftType.ROCKET_FIGHTER;
            case DIVE_BOMB -> AircraftInfo.AircraftType.DIVE_BOMBER;
            case LEVEL_BOMB -> AircraftInfo.AircraftType.LEVEL_BOMBER;
            case TORPEDO -> AircraftInfo.AircraftType.TORPEDO_BOMBER;
            case DEPTH_CHARGE -> AircraftInfo.AircraftType.ASW;
            case RECON, NONE -> AircraftInfo.AircraftType.RECON;
        };
        AircraftDefinition.PayloadType payload = switch (type) {
            case TORPEDO_BOMBER -> AircraftDefinition.PayloadType.AERIAL_TORPEDO;
            case DIVE_BOMBER, LEVEL_BOMBER -> AircraftDefinition.PayloadType.AERIAL_BOMB;
            case ASW -> AircraftDefinition.PayloadType.DEPTH_CHARGE;
            case ROCKET_FIGHTER -> AircraftDefinition.PayloadType.ROCKET_AMMO;
            default -> AircraftDefinition.PayloadType.NONE;
        };
        return new AircraftDefinition("test:" + profile.id(), type, profile, payload, "test_visual",
                100, 8, 4.0f, 1.0f, 1, AircraftInfo.BombingMode.DIVE);
    }
}
