package com.piranport.combat.cannon;

import com.piranport.combat.cannon.ammo.AmmoBehavior;
import com.piranport.combat.cannon.ammo.AmmoDefinition;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AmmoDefinitionTest {
    @Test
    void defaultsUseStableItemIdsAndDistinctBehaviors() {
        var he = AmmoDefinitionService.require(id("small_he_shell"));
        var ap = AmmoDefinitionService.require(id("small_ap_shell"));
        var vt = AmmoDefinitionService.require(id("small_vt_shell"));
        var type3 = AmmoDefinitionService.require(id("small_type3_shell"));

        assertEquals(AmmoBehavior.HE, he.behavior());
        assertEquals(AmmoBehavior.AP, ap.behavior());
        assertEquals(AmmoBehavior.VT, vt.behavior());
        assertEquals(AmmoBehavior.TYPE3, type3.behavior());
        assertNotEquals(he.itemId(), ap.itemId());
    }

    @Test
    void caliberConstraintIsExplicitAndValidated() {
        var definition = AmmoDefinitionService.require(id("medium_type3_shell"));
        assertTrue(definition.isCompatibleWith(CannonAmmoRules.CaliberFamily.MEDIUM));
        assertFalse(definition.isCompatibleWith(CannonAmmoRules.CaliberFamily.SMALL));
    }

    @Test
    void replacementValidatesMapKeysAndPublishesSnapshot() {
        var id = id("test_ammo");
        var definition = new AmmoDefinition(id, AmmoBehavior.HE);
        var replacement = new LinkedHashMap<ResourceLocation, AmmoDefinition>();
        replacement.put(id, definition);
        try {
            AmmoDefinitionService.replaceAll(replacement);
            assertSame(definition, AmmoDefinitionService.require(id));
            assertEquals(1, AmmoDefinitionService.all().size());
        } finally {
            // Restore built-in defaults for other tests in the same JVM.
            AmmoDefinitionService.resetDefaults();
        }
    }

    @Test
    void mismatchedDefinitionKeyIsRejected() {
        var key = id("key");
        var value = new AmmoDefinition(id("value"), AmmoBehavior.AP);
        var replacement = new LinkedHashMap<ResourceLocation, AmmoDefinition>();
        replacement.put(key, value);
        assertThrows(IllegalArgumentException.class, () -> AmmoDefinitionService.replaceAll(replacement));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("piranport", path);
    }
}
