package com.piranport.registry.items;

import com.piranport.item.AmmoItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 材料物品注册工厂 — 矿物、弹药、蓝图、燃料、指南书。
 */
public class MaterialItems {

    private MaterialItems() {}

    // ===== Materials =====
    public static DeferredItem<Item> createRawAluminum(DeferredRegister.Items registry) { return registry.registerSimpleItem("raw_aluminum"); }
    public static DeferredItem<Item> createAluminumIngot(DeferredRegister.Items registry) { return registry.registerSimpleItem("aluminum_ingot"); }
    public static DeferredItem<Item> createSalt(DeferredRegister.Items registry) { return registry.registerSimpleItem("salt"); }
    public static DeferredItem<Item> createTabIcon(DeferredRegister.Items registry) { return registry.registerSimpleItem("tab_icon"); }

    // ===== HE Shells =====
    public static DeferredItem<Item> createSmallHeShell(DeferredRegister.Items registry) {
        return registry.register("small_he_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    }
    public static DeferredItem<Item> createMediumHeShell(DeferredRegister.Items registry) {
        return registry.register("medium_he_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    }
    public static DeferredItem<Item> createLargeHeShell(DeferredRegister.Items registry) {
        return registry.register("large_he_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    }

    // ===== AP Shells =====
    public static DeferredItem<Item> createSmallApShell(DeferredRegister.Items registry) {
        return registry.register("small_ap_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    }
    public static DeferredItem<Item> createMediumApShell(DeferredRegister.Items registry) {
        return registry.register("medium_ap_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    }
    public static DeferredItem<Item> createLargeApShell(DeferredRegister.Items registry) {
        return registry.register("large_ap_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    }

    // ===== VT Shells =====
    public static DeferredItem<Item> createSmallVtShell(DeferredRegister.Items registry) {
        return registry.register("small_vt_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.vt_shell"));
    }

    // ===== Type 3 Shells =====
    public static DeferredItem<Item> createSmallType3Shell(DeferredRegister.Items registry) {
        return registry.register("small_type3_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    }
    public static DeferredItem<Item> createMediumType3Shell(DeferredRegister.Items registry) {
        return registry.register("medium_type3_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    }
    public static DeferredItem<Item> createLargeType3Shell(DeferredRegister.Items registry) {
        return registry.register("large_type3_shell",
                () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    }

    // ===== Blueprints =====
    public static DeferredItem<Item> createMediumGunBlueprint(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("medium_gun_blueprint", new Item.Properties().stacksTo(1));
    }
    public static DeferredItem<Item> createLargeGunBlueprint(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("large_gun_blueprint", new Item.Properties().stacksTo(1));
    }
    public static DeferredItem<Item> createCreativeBlueprint(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("creative_blueprint",
                new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    // ===== Fuel =====
    public static DeferredItem<Item> createShipFuel(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("ship_fuel", new Item.Properties().stacksTo(16));
    }
    public static DeferredItem<Item> createAviationFuel(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("aviation_fuel", new Item.Properties().stacksTo(16));
    }

    // ===== Phase 23: Guidebook =====
    public static DeferredItem<Item> createGuidebook(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("guidebook", new Item.Properties().stacksTo(1));
    }
}
