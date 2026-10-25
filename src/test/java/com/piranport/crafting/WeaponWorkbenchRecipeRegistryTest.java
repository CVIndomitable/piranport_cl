package com.piranport.crafting;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WeaponWorkbenchRecipeRegistryTest {
    private static final Path REGISTRY = Path.of(
            "src/main/java/com/piranport/crafting/WeaponWorkbenchRecipeRegistry.java");
    private static final Path TABS = Path.of(
            "src/main/java/com/piranport/crafting/WeaponWorkbenchTabs.java");
    private static final Path BLOCK_ENTITY = Path.of(
            "src/main/java/com/piranport/block/entity/WeaponWorkbenchBlockEntity.java");
    private static final Path MENU = Path.of("src/main/java/com/piranport/menu/WeaponWorkbenchMenu.java");
    private static final Path SCREEN = Path.of("src/main/java/com/piranport/menu/WeaponWorkbenchScreen.java");
    private static final Path ASSETS = Path.of("src/main/resources/assets/piranport");

    @Test
    void legacyMediumGunIsNotCraftableAndOldBlueprintsGateNothing() throws Exception {
        String source = Files.readString(REGISTRY);
        // 数值/09：通用占位炮 medium_gun（中国双联140）不在 07 注册表，生存模式不给配方。
        assertFalse(source.contains("ModItems.MEDIUM_GUN"),
                "medium_gun 不应再进武器制造台");
        // 旧中型/大型火炮蓝图物品已删除；保留断言作为防回流守卫，防止旧蓝图重新成为配方门槛。
        assertFalse(source.contains("MEDIUM_GUN_BLUEPRINT"));
        assertFalse(source.contains("LARGE_GUN_BLUEPRINT"));
        // requiredBlueprint == null 表示无需蓝图，不是不可合成（既定设计）。
        assertTrue(source.contains("recipe.requiredBlueprint() == null) return true;"));
    }

    /**
     * 页签上限曾分散成 BE 5 / Menu 5 / Screen 6 三份字面量，导致第 6 个「装备」页
     * 服务端直接拒绝、整页配方不可达。这里守住「单一定义 + 各端引用 + 装备页非空」。
     */
    @Test
    void tabCountHasSingleSourceAndEquipmentTabIsPopulated() throws Exception {
        assertTrue(Files.readString(TABS).contains("public static final int TAB_COUNT = 6;"),
                "页签总数应只定义在共享常量类里");

        String be = Files.readString(BLOCK_ENTITY);
        assertFalse(be.contains("private static final int TAB_COUNT"), "BE 不应再自带页签常量");
        assertTrue(be.contains("WeaponWorkbenchTabs.TAB_COUNT"),
                "BE 的 setSelectedTab/读档 clamp 应引用共享常量");

        assertTrue(Files.readString(MENU)
                        .contains("BTN_TAB_MAX = WeaponWorkbenchTabs.TAB_COUNT - 1;"),
                "Menu 页签按钮上限应引用共享常量");
        assertTrue(Files.readString(SCREEN).contains("WeaponWorkbenchTabs.TAB_COUNT"),
                "Screen 渲染循环应引用共享常量");

        String registry = Files.readString(REGISTRY);

        // 每个页签都至少要有一条配方：只放开上限、页面却是空的同样算回归。
        for (int tab = 0; tab < 6; tab++) {
            assertTrue(registry.contains("add(r, " + tab + ", "),
                    "页签 " + tab + " 应有配方注册");
        }

        // 页签提示数组必须与共享常量等长，否则渲染到最后一个页签时会越界。
        long tooltips = Files.readString(SCREEN).lines()
                .filter(line -> line.contains("gui.piranport.workbench.tab."))
                .count();
        assertEquals(6, tooltips, "Screen 的页签提示数量应与 TAB_COUNT 一致");
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
