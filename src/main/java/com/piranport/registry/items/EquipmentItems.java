package com.piranport.registry.items;

import com.piranport.item.ArmorPlateItem;
import com.piranport.item.ConfigInspectorItem;
import com.piranport.item.DamageControlItem;
import com.piranport.item.EngineItem;
import com.piranport.item.EugenShieldItem;
import com.piranport.item.FootballArmorItem;
import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.RepairKitItem;
import com.piranport.item.SmokeCandleItem;
import com.piranport.item.SonarItem;
import com.piranport.item.TaihouUmbrellaItem;
import com.piranport.item.TorpedoReloadItem;
import com.piranport.component.WeaponCategory;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 装备/道具物品注册工厂 — 声呐、引擎、装弹机、工具、防具、道具。
 */
public class EquipmentItems {

    private EquipmentItems() {}

    // ===== Armor Plates =====
    public static DeferredItem<ArmorPlateItem> createSmallArmorPlate(DeferredRegister.Items registry) {
        return registry.register("small_armor_plate",
                () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 2, 10, 3));
    }

    public static DeferredItem<ArmorPlateItem> createMediumArmorPlate(DeferredRegister.Items registry) {
        return registry.register("medium_armor_plate",
                () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 4, 20, 6));
    }

    public static DeferredItem<ArmorPlateItem> createLargeArmorPlate(DeferredRegister.Items registry) {
        return registry.register("large_armor_plate",
                () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                        .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 6, 30, 9));
    }

    // ===== Sonar =====
    public static DeferredItem<SonarItem> createSonar(DeferredRegister.Items registry) {
        return registry.register("sonar",
                () -> new SonarItem(new Item.Properties().stacksTo(1).durability(200)));
    }

    public static DeferredItem<SonarItem> createAdvancedSonar(DeferredRegister.Items registry) {
        return registry.register("advanced_sonar",
                () -> new SonarItem(new Item.Properties().stacksTo(1).durability(400)));
    }

    // ===== Engines =====
    public static DeferredItem<EngineItem> createEngine(DeferredRegister.Items registry) {
        return registry.register("engine",
                () -> new EngineItem(new Item.Properties().stacksTo(1), 1.15f));
    }

    public static DeferredItem<EngineItem> createAdvancedEngine(DeferredRegister.Items registry) {
        return registry.register("advanced_engine",
                () -> new EngineItem(new Item.Properties().stacksTo(1), 1.3f));
    }

    // ===== Torpedo Reload Enhancement =====
    public static DeferredItem<TorpedoReloadItem> createTorpedoReload(DeferredRegister.Items registry) {
        return registry.register("torpedo_reload",
                () -> new TorpedoReloadItem(new Item.Properties().stacksTo(1)));
    }

    // ===== Tools =====
    public static DeferredItem<Item> createCrowbar(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("crowbar", new Item.Properties().stacksTo(1));
    }
    public static DeferredItem<Item> createWrench(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("wrench", new Item.Properties().stacksTo(1));
    }
    public static DeferredItem<Item> createHammer(DeferredRegister.Items registry) {
        return registry.registerSimpleItem("hammer", new Item.Properties().stacksTo(1));
    }

    // ===== Quick Repair =====
    public static DeferredItem<DamageControlItem> createQuickRepair(DeferredRegister.Items registry) {
        return registry.register("quick_repair",
                () -> new DamageControlItem(new Item.Properties().stacksTo(16)));
    }
    public static DeferredItem<DamageControlItem> createEliteDamageControl(DeferredRegister.Items registry) {
        return registry.register("elite_damage_control",
                () -> new DamageControlItem(new Item.Properties().stacksTo(1)));
    }

    // ===== Config Inspector =====
    public static DeferredItem<ConfigInspectorItem> createConfigInspector(DeferredRegister.Items registry) {
        return registry.register("config_inspector",
                () -> new ConfigInspectorItem(new Item.Properties().stacksTo(1)));
    }

    // ===== Smoke Candle =====
    public static DeferredItem<SmokeCandleItem> createSmokeCandle(DeferredRegister.Items registry) {
        return registry.register("smoke_candle",
                () -> new SmokeCandleItem(new Item.Properties().stacksTo(16)));
    }

    // ===== Repair Kit =====
    public static DeferredItem<RepairKitItem> createRepairKit(DeferredRegister.Items registry) {
        return registry.register("repair_kit",
                () -> new RepairKitItem(new Item.Properties().stacksTo(16)));
    }

    // ===== Kirin Headband =====
    public static DeferredItem<KirinHeadbandItem> createKirinHeadband(DeferredRegister.Items registry) {
        return registry.register("kirin_headband",
                () -> new KirinHeadbandItem(new Item.Properties().stacksTo(1).durability(300)));
    }

    // ===== Taihou's Umbrella =====
    public static DeferredItem<TaihouUmbrellaItem> createTaihouUmbrella(DeferredRegister.Items registry) {
        return registry.register("taihou_umbrella",
                () -> new TaihouUmbrellaItem(new Item.Properties().stacksTo(1).durability(500)));
    }

    // ===== Eugen's Ship Shield =====
    public static DeferredItem<EugenShieldItem> createEugenShield(DeferredRegister.Items registry) {
        return registry.register("eugen_shield",
                () -> new EugenShieldItem(new Item.Properties().stacksTo(1).durability(1000)));
    }

    // ===== Football Superstar Set =====
    public static DeferredItem<FootballArmorItem> createFootballHelmet(DeferredRegister.Items registry) {
        return registry.register("football_helmet",
                () -> new FootballArmorItem(net.minecraft.world.entity.EquipmentSlot.HEAD,
                        new Item.Properties().stacksTo(1).durability(300)));
    }

    public static DeferredItem<FootballArmorItem> createFootballChestplate(DeferredRegister.Items registry) {
        return registry.register("football_chestplate",
                () -> new FootballArmorItem(net.minecraft.world.entity.EquipmentSlot.CHEST,
                        new Item.Properties().stacksTo(1).durability(400)));
    }

    public static DeferredItem<FootballArmorItem> createFootballLeggings(DeferredRegister.Items registry) {
        return registry.register("football_leggings",
                () -> new FootballArmorItem(net.minecraft.world.entity.EquipmentSlot.LEGS,
                        new Item.Properties().stacksTo(1).durability(350)));
    }

    public static DeferredItem<FootballArmorItem> createFootballBoots(DeferredRegister.Items registry) {
        return registry.register("football_boots",
                () -> new FootballArmorItem(net.minecraft.world.entity.EquipmentSlot.FEET,
                        new Item.Properties().stacksTo(1).durability(250)));
    }
}
