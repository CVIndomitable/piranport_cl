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
    void legacyMediumGunIsNotCraftableAndOldBlueprintsGateNothing() throws Exception {
        String source = Files.readString(REGISTRY);
        // 数值/09：通用占位炮 medium_gun（中国双联140）不在 07 注册表，生存模式不给配方。
        assertFalse(source.contains("ModItems.MEDIUM_GUN"),
                "medium_gun 不应再进武器制造台");
        // 旧中型/大型火炮蓝图只作为物品保留（兼容旧存档），不再是任何配方的蓝图门槛。
        assertFalse(source.contains("MEDIUM_GUN_BLUEPRINT"));
        assertFalse(source.contains("LARGE_GUN_BLUEPRINT"));
        // requiredBlueprint == null 表示无需蓝图，不是不可合成（既定设计）。
        assertTrue(source.contains("recipe.requiredBlueprint() == null) return true;"));
    }

    @Test
    void tierBlueprintModelsUseTheirOwnTextures() throws Exception {
        for (String id : new String[] {
                "standard_weapon_blueprint", "improved_weapon_blueprint", "advanced_weapon_blueprint"
        }) {
            String model = Files.readString(ASSETS.resolve("models/item/" + id + ".json"));
            assertTrue(model.contains("\"layer0\": \"piranport:item/" + id + "\""), id);
            // f4c82ef9 已补上三档蓝图贴图；此前的“缺失贴图占位”断言已过期。
            assertTrue(Files.exists(ASSETS.resolve("textures/item/" + id + ".png")),
                    id + " 的物品贴图应已存在");
        }
    }
}
