package com.piranport.combat.cannon.ammo;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AmmoDefinitionServiceReloadTest {
    @AfterEach
    void resetDefinitions() {
        AmmoDefinitionService.resetDefaults();
    }

    @Test
    void builtInBaselineDoesNotInheritReplacedDefinitions() {
        ResourceLocation customId = ResourceLocation.fromNamespaceAndPath("piranport", "custom_shell");
        AmmoDefinition custom = new AmmoDefinition(customId, AmmoBehavior.HE);
        LinkedHashMap<ResourceLocation, AmmoDefinition> replacement = new LinkedHashMap<>();
        replacement.put(customId, custom);
        AmmoDefinitionService.replaceAll(replacement);

        assertTrue(AmmoDefinitionService.allInOrder().contains(custom));
        assertFalse(AmmoDefinitionService.builtInInOrder().stream()
                .anyMatch(definition -> definition.itemId().equals(customId)));
        assertTrue(AmmoDefinitionService.builtInInOrder().stream()
                .anyMatch(definition -> definition.itemId().getPath().equals("small_he_shell")));
    }
}
