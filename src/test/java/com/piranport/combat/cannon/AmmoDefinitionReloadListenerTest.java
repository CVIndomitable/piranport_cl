package com.piranport.combat.cannon.ammo;

import com.google.gson.Gson;
import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AmmoDefinitionReloadListenerTest {
    @AfterEach
    void resetDefinitions() {
        com.piranport.combat.cannon.ammo.AmmoDefinitionService.resetDefaults();
    }

    @Test
    void parsesOptionalFieldsAndUsesBehaviorDefaults() {
        var dto = new Gson().fromJson("{\"behavior\":\"ap\",\"caliber_family\":\"large\"}",
                AmmoDefinitionReloadListener.JsonAmmoDefinition.class);
        AmmoDefinition definition = dto.toDefinition(id("custom_ap_shell"));
        assertEquals(AmmoBehavior.AP, definition.behavior());
        assertEquals(CannonAmmoRules.CaliberFamily.LARGE, definition.caliberFamily().orElseThrow());
        assertEquals(1.0F, definition.damageMultiplier());
        assertEquals(0.5F, definition.armorIgnore());
        assertEquals(com.piranport.combat.cannon.ammo.AmmoBehaviorStrategy.ImpactKind.AP,
                definition.impactKind());
    }

    @Test
    void explicitValuesOverrideBehaviorDefaults() {
        var dto = new Gson().fromJson("{\"behavior\":\"he\",\"damage_multiplier\":2.0,"
                        + "\"explosion_multiplier\":3.0,\"armor_ignore\":0.2,"
                        + "\"underwater_explosion\":false,\"impact_kind\":\"vt\"}",
                AmmoDefinitionReloadListener.JsonAmmoDefinition.class);
        AmmoDefinition definition = dto.toDefinition(id("custom_he_shell"));
        assertEquals(2.0F, definition.damageMultiplier());
        assertEquals(3.0F, definition.explosionMultiplier());
        assertEquals(0.2F, definition.armorIgnore());
        assertFalse(definition.underwaterExplosion());
        assertEquals(com.piranport.combat.cannon.ammo.AmmoBehaviorStrategy.ImpactKind.VT,
                definition.impactKind());
    }

    @Test
    void invalidBehaviorIsRejected() {
        var dto = new Gson().fromJson("{\"behavior\":\"unknown\"}",
                AmmoDefinitionReloadListener.JsonAmmoDefinition.class);
        assertThrows(IllegalArgumentException.class,
                () -> dto.toDefinition(id("invalid_shell")));
    }

    @Test
    void builtInAmmoResourcesContainValidDefinitions() {
        List<String> ids = List.of(
                "small_he_shell", "medium_he_shell", "large_he_shell",
                "small_ap_shell", "medium_ap_shell", "large_ap_shell",
                "type_91_ap_shell", "type_1_ap_shell", "super_heavy_ap_shell",
                "small_vt_shell", "small_type3_shell", "medium_type3_shell",
                "large_type3_shell", "mk23_nuclear_shell");
        Gson gson = new Gson();
        for (String id : ids) {
            String resource = "data/piranport/ammo/" + id + ".json";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream, "missing ammo definition resource: " + resource);
                var json = gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
                        AmmoDefinitionReloadListener.JsonAmmoDefinition.class);
                AmmoDefinition definition = json.toDefinition(id(id));
                assertEquals(id(id), definition.itemId());
                assertTrue(definition.damageMultiplier() >= 0.0F);
                assertTrue(definition.explosionMultiplier() >= 0.0F);
            } catch (Exception exception) {
                fail("invalid ammo definition resource: " + resource, exception);
            }
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("piranport", path);
    }
}
