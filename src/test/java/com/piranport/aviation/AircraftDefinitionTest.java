package com.piranport.aviation;

import com.piranport.component.AircraftInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;

class AircraftDefinitionTest {
    @AfterEach
    void clearDefinitions() {
        AircraftDefinitionService.clear();
    }

    @Test
    void legacyInfoGetsStableDefinitionAndPayload() {
        AircraftInfo info = new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                1200, 1, 0, 18.0F, 1.0F, 18,
                AircraftInfo.BombingMode.DIVE, false);

        assertEquals("piranport:aircraft/torpedo_bomber", info.definitionId());
        AircraftDefinition definition = AircraftDefinitionService.resolve(info);
        assertEquals(AircraftDefinition.AttackProfile.TORPEDO, definition.attackProfile());
        assertEquals("piranport:aerial_torpedo", definition.payloadRegistryName());
        assertSame(definition, AircraftDefinitionService.resolve(info));
    }

    @Test
    void stableStreamRoundTripsDefinitionId() {
        AircraftInfo original = new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                1200, 64, 37, 3.0F, 1.8F, 12,
                AircraftInfo.BombingMode.DIVE, true, "piranport:aircraft/f4f_custom");
        ByteBuf buf = Unpooled.buffer();
        AircraftInfo.STREAM_CODEC.encode(buf, original);
        AircraftInfo decoded = AircraftInfo.STREAM_CODEC.decode(buf);

        assertEquals(original.definitionId(), decoded.definitionId());
        assertEquals(original.aircraftType(), decoded.aircraftType());
        assertEquals(original.currentFuel(), decoded.currentFuel());
        assertEquals(original.payloadLoaded(), decoded.payloadLoaded());
    }

    @Test
    void invalidClassProfileCombinationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftDefinition(
                "piranport:aircraft/invalid",
                AircraftInfo.AircraftType.RECON,
                AircraftDefinition.AttackProfile.LEVEL_BOMB,
                AircraftDefinition.PayloadType.AERIAL_BOMB,
                "recon", 100, 1, 1.0F, 1.0F, 1,
                AircraftInfo.BombingMode.DIVE));
    }

    @Test
    void sameAircraftClassDoesNotShareDefinitionAcrossItemIds() {
        AircraftInfo first = new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                1200, 64, 0, 2.0F, 1.6F, 12,
                AircraftInfo.BombingMode.DIVE, true);
        AircraftInfo second = new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                1200, 64, 0, 5.0F, 2.4F, 14,
                AircraftInfo.BombingMode.DIVE, true);

        AircraftDefinition firstDefinition = AircraftDefinitionService.resolve(first, "piranport:seafire");
        AircraftDefinition secondDefinition = AircraftDefinitionService.resolve(second, "piranport:f2h_banshee");

        assertNotSame(firstDefinition, secondDefinition);
        assertEquals(2.0F, firstDefinition.panelDamage());
        assertEquals(5.0F, secondDefinition.panelDamage());
    }

    @Test
    void jsonShapeBuildsDefinitionWithStableResourceId() {
        String json = "{\"aircraft_class\":\"torpedo_bomber\",\"attack_profile\":\"torpedo\","
                + "\"payload_type\":\"aerial_torpedo\",\"visual_id\":\"swordfish\","
                + "\"fuel_capacity\":1200,\"ammo_capacity\":1,\"panel_damage\":18.0,"
                + "\"panel_speed\":1.0,\"weight\":18,\"bombing_mode\":\"dive\"}";
        AircraftDefinition definition = new Gson().fromJson(json,
                AircraftDefinitionReloadListener.JsonAircraftDefinition.class)
                .toDefinition("piranport:aircraft/test");
        assertEquals(AircraftInfo.AircraftType.TORPEDO_BOMBER, definition.aircraftClass());
        assertEquals(AircraftDefinition.AttackProfile.TORPEDO, definition.attackProfile());
        assertEquals(AircraftDefinition.PayloadType.AERIAL_TORPEDO, definition.payloadType());
        assertEquals("swordfish", definition.visualId());
        assertEquals(18.0F, definition.panelDamage());
        assertEquals(15, definition.health());
        assertEquals(40, definition.attackCooldown());
    }

    @Test
    void jsonExplicitHealthAndCooldownOverrideLegacyDefaults() {
        String json = "{\"aircraft_class\":\"fighter\",\"attack_profile\":\"gun\","
                + "\"payload_type\":\"none\",\"visual_id\":\"custom\","
                + "\"fuel_capacity\":100,\"ammo_capacity\":1,\"panel_damage\":1.0,"
                + "\"panel_speed\":1.0,\"weight\":1,\"bombing_mode\":\"dive\","
                + "\"health\":37,\"attack_cooldown\":9}";
        AircraftDefinition definition = new Gson().fromJson(json,
                AircraftDefinitionReloadListener.JsonAircraftDefinition.class)
                .toDefinition("piranport:aircraft/custom");
        assertEquals(37, definition.health());
        assertEquals(9, definition.attackCooldown());
    }
}
