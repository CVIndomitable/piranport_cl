package com.piranport.aviation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.piranport.component.AircraftState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AircraftDefinitionTest {
    @AfterEach
    void clearDefinitions() {
        AircraftDefinitionService.clear();
    }

    @Test
    void definitionIdIsItemIdAndResolvesDirectly() {
        AircraftDefinition definition = new AircraftDefinition(
                ResourceLocation.parse("piranport:swordfish_torpedo"),
                AircraftClass.TORPEDO_BOMBER,
                AircraftDefinition.AttackProfile.TORPEDO,
                ResourceLocation.parse("piranport:aerial_torpedo"),
                "torpedo_bomber", 1200, 1, 18.0F, 0.5F, 18, 15, 40, null);

        AircraftDefinitionService.register(definition);
        assertSame(definition, AircraftDefinitionService.find(
                ResourceLocation.parse("piranport:swordfish_torpedo")));
        assertEquals("piranport:aerial_torpedo", definition.payloadItem().toString());
        assertTrue(definition.requiresPayload());
    }

    @Test
    void invalidClassProfileCombinationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftDefinition(
                ResourceLocation.parse("piranport:invalid"),
                AircraftClass.RECON,
                AircraftDefinition.AttackProfile.LEVEL_BOMB,
                ResourceLocation.parse("piranport:aerial_bomb"),
                "recon", 100, 1, 1.0F, 1.0F, 1, 10, 1, null));
    }

    @Test
    void reconWithoutAttackProfileIsAccepted() {
        AircraftDefinition definition = new AircraftDefinition(
                ResourceLocation.parse("piranport:recon"),
                AircraftClass.RECON,
                null,
                null,
                "recon", 100, 0, 0.0F, 1.0F, 1, 10, 1, null);

        assertNull(definition.attackProfile());
        assertTrue(definition.isRecon());
        assertFalse(definition.usesGunAttack());
        assertFalse(definition.isPayloadAttacker());
    }

    @Test
    void reconWithAttackProfileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftDefinition(
                ResourceLocation.parse("piranport:invalid_recon"),
                AircraftClass.RECON,
                AircraftDefinition.AttackProfile.NONE,
                null,
                "recon", 100, 0, 0.0F, 1.0F, 1, 10, 1, null));
    }

    @Test
    void nonReconWithoutAttackProfileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftDefinition(
                ResourceLocation.parse("piranport:invalid_fighter"),
                AircraftClass.FIGHTER,
                null,
                null,
                "fighter", 100, 1, 1.0F, 1.0F, 1, 10, 1, null));
    }

    @Test
    void unknownVisualIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftDefinition(
                ResourceLocation.parse("piranport:invalid_visual"),
                AircraftClass.FIGHTER,
                AircraftDefinition.AttackProfile.GUN,
                null, "not_a_visual", 100, 1, 1.0F, 1.0F, 1, 10, 1, null));
    }

    @Test
    void reconRejectsSelfDefense() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftDefinition(
                ResourceLocation.parse("piranport:invalid_recon_gun"),
                AircraftClass.RECON, null, null, "recon",
                100, 0, 0.0F, 1.0F, 1, 10, 1,
                new SelfDefense(Set.of(SelfDefenseZone.UPPER), 10.0, 10, 1.0F)));
    }

    @Test
    void jsonCodecBuildsDefinitionWithResourceId() {
        String json = "{\"aircraft_class\":\"torpedo_bomber\",\"attack_profile\":\"torpedo\","
                + "\"payload_item\":\"piranport:aerial_torpedo\",\"visual_id\":\"torpedo_bomber\","
                + "\"fuel_capacity\":1200,\"ammo_capacity\":1,\"panel_damage\":18.0,"
                + "\"panel_speed\":0.5,\"weight\":18,\"health\":15,\"attack_cooldown\":40}";
        JsonElement element = JsonParser.parseString(json);
        AircraftDefinition definition = AircraftDefinition.JsonDefinition.CODEC
                .parse(JsonOps.INSTANCE, element)
                .getOrThrow(message -> new IllegalArgumentException(message))
                .withId(ResourceLocation.parse("piranport:swordfish_torpedo"));

        assertEquals(AircraftClass.TORPEDO_BOMBER, definition.aircraftClass());
        assertEquals(AircraftDefinition.AttackProfile.TORPEDO, definition.attackProfile());
        assertEquals(ResourceLocation.parse("piranport:aerial_torpedo"), definition.payloadItem());
        assertEquals("torpedo_bomber", definition.visualId());
        assertEquals(18.0F, definition.panelDamage());
        assertEquals(15, definition.health());
        assertEquals(40, definition.attackCooldown());
    }

    @Test
    void jsonCodecAllowsReconWithoutAttackProfile() {
        String json = "{\"aircraft_class\":\"recon\","
                + "\"visual_id\":\"recon\","
                + "\"fuel_capacity\":100,\"ammo_capacity\":0,\"panel_damage\":0.0,"
                + "\"panel_speed\":1.0,\"weight\":1,\"health\":10,\"attack_cooldown\":1}";
        JsonElement element = JsonParser.parseString(json);
        AircraftDefinition definition = AircraftDefinition.JsonDefinition.CODEC
                .parse(JsonOps.INSTANCE, element)
                .getOrThrow(message -> new IllegalArgumentException(message))
                .withId(ResourceLocation.parse("piranport:c1_recon"));

        assertNull(definition.attackProfile());
        assertTrue(definition.isRecon());
        assertTrue(definition.payloadItem() == null);
    }

    @Test
    void nationDefaultsToUnknownAndParsesDeclaredValue() {
        String template = "{\"aircraft_class\":\"fighter\",\"attack_profile\":\"gun\","
                + "\"visual_id\":\"fighter\",\"fuel_capacity\":100,\"ammo_capacity\":1,"
                + "\"panel_damage\":1.0,\"panel_speed\":1.0,\"weight\":1,\"health\":10,"
                + "\"attack_cooldown\":40%s}";

        AircraftDefinition defaults = AircraftDefinition.JsonDefinition.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(String.format(template, "")))
                .getOrThrow(message -> new IllegalArgumentException(message))
                .withId(ResourceLocation.parse("piranport:nation_default"));
        assertEquals(AircraftDefinition.Nation.UNKNOWN, defaults.nation());

        AircraftDefinition declared = AircraftDefinition.JsonDefinition.CODEC
                .parse(JsonOps.INSTANCE,
                        JsonParser.parseString(String.format(template, ",\"nation\":\"gb\"")))
                .getOrThrow(message -> new IllegalArgumentException(message))
                .withId(ResourceLocation.parse("piranport:nation_declared"));
        assertEquals(AircraftDefinition.Nation.GB, declared.nation());
    }

    @Test
    void aircraftStateStreamCodecRoundTrips() {
        AircraftState original = new AircraftState(37, true);
        ByteBuf buffer = Unpooled.buffer();
        AircraftState.STREAM_CODEC.encode(buffer, original);
        AircraftState decoded = AircraftState.STREAM_CODEC.decode(buffer);
        assertEquals(original, decoded);
    }

    @Test
    void allRegisteredAircraftResourcesResolveToValidDefinitions() {
        List<String> ids = List.of(
                "xtb2d", "swordfish_torpedo", "swordfish_asw", "tbf_torpedo",
                "tbf_asw", "tenzan_torpedo", "type97_torpedo", "sky_pirate_torpedo",
                "petrel_bomber", "type99_dive_bomber", "sbd_dauntless", "firefly_as_mk5",
                "suisei_bomber", "seiun_kai_bomber", "b25_bomber", "xa2j_bomber",
                "f6f_hellcat_rocket", "seafire", "zero_model52", "f4f_wildcat",
                "f4u_corsair_ice", "f4u_corsair", "f2h_banshee",
                "type0_recon", "c1_recon", "saiun_recon",
                "deep_sea_fighter_1", "deep_sea_fighter_2",
                "deep_sea_bomber_1", "deep_sea_bomber_2",
                "deep_sea_torpedo_bomber_1", "deep_sea_torpedo_bomber_2");
        Set<ResourceLocation> seen = new HashSet<>();
        for (String id : ids) {
            String resource = "data/piranport/aircraft/" + id + ".json";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream, "missing aircraft definition resource: " + resource);
                JsonElement json = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                ResourceLocation definitionId = ResourceLocation.parse("piranport:" + id);
                AircraftDefinition definition = AircraftDefinition.JsonDefinition.CODEC
                        .parse(JsonOps.INSTANCE, json)
                        .getOrThrow(message -> new IllegalArgumentException(
                                resource + ": " + message))
                        .withId(definitionId);
                assertEquals(definitionId, definition.id());
                assertTrue(seen.add(definition.id()), "duplicate aircraft id: " + definition.id());
                assertTrue(definition.health() > 0);
                assertTrue(definition.attackCooldown() > 0);
                assertTrue(AircraftVisualIds.isKnown(definition.visualId()));
                // 玩家飞机必须声明系别；深海编队没有国籍，允许 unknown。
                if (!id.startsWith("deep_sea_")) {
                    assertNotEquals(AircraftDefinition.Nation.UNKNOWN, definition.nation(),
                            "aircraft definition must declare nation: " + definition.id());
                }
                if ("b25_bomber".equals(id)) {
                    assertNotNull(definition.selfDefense());
                    assertTrue(definition.selfDefense().covers(SelfDefenseZone.UPPER));
                    assertTrue(definition.selfDefense().covers(SelfDefenseZone.FRONT));
                    assertTrue(definition.selfDefense().covers(SelfDefenseZone.REAR));
                    assertFalse(definition.selfDefense().covers(SelfDefenseZone.LOWER));
                } else {
                    assertNull(definition.selfDefense(),
                            "本期只有 B-25 声明 self_defense");
                }
                if (definition.aircraftClass() == AircraftClass.RECON) {
                    assertNull(definition.attackProfile(),
                            "recon aircraft must not declare attack_profile: " + definition.id());
                    assertTrue(definition.isRecon());
                } else {
                    assertNotNull(definition.attackProfile(),
                            "non-recon aircraft must declare attack_profile: " + definition.id());
                }
            } catch (Exception exception) {
                fail("invalid aircraft definition resource: " + resource, exception);
            }
        }
    }
}
