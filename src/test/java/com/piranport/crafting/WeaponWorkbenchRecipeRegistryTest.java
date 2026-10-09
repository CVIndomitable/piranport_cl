package com.piranport.crafting;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WeaponWorkbenchRecipeRegistryTest {
    private static final Path REGISTRY = Path.of(
            "src/main/java/com/piranport/crafting/WeaponWorkbenchRecipeRegistry.java");
    private static final Path ASSETS = Path.of("src/main/resources/assets/piranport");

    @Test
    void legacyMediumGunRecipeDoesNotRequireLegacyBlueprint() throws Exception {
        String source = Files.readString(REGISTRY);
        int recipeStart = source.indexOf("add(r, 0, ModItems.MEDIUM_GUN.get()");
        int recipeEnd = source.indexOf("// 数值/07 注册表", recipeStart);
        assertTrue(recipeStart >= 0);
        assertTrue(recipeEnd > recipeStart);

        String recipe = source.substring(recipeStart, recipeEnd);
        assertTrue(recipe.contains("), null, 200);"));
        assertFalse(recipe.contains("BLUEPRINT"));
        assertTrue(source.contains("stack.is(ModItems.MEDIUM_GUN_BLUEPRINT.get())"),
                "旧中型火炮蓝图仍应保持物品兼容入口");
        assertTrue(source.contains("stack.is(ModItems.LARGE_GUN_BLUEPRINT.get())"),
                "旧大型火炮蓝图仍应保持物品兼容入口");
    }

    @Test
    void tierBlueprintModelsUseDistinctMissingTexturePlaceholders() throws Exception {
        for (String id : new String[] {
                "standard_weapon_blueprint", "improved_weapon_blueprint", "advanced_weapon_blueprint"
        }) {
            String model = Files.readString(ASSETS.resolve("models/item/" + id + ".json"));
            assertTrue(model.contains("\"layer0\": \"piranport:item/" + id + "\""), id);
            assertFalse(Files.exists(ASSETS.resolve("textures/item/" + id + ".png")),
                    id + " 暂无物品贴图，应继续显示缺失贴图占位");
        }
    }
}
