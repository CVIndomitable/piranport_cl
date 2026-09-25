package com.piranport.combat.cannon.ammo;

import com.google.gson.Gson;
import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("piranport", path);
    }
}
