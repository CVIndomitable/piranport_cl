package com.piranport.registry;

import com.piranport.item.ShipType;

import com.piranport.PiranPort;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FuelData;
import com.piranport.component.PlaceableInfo;
import com.piranport.component.WeaponCategory;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.entitycore.EntityCoreDefinitions;
import com.piranport.item.AircraftItem;
import com.piranport.item.AmmoItem;
import com.piranport.item.AbyssalReportItem;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.FloatingTargetItem;
import com.piranport.item.GuidebookItem;
import com.piranport.item.AutoCIWSItem;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.artillery.config.MuzzlePos;
import com.piranport.item.BottleFoodItem;
import com.piranport.item.EntityCoreItem;
import com.piranport.item.ModFoodItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipGirlContractItem;
import com.piranport.item.SkinCoreItem;
import com.piranport.item.SonarItem;
import com.piranport.item.TooltipItem;
import com.piranport.item.EngineItem;
import com.piranport.item.ExperienceShellItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.item.TorpedoReloadItem;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.FlareLauncherItem;
import com.piranport.item.ConfigInspectorItem;
import com.piranport.item.ArtilleryConfigToolItem;
import com.piranport.item.DamageControlItem;
import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.RepairKitItem;
import com.piranport.item.FootballArmorItem;
import com.piranport.item.GungnirItem;
import com.piranport.item.HatsuyukiMainGunItem;
import com.piranport.item.CommandSwordItem;
import com.piranport.item.MysteriousWeaponItem;
import com.piranport.item.SmokeCandleItem;
import com.piranport.item.EugenShieldItem;
import com.piranport.item.ShoukakuScytheItem;
import com.piranport.item.TaihouUmbrellaItem;
import com.piranport.item.UnicornHarpItem;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PiranPort.MOD_ID);

    // ===== Block Items（已迁移到 MaterialItems；保留薄包装以兼容旧引用） =====
    public static final DeferredItem<BlockItem> BAUXITE_ORE = MaterialItems.BAUXITE_ORE;
    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK = MaterialItems.ALUMINUM_BLOCK;
    public static final DeferredItem<BlockItem> SALT_BLOCK = MaterialItems.SALT_BLOCK;
    public static final DeferredItem<BlockItem> SALT_CHIP = MaterialItems.SALT_CHIP;

    // ===== Abyssal Blocks (v0.0.11)（已迁移到 MaterialItems） =====
    public static final DeferredItem<BlockItem> ABYSSAL_PORTAL_FRAME = MaterialItems.ABYSSAL_PORTAL_FRAME;
    public static final DeferredItem<BlockItem> ABYSSAL_SPAWNER = MaterialItems.ABYSSAL_SPAWNER;
    public static final DeferredItem<BlockItem> ABYSSAL_SEEP = MaterialItems.ABYSSAL_SEEP;

    // ===== Decorative Blocks（已迁移到 MaterialItems） =====
    public static final DeferredItem<BlockItem> CONFIDENTIAL_CARGO = MaterialItems.CONFIDENTIAL_CARGO;
    public static final DeferredItem<BlockItem> ABYSS_RED_SPIDER_LILY = MaterialItems.ABYSS_RED_SPIDER_LILY;
    public static final DeferredItem<BlockItem> ITALIAN_DISH_KIT = MaterialItems.ITALIAN_DISH_KIT;

    public static final DeferredItem<BlockItem> PIRATE_CHAIR = MaterialItems.PIRATE_CHAIR;

    public static final DeferredItem<BlockItem> PIRATE_TABLE = MaterialItems.PIRATE_TABLE;
    public static final DeferredItem<BlockItem> B25_MODEL = MaterialItems.B25_MODEL;

    // ===== Tab Icon（已迁移到 MaterialItems） =====
    public static final DeferredItem<Item> TAB_ICON = MaterialItems.TAB_ICON;

    // ===== Materials（已迁移到 MaterialItems） =====
    public static final DeferredItem<Item> RAW_ALUMINUM = MaterialItems.RAW_ALUMINUM;
    public static final DeferredItem<Item> ALUMINUM_INGOT = MaterialItems.ALUMINUM_INGOT;
    public static final DeferredItem<Item> SALT = MaterialItems.SALT;

    // ===== Ship Cores (extracted to WeaponItems) =====
    public static final DeferredItem<ShipCoreItem> SMALL_SHIP_CORE = WeaponItems.SMALL_SHIP_CORE;
    public static final DeferredItem<ShipCoreItem> MEDIUM_SHIP_CORE = WeaponItems.MEDIUM_SHIP_CORE;
    public static final DeferredItem<ShipCoreItem> LARGE_SHIP_CORE = WeaponItems.LARGE_SHIP_CORE;
    public static final DeferredItem<ShipCoreItem> SUBMARINE_CORE = WeaponItems.SUBMARINE_CORE;

    // ===== HE Shells (extracted to AmmoItems) =====
    public static final DeferredItem<Item> SMALL_HE_SHELL = AmmoItems.SMALL_HE_SHELL;
    public static final DeferredItem<Item> MEDIUM_HE_SHELL = AmmoItems.MEDIUM_HE_SHELL;
    public static final DeferredItem<Item> LARGE_HE_SHELL = AmmoItems.LARGE_HE_SHELL;

    // ===== MK23 Nuclear Shell (extracted to AmmoItems) =====
    public static final DeferredItem<Item> MK23_NUCLEAR_SHELL = AmmoItems.MK23_NUCLEAR_SHELL;

    // ===== AP Shells (extracted to AmmoItems) =====
    public static final DeferredItem<Item> SMALL_AP_SHELL = AmmoItems.SMALL_AP_SHELL;
    public static final DeferredItem<Item> MEDIUM_AP_SHELL = AmmoItems.MEDIUM_AP_SHELL;
    public static final DeferredItem<Item> LARGE_AP_SHELL = AmmoItems.LARGE_AP_SHELL;

    // Phase 27: named AP shells (extracted to AmmoItems)
    public static final DeferredItem<Item> TYPE_91_AP_SHELL = AmmoItems.TYPE_91_AP_SHELL;
    public static final DeferredItem<Item> TYPE_1_AP_SHELL = AmmoItems.TYPE_1_AP_SHELL;
    public static final DeferredItem<Item> SUPER_HEAVY_AP_SHELL = AmmoItems.SUPER_HEAVY_AP_SHELL;

    // ===== VT Shells (proximity fuze, small caliber only) (extracted to AmmoItems) =====
    public static final DeferredItem<Item> SMALL_VT_SHELL = AmmoItems.SMALL_VT_SHELL;

    // ===== Type 3 (Sanshiki) Shells (extracted to AmmoItems) =====
    public static final DeferredItem<Item> SMALL_TYPE3_SHELL = AmmoItems.SMALL_TYPE3_SHELL;
    public static final DeferredItem<Item> MEDIUM_TYPE3_SHELL = AmmoItems.MEDIUM_TYPE3_SHELL;
    public static final DeferredItem<Item> LARGE_TYPE3_SHELL = AmmoItems.LARGE_TYPE3_SHELL;

    // ===== Guns (extracted to WeaponItems) =====
    public static final DeferredItem<Item> SINGLE_SMALL_GUN = WeaponItems.SINGLE_SMALL_GUN;
    public static final DeferredItem<Item> SMALL_GUN = WeaponItems.SMALL_GUN;
    public static final DeferredItem<Item> MEDIUM_GUN = WeaponItems.MEDIUM_GUN;
    public static final DeferredItem<Item> LARGE_GUN = WeaponItems.LARGE_GUN;
    public static final DeferredItem<Item> FRENCH_QUAD_380MM_GUN = WeaponItems.FRENCH_QUAD_380MM_GUN;
    public static final DeferredItem<Item> SEVEN_BARREL_GUN = WeaponItems.SEVEN_BARREL_GUN;
    public static final DeferredItem<Item> SALVO_TEST_GUN = WeaponItems.SALVO_TEST_GUN;
    public static final DeferredItem<Item> FOURTEEN_BARREL_GUN = WeaponItems.FOURTEEN_BARREL_GUN;

    // ===== Torpedo Ammo (legacy generic) (extracted to AmmoItems) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM = AmmoItems.TORPEDO_533MM;
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM = AmmoItems.TORPEDO_610MM;
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM = AmmoItems.MAGNETIC_TORPEDO_533MM;
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM = AmmoItems.WIRE_GUIDED_TORPEDO_533MM;
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM = AmmoItems.ACOUSTIC_TORPEDO_533MM;

    // Phase 27: oxygen torpedo (extracted to AmmoItems)
    public static final DeferredItem<TorpedoItem> OXYGEN_TORPEDO_610MM = AmmoItems.OXYGEN_TORPEDO_610MM;

    // ===== Torpedo Ammo (named variants) (extracted to AmmoItems) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_G7A = AmmoItems.TORPEDO_533MM_G7A;
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7A = AmmoItems.MAGNETIC_TORPEDO_533MM_G7A;
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK17 = AmmoItems.TORPEDO_533MM_MK17;
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE91 = AmmoItems.TORPEDO_610MM_TYPE91;
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK1 = AmmoItems.TORPEDO_610MM_TYPE93_MK1;
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK3 = AmmoItems.TORPEDO_610MM_TYPE93_MK3;
    public static final DeferredItem<TorpedoItem> TORPEDO_720MM_TYPE0 = AmmoItems.TORPEDO_720MM_TYPE0;
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK14 = AmmoItems.TORPEDO_533MM_MK14;
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK16 = AmmoItems.TORPEDO_533MM_MK16;
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7E = AmmoItems.MAGNETIC_TORPEDO_533MM_G7E;
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_G7E = AmmoItems.ACOUSTIC_TORPEDO_533MM_G7E;
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM_G7E = AmmoItems.WIRE_GUIDED_TORPEDO_533MM_G7E;
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_MK27 = AmmoItems.ACOUSTIC_TORPEDO_533MM_MK27;
    public static final DeferredItem<TorpedoItem> TORPEDO_530MM_TYPE95 = AmmoItems.TORPEDO_530MM_TYPE95;
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE95_MK2 = AmmoItems.TORPEDO_610MM_TYPE95_MK2;

    // 数值配置/05 鱼雷补缺（2026-09-07 项目所有者定稿）(extracted to AmmoItems)
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE92 = AmmoItems.TORPEDO_610MM_TYPE92;
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK13 = AmmoItems.TORPEDO_533MM_MK13;
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_53_38 = AmmoItems.TORPEDO_533MM_53_38;

    // ===== Armor Plates（已迁移到 MaterialItems） =====
    public static final DeferredItem<ArmorPlateItem> SMALL_ARMOR_PLATE = MaterialItems.SMALL_ARMOR_PLATE;
    public static final DeferredItem<ArmorPlateItem> MEDIUM_ARMOR_PLATE = MaterialItems.MEDIUM_ARMOR_PLATE;
    public static final DeferredItem<ArmorPlateItem> LARGE_ARMOR_PLATE = MaterialItems.LARGE_ARMOR_PLATE;

    // ===== Auto CIWS（强化部件槽 — 策划决策/舰装/舰装-自动近防炮系统.md）(extracted to WeaponItems) =====
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_20MM = WeaponItems.AUTO_CIWS_20MM;
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_40MM = WeaponItems.AUTO_CIWS_40MM;
    public static final DeferredItem<AutoCIWSItem> AUTO_CIWS_76MM = WeaponItems.AUTO_CIWS_76MM;

    // ===== Food Ingredients (Phase 11a) =====
    public static final DeferredItem<Item> FLOUR           = ITEMS.registerSimpleItem("flour");
    public static final DeferredItem<Item> RICE_FLOUR      = ITEMS.registerSimpleItem("rice_flour");
    public static final DeferredItem<Item> CHILI_POWDER    = ITEMS.registerSimpleItem("chili_powder");
    public static final DeferredItem<Item> PORK_PASTE      = ITEMS.registerSimpleItem("pork_paste");
    public static final DeferredItem<Item> EDIBLE_OIL      = ITEMS.registerSimpleItem("edible_oil");
    public static final DeferredItem<Item> BUTTER          = ITEMS.registerSimpleItem("butter");
    public static final DeferredItem<Item> CREAM           = ITEMS.registerSimpleItem("cream");
    public static final DeferredItem<Item> SOYBEAN_MILK    = ITEMS.registerSimpleItem("soybean_milk");
    public static final DeferredItem<Item> TOFU            = ITEMS.registerSimpleItem("tofu");
    public static final DeferredItem<Item> CHEESE          = ITEMS.registerSimpleItem("cheese");
    public static final DeferredItem<Item> YEAST           = ITEMS.registerSimpleItem("yeast");
    public static final DeferredItem<Item> SOY_SAUCE       = ITEMS.registerSimpleItem("soy_sauce");
    public static final DeferredItem<Item> VINEGAR         = ITEMS.registerSimpleItem("vinegar");
    public static final DeferredItem<Item> COOKING_WINE    = ITEMS.registerSimpleItem("cooking_wine");
    public static final DeferredItem<Item> MISO            = ITEMS.registerSimpleItem("miso");
    public static final DeferredItem<Item> BRINE           = ITEMS.registerSimpleItem("brine");
    public static final DeferredItem<Item> PIE_CRUST       = ITEMS.registerSimpleItem("pie_crust");
    public static final DeferredItem<Item> RAW_PASTA       = ITEMS.registerSimpleItem("raw_pasta");
    public static final DeferredItem<Item> FERMENTED_FISH  = ITEMS.registerSimpleItem("fermented_fish");
    public static final DeferredItem<Item> PIZZA_BASE      = ITEMS.registerSimpleItem("pizza_base");
    public static final DeferredItem<Item> GYPSUM_CHIP     = ITEMS.registerSimpleItem("gypsum_chip");
    public static final DeferredItem<Item> QUICKLIME       = ITEMS.registerSimpleItem("quicklime");

    // ===== Crop Produce (Phase 11b) =====
    public static final DeferredItem<Item> TOMATO  = ITEMS.registerSimpleItem("tomato");
    public static final DeferredItem<Item> SOYBEAN = ITEMS.registerSimpleItem("soybean");
    public static final DeferredItem<Item> CHILI   = ITEMS.registerSimpleItem("chili");
    public static final DeferredItem<Item> LETTUCE = ITEMS.registerSimpleItem("lettuce");
    public static final DeferredItem<Item> RICE    = ITEMS.registerSimpleItem("rice");
    public static final DeferredItem<Item> ONION   = ITEMS.registerSimpleItem("onion");
    public static final DeferredItem<Item> GARLIC  = ITEMS.registerSimpleItem("garlic");

    // ===== Crop Seeds (Phase 11b) =====
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> TOMATO_SEEDS =
            ITEMS.register("tomato_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.TOMATO_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> SOYBEAN_SEEDS =
            ITEMS.register("soybean_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.SOYBEAN_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> CHILI_SEEDS =
            ITEMS.register("chili_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.CHILI_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> ONION_SEEDS =
            ITEMS.register("onion_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.ONION_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> RICE_SEEDS =
            ITEMS.register("rice_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.RICE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> LETTUCE_SEEDS =
            ITEMS.register("lettuce_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.LETTUCE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> GARLIC_SEEDS =
            ITEMS.register("garlic_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.GARLIC_CROP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WILD_GARDEN =
            ITEMS.registerSimpleBlockItem(ModBlocks.WILD_GARDEN);

    // ===== Functional Block Items (Phase 12-14)（已迁移到 MaterialItems） =====
    public static final DeferredItem<BlockItem> STONE_MILL = MaterialItems.STONE_MILL;
    public static final DeferredItem<BlockItem> CUTTING_BOARD = MaterialItems.CUTTING_BOARD;
    public static final DeferredItem<BlockItem> COOKING_POT = MaterialItems.COOKING_POT;
    public static final DeferredItem<BlockItem> STOVE = MaterialItems.STOVE;
    public static final DeferredItem<BlockItem> RELOAD_FACILITY = MaterialItems.RELOAD_FACILITY;
    public static final DeferredItem<BlockItem> SHIP_CORE_MODIFIER = MaterialItems.SHIP_CORE_MODIFIER;
    public static final DeferredItem<BlockItem> YUBARI_WATER_BUCKET = MaterialItems.YUBARI_WATER_BUCKET;

    public static final DeferredItem<BlockItem> AMMO_WORKBENCH = MaterialItems.AMMO_WORKBENCH;
    public static final DeferredItem<BlockItem> WEAPON_WORKBENCH = MaterialItems.WEAPON_WORKBENCH;
    public static final DeferredItem<BlockItem> BLUEPRINT_CHEST = MaterialItems.BLUEPRINT_CHEST;

    // ===== Blueprints（已迁移到 MaterialItems） =====
    public static final DeferredItem<Item> MEDIUM_GUN_BLUEPRINT = MaterialItems.MEDIUM_GUN_BLUEPRINT;
    public static final DeferredItem<Item> LARGE_GUN_BLUEPRINT = MaterialItems.LARGE_GUN_BLUEPRINT;
    public static final DeferredItem<Item> CREATIVE_BLUEPRINT = MaterialItems.CREATIVE_BLUEPRINT;

    // ===== Intermediate Products (Phase 13/16)（已迁移到 MaterialItems） =====
    public static final DeferredItem<Item> SAUSAGE = MaterialItems.SAUSAGE;
    public static final DeferredItem<Item> SLICED_SAUSAGE = MaterialItems.SLICED_SAUSAGE;
    public static final DeferredItem<Item> BACON = MaterialItems.BACON;
    public static final DeferredItem<Item> TOAST_BREAD_SLICES = MaterialItems.TOAST_BREAD_SLICES;
    public static final DeferredItem<Item> BEER = MaterialItems.BEER;
    public static final DeferredItem<Item> ROUND_BUN = MaterialItems.ROUND_BUN;

    // ===== Food Items (Phase 16) =====
    private static FoodProperties.Builder fp(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation / (nutrition * 2f))
                .alwaysEdible();
    }

    public static final DeferredItem<ModFoodItem> TOAST_BREAD = ITEMS.register("toast_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(15, 18.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> NAVAL_BAKED_BEANS = ITEMS.register("naval_baked_beans",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> LATIAO = ITEMS.register("latiao",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(2, 2.5f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 2), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> MAPO_TOFU = ITEMS.register("mapo_tofu",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> NAVAL_CURRY = ITEMS.register("naval_curry",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f)
                            .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> FRIED_FISH_AND_CHIPS = ITEMS.register("fried_fish_and_chips",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f)
                            .effect(() -> new MobEffectInstance(MobEffects.JUMP, 3600, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SCONE = ITEMS.register("scone",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 4))));

    public static final DeferredItem<ModFoodItem> APPLE_PIE = ITEMS.register("apple_pie",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> ASSORTED_CHAR_SIU_FRIED_RICE = ITEMS.register("assorted_char_siu_fried_rice",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SALTED_EGG_TOFU = ITEMS.register("salted_egg_tofu",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    public static final DeferredItem<ModFoodItem> SURSTROMMING = ITEMS.register("surstromming",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> AMERICAN_BURGER = ITEMS.register("american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<Item> HOTDOG = ITEMS.register("hotdog",
            () -> new Item(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0), 1.0f)
                            .build())));

    public static final DeferredItem<ModFoodItem> PASTA = ITEMS.register("pasta",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> COOKED_RICE = ITEMS.register("cooked_rice",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BEET_BLOSSOM = ITEMS.register("beet_blossom",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    public static final DeferredItem<ModFoodItem> MISO_SOUP = ITEMS.register("miso_soup",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> BARBECUE = ITEMS.register("barbecue",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BLACK_FOREST_GATEAU = ITEMS.register("black_forest_gateau",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("cake", 3))));

    public static final DeferredItem<ModFoodItem> BLACK_TEA_SANDWICH = ITEMS.register("black_tea_sandwich",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BLACK_TEA_SCONE = ITEMS.register("black_tea_scone",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> BORSCHT = ITEMS.register("borscht",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> BOUILLABAISSE = ITEMS.register("bouillabaisse",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> DELUXE_BAOZI = ITEMS.register("deluxe_baozi",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> DONGPO_PORK = ITEMS.register("dongpo_pork",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> DOUBLE_SHELL_AMERICAN_BURGER = ITEMS.register("double_shell_american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> EGGS_BENEDICT = ITEMS.register("eggs_benedict",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> FRIED_FISH_MISO_SOUP = ITEMS.register("fried_fish_miso_soup",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> MACARON = ITEMS.register("macaron",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> MUSSOLINIS_OO = ITEMS.register("mussolinis_oo",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> NEW_RYE_BREAD = ITEMS.register("new_rye_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> SCHWEINSHAXE = ITEMS.register("schweinshaxe",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA = ITEMS.register("salami_pizza",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> RYE_BREAD = ITEMS.register("rye_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> OKROSHKA = ITEMS.register("okroshka",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> PEA_SOUP_WITH_RYE_BREAD = ITEMS.register("pea_soup_with_rye_bread",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> ROYAL_NAVAL_SALTED_BEEF = ITEMS.register("royal_naval_salted_beef",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> RUSSIAN_DUMPLING = ITEMS.register("russian_dumpling",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SOBA_NOODLE = ITEMS.register("soba_noodle",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> TANGYUAN = ITEMS.register("tangyuan",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> TARTE_TATIN = ITEMS.register("tarte_tatin",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> TEMPURA_SOBA_NOODLE = ITEMS.register("tempura_soba_noodle",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    public static final DeferredItem<ModFoodItem> THURINGER_ROSTBRATWURST_UND_BIER = ITEMS.register("thuringer_rostbratwurst_und_bier",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> THURINGER_ROSTBRATWURST = ITEMS.register("thuringer_rostbratwurst",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> TRIPLE_SHELL_AMERICAN_BURGER = ITEMS.register("triple_shell_american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> VENICE_CUTTLEFISH_NOODLES = ITEMS.register("venice_cuttlefish_noodles",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> WEISSWURST_MIT_DER_BAGEL = ITEMS.register("weisswurst_mit_der_bagel",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> YOKAN = ITEMS.register("yokan",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> YORKSHIRE_PUDDING = ITEMS.register("yorkshire_pudding",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    // ===== Aircraft Squadrons (Phase 18) — 实际定义见 AircraftItems =====
    public static final DeferredItem<AircraftItem> FIGHTER_SQUADRON = AircraftItems.FIGHTER_SQUADRON;
    public static final DeferredItem<AircraftItem> DIVE_BOMBER_SQUADRON = AircraftItems.DIVE_BOMBER_SQUADRON;
    public static final DeferredItem<AircraftItem> XTB2D = AircraftItems.XTB2D;
    public static final DeferredItem<AircraftItem> RECON_SQUADRON = AircraftItems.RECON_SQUADRON;

    // ===== Named Aircraft — 实际定义见 AircraftItems =====
    // --- 鱼雷机 ---
    public static final DeferredItem<AircraftItem> SWORDFISH_TORPEDO = AircraftItems.SWORDFISH_TORPEDO;
    public static final DeferredItem<AircraftItem> SWORDFISH_ASW = AircraftItems.SWORDFISH_ASW;
    public static final DeferredItem<AircraftItem> TBF_TORPEDO = AircraftItems.TBF_TORPEDO;
    public static final DeferredItem<AircraftItem> TBF_ASW = AircraftItems.TBF_ASW;
    public static final DeferredItem<AircraftItem> TENZAN_TORPEDO = AircraftItems.TENZAN_TORPEDO;
    public static final DeferredItem<AircraftItem> TYPE97_TORPEDO = AircraftItems.TYPE97_TORPEDO;
    public static final DeferredItem<AircraftItem> SKY_PIRATE_TORPEDO = AircraftItems.SKY_PIRATE_TORPEDO;
    // --- 俯冲轰炸机 ---
    public static final DeferredItem<AircraftItem> PETREL_BOMBER = AircraftItems.PETREL_BOMBER;
    public static final DeferredItem<AircraftItem> TYPE99_DIVE_BOMBER = AircraftItems.TYPE99_DIVE_BOMBER;
    public static final DeferredItem<AircraftItem> SBD_DAUNTLESS = AircraftItems.SBD_DAUNTLESS;
    public static final DeferredItem<AircraftItem> FIREFLY_AS_MK5 = AircraftItems.FIREFLY_AS_MK5;
    public static final DeferredItem<AircraftItem> SUISEI_BOMBER = AircraftItems.SUISEI_BOMBER;
    // --- 水平轰炸机 ---
    public static final DeferredItem<AircraftItem> SEIUN_BOMBER = AircraftItems.SEIUN_BOMBER;
    public static final DeferredItem<AircraftItem> B25_BOMBER = AircraftItems.B25_BOMBER;
    public static final DeferredItem<AircraftItem> XA2J_BOMBER = AircraftItems.XA2J_BOMBER;
    // --- 战斗机 ---
    public static final DeferredItem<AircraftItem> F6F_HELLCAT_ROCKET = AircraftItems.F6F_HELLCAT_ROCKET;
    public static final DeferredItem<AircraftItem> SEAFIRE = AircraftItems.SEAFIRE;
    public static final DeferredItem<AircraftItem> ZERO_MODEL52 = AircraftItems.ZERO_MODEL52;
    public static final DeferredItem<AircraftItem> F4F_WILDCAT = AircraftItems.F4F_WILDCAT;
    public static final DeferredItem<AircraftItem> F4U_CORSAIR_ICE = AircraftItems.F4U_CORSAIR_ICE;
    public static final DeferredItem<AircraftItem> F4U_CORSAIR = AircraftItems.F4U_CORSAIR;
    public static final DeferredItem<AircraftItem> F2H_BANSHEE = AircraftItems.F2H_BANSHEE;
    // --- 侦察机 ---
    public static final DeferredItem<AircraftItem> TYPE0_RECON = AircraftItems.TYPE0_RECON;
    public static final DeferredItem<AircraftItem> C1_RECON = AircraftItems.C1_RECON;
    public static final DeferredItem<AircraftItem> SAIUN_RECON = AircraftItems.SAIUN_RECON;

    // ===== Aviation Ammo (Phase 18) (extracted to AmmoItems) =====
    public static final DeferredItem<Item> AVIATION_FUEL = AmmoItems.AVIATION_FUEL;
    // Legacy items kept for world compatibility — unified into AERIAL_BOMB below
    @Deprecated public static final DeferredItem<Item> AERIAL_BOMB_SMALL = AmmoItems.AERIAL_BOMB_SMALL;
    @Deprecated public static final DeferredItem<Item> AERIAL_BOMB_MEDIUM = AmmoItems.AERIAL_BOMB_MEDIUM;
    public static final DeferredItem<Item> AERIAL_TORPEDO = AmmoItems.AERIAL_TORPEDO;
    // Unified aerial bomb (replaces small/medium distinction)
    public static final DeferredItem<Item> AERIAL_BOMB = AmmoItems.AERIAL_BOMB;
    // 深水炸弹
    public static final DeferredItem<Item> DEPTH_CHARGE = AmmoItems.DEPTH_CHARGE;
    // Fighter ammo (子弹)
    public static final DeferredItem<Item> FIGHTER_AMMO = AmmoItems.FIGHTER_AMMO;

    // 弹丸渲染用隐藏物品（不加入创造模式标签页）
    public static final DeferredItem<Item> PROJECTILE_BULLET = AmmoItems.PROJECTILE_BULLET;

    // ===== Phase 19: Floating Target（已迁移到 MaterialItems） =====
    public static final DeferredItem<FloatingTargetItem> FLOATING_TARGET = MaterialItems.FLOATING_TARGET;

    // ===== Phase 23: Guidebook（已迁移到 MaterialItems） =====
    public static final DeferredItem<GuidebookItem> GUIDEBOOK = MaterialItems.GUIDEBOOK;

    // ===== Phase 27: Pineapple chain（已迁移到 MaterialItems） =====
    public static final DeferredItem<ItemNameBlockItem> PINEAPPLE_SEED = MaterialItems.PINEAPPLE_SEED;
    public static final DeferredItem<Item> PINEAPPLE = MaterialItems.PINEAPPLE;
    public static final DeferredItem<Item> PINEAPPLE_JUICE = MaterialItems.PINEAPPLE_JUICE;

    // ===== Phase 27: Buff foods =====

    /** 龙田烧 — 装填加速 I × 180s; plate × 2 */
    public static final DeferredItem<ModFoodItem> CHICKEN_TATSUTA = ITEMS.register("chicken_tatsuta",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.RELOAD_BOOST, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 鱼雷果汁 — 饥饿 II 180s + 装填加速 II 300s + 抗火 I 300s; 食用后返还玻璃瓶 */
    public static final DeferredItem<BottleFoodItem> TORPEDO_JUICE = ITEMS.register("torpedo_juice",
            () -> new BottleFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 3600, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.RELOAD_BOOST, 6000, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)
                            .build())));

    /** 炸鱼天妇罗 — 高速规避 I × 180s; plate × 2 */
    public static final DeferredItem<ModFoodItem> TEMPURA = ITEMS.register("tempura",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.EVASION, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 格瓦斯 — 缓慢 I + 缓降 I + 高速规避 II × 120s; 食用后返还玻璃瓶 */
    public static final DeferredItem<BottleFoodItem> KVASS = ITEMS.register("kvass",
            () -> new BottleFoodItem(new Item.Properties()
                    .food(fp(4, 5.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 2400, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(ModMobEffects.EVASION, 2400, 1), 1.0f)
                            .build())));

    // ===== Sonar (extracted to AircraftItems) =====
    public static final DeferredItem<SonarItem> STANDARD_SONAR = AircraftItems.STANDARD_SONAR;
    public static final DeferredItem<SonarItem> IMPROVED_SONAR = AircraftItems.IMPROVED_SONAR;
    public static final DeferredItem<SonarItem> ADVANCED_SONAR = AircraftItems.ADVANCED_SONAR;

    // ===== Engines (extracted to AircraftItems) =====
    public static final DeferredItem<EngineItem> STANDARD_ENGINE = AircraftItems.STANDARD_ENGINE;
    public static final DeferredItem<EngineItem> IMPROVED_ENGINE = AircraftItems.IMPROVED_ENGINE;
    public static final DeferredItem<EngineItem> ADVANCED_ENGINE = AircraftItems.ADVANCED_ENGINE;
    public static final DeferredItem<EngineItem> HIGH_PRESSURE_BOILER = AircraftItems.HIGH_PRESSURE_BOILER;
    public static final DeferredItem<EngineItem> DIESEL_ENGINE = AircraftItems.DIESEL_ENGINE;

    // ===== Torpedo Reload Enhancement (extracted to SpecialtyItems) =====
    public static final DeferredItem<TorpedoReloadItem> TORPEDO_RELOAD = SpecialtyItems.TORPEDO_RELOAD;

    // ===== Torpedo Launchers (extracted to WeaponItems) =====
    public static final DeferredItem<TorpedoLauncherItem> TWIN_TORPEDO_LAUNCHER = WeaponItems.TWIN_TORPEDO_LAUNCHER;
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_TORPEDO_LAUNCHER = WeaponItems.TRIPLE_TORPEDO_LAUNCHER;
    public static final DeferredItem<TorpedoLauncherItem> QUAD_TORPEDO_LAUNCHER = WeaponItems.QUAD_TORPEDO_LAUNCHER;
    public static final DeferredItem<TorpedoLauncherItem> QUINTUPLE_TORPEDO_LAUNCHER = WeaponItems.QUINTUPLE_TORPEDO_LAUNCHER;

    // ===== Depth Charge Launchers (extracted to WeaponItems) =====
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER = WeaponItems.DEPTH_CHARGE_LAUNCHER;
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER_IMPROVED = WeaponItems.DEPTH_CHARGE_LAUNCHER_IMPROVED;
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER_ADVANCED = WeaponItems.DEPTH_CHARGE_LAUNCHER_ADVANCED;

    // ===== Missile / Rocket Ammo (extracted to AmmoItems) =====
    public static final DeferredItem<MissileItem> SY1_MISSILE = AmmoItems.SY1_MISSILE;
    public static final DeferredItem<MissileItem> HARPOON_MISSILE = AmmoItems.HARPOON_MISSILE;
    public static final DeferredItem<MissileItem> TERRIER_MISSILE = AmmoItems.TERRIER_MISSILE;
    public static final DeferredItem<MissileItem> ANTI_AIR_MISSILE = AmmoItems.ANTI_AIR_MISSILE;
    public static final DeferredItem<MissileItem> ROCKET_AMMO = AmmoItems.ROCKET_AMMO;

    // ===== Missile Launchers (extracted to WeaponItems) =====
    public static final DeferredItem<MissileLauncherItem> SY1_LAUNCHER = WeaponItems.SY1_LAUNCHER;
    public static final DeferredItem<MissileLauncherItem> MK14_HARPOON_LAUNCHER = WeaponItems.MK14_HARPOON_LAUNCHER;
    public static final DeferredItem<MissileLauncherItem> TERRIER_LAUNCHER = WeaponItems.TERRIER_LAUNCHER;
    public static final DeferredItem<MissileLauncherItem> SHIP_ROCKET_LAUNCHER = WeaponItems.SHIP_ROCKET_LAUNCHER;
    public static final DeferredItem<MissileLauncherItem> SEA_DART_LAUNCHER = WeaponItems.SEA_DART_LAUNCHER;
    public static final DeferredItem<MissileLauncherItem> SEACAT_LAUNCHER = WeaponItems.SEACAT_LAUNCHER;

    // ===== Dungeon System (v0.0.8) (extracted to SpecialtyItems) =====
    public static final DeferredItem<com.piranport.dungeon.key.DungeonKeyItem> DUNGEON_KEY = SpecialtyItems.DUNGEON_KEY;
    public static final DeferredItem<com.piranport.dungeon.item.TownScrollItem> TOWN_SCROLL = SpecialtyItems.TOWN_SCROLL;
    public static final DeferredItem<BlockItem> DUNGEON_LECTERN = SpecialtyItems.DUNGEON_LECTERN;

    // ===== Skin Cores (extracted to SpecialtyItems) =====
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_4 = SpecialtyItems.SKIN_CORE_4;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_5 = SpecialtyItems.SKIN_CORE_5;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_6 = SpecialtyItems.SKIN_CORE_6;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_7 = SpecialtyItems.SKIN_CORE_7;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_8 = SpecialtyItems.SKIN_CORE_8;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_9 = SpecialtyItems.SKIN_CORE_9;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_10 = SpecialtyItems.SKIN_CORE_10;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_11 = SpecialtyItems.SKIN_CORE_11;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_12 = SpecialtyItems.SKIN_CORE_12;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_13 = SpecialtyItems.SKIN_CORE_13;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_14 = SpecialtyItems.SKIN_CORE_14;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_15 = SpecialtyItems.SKIN_CORE_15;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_16 = SpecialtyItems.SKIN_CORE_16;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_17 = SpecialtyItems.SKIN_CORE_17;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_18 = SpecialtyItems.SKIN_CORE_18;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_19 = SpecialtyItems.SKIN_CORE_19;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_20 = SpecialtyItems.SKIN_CORE_20;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_21 = SpecialtyItems.SKIN_CORE_21;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_22 = SpecialtyItems.SKIN_CORE_22;
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_23 = SpecialtyItems.SKIN_CORE_23;

    // ===== Entity Cores =====
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_SUPPLY =
            ITEMS.register("entity_core_deep_ocean_supply",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_SUPPLY));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_ARCHIVIST =
            ITEMS.register("entity_core_deep_ocean_archivist",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_ARCHIVIST));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_ENGINEER =
            ITEMS.register("entity_core_deep_ocean_engineer",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_ENGINEER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_NAVIGATOR =
            ITEMS.register("entity_core_deep_ocean_navigator",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_NAVIGATOR));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_QUARTERMASTER =
            ITEMS.register("entity_core_deep_ocean_quartermaster",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_QUARTERMASTER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_DESTROYER =
            ITEMS.register("entity_core_deep_ocean_destroyer",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_DESTROYER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_LIGHT_CRUISER =
            ITEMS.register("entity_core_deep_ocean_light_cruiser",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_LIGHT_CRUISER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_HEAVY_CRUISER =
            ITEMS.register("entity_core_deep_ocean_heavy_cruiser",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_HEAVY_CRUISER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_BATTLE_CRUISER =
            ITEMS.register("entity_core_deep_ocean_battle_cruiser",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_BATTLE_CRUISER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_BATTLESHIP =
            ITEMS.register("entity_core_deep_ocean_battleship",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_BATTLESHIP));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_LIGHT_CARRIER =
            ITEMS.register("entity_core_deep_ocean_light_carrier",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_LIGHT_CARRIER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_CARRIER =
            ITEMS.register("entity_core_deep_ocean_carrier",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_CARRIER));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_SUBMARINE =
            ITEMS.register("entity_core_deep_ocean_submarine",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_SUBMARINE));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_FLAGSHIP =
            ITEMS.register("entity_core_deep_ocean_flagship",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.DEEP_OCEAN_FLAGSHIP));
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_SHIP_GIRL =
            ITEMS.register("entity_core_ship_girl",
                    () -> new EntityCoreItem(new Item.Properties().stacksTo(1),
                            EntityCoreDefinitions.SHIP_GIRL));

    // ===== Fuel =====
    public static final DeferredItem<Item> FUEL =
            ITEMS.registerSimpleItem("fuel");

    // ===== Tools =====
    public static final DeferredItem<UnicornHarpItem> UNICORN_HARP =
            ITEMS.register("unicorn_harp",
                    () -> new UnicornHarpItem(new Item.Properties().stacksTo(1)));

    // ===== 道具 =====
    public static final DeferredItem<Item> ELITE_DAMAGE_CONTROL =
            ITEMS.register("elite_damage_control",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DamageControlItem> DAMAGE_CONTROL =
            ITEMS.register("damage_control",
                    () -> new DamageControlItem(new Item.Properties().stacksTo(1)));

    // ===== Quick Repair =====
    public static final DeferredItem<com.piranport.item.QuickRepairItem> QUICK_REPAIR =
            ITEMS.register("quick_repair",
                    () -> new com.piranport.item.QuickRepairItem(new Item.Properties().stacksTo(1)));

    // ===== Config Inspector =====
    public static final DeferredItem<ConfigInspectorItem> CONFIG_INSPECTOR =
            ITEMS.register("config_inspector",
                    () -> new ConfigInspectorItem(new Item.Properties().stacksTo(1)));

    // ===== Artillery Config Tool =====
    public static final DeferredItem<ArtilleryConfigToolItem> ARTILLERY_CONFIG_TOOL =
            ITEMS.register("artillery_config_tool",
                    () -> new ArtilleryConfigToolItem(new Item.Properties().stacksTo(1)));

    // ===== Smoke Candle =====
    public static final DeferredItem<SmokeCandleItem> SMOKE_CANDLE =
            ITEMS.register("smoke_candle",
                    () -> new SmokeCandleItem(new Item.Properties().stacksTo(1).durability(128)));

    // ===== Flare Launcher =====
    public static final DeferredItem<FlareLauncherItem> FLARE_LAUNCHER =
            ITEMS.register("flare_launcher",
                    () -> new FlareLauncherItem(new Item.Properties().stacksTo(1).durability(4096)));

    // ===== Repair Kit =====
    public static final DeferredItem<RepairKitItem> REPAIR_KIT =
            ITEMS.register("repair_kit",
                    () -> new RepairKitItem(new Item.Properties().stacksTo(1)));

    // ===== Kirin Headband =====
    public static final DeferredItem<KirinHeadbandItem> KIRIN_HEADBAND =
            ITEMS.register("kirin_headband",
                    () -> new KirinHeadbandItem(new Item.Properties().stacksTo(1)));

    // ===== Mysterious Weapon =====
    public static final DeferredItem<MysteriousWeaponItem> MYSTERIOUS_WEAPON =
            ITEMS.register("mysterious_weapon",
                    () -> new MysteriousWeaponItem(new Item.Properties().stacksTo(1).durability(128)));

    // ===== Richelieu's Command Sword =====
    public static final DeferredItem<CommandSwordItem> RICHELIEU_COMMAND_SWORD =
            ITEMS.register("richelieu_command_sword",
                    () -> new CommandSwordItem(new Item.Properties().stacksTo(1)));

    // ===== Ship Girl Contract =====
    public static final DeferredItem<ShipGirlContractItem> SHIP_GIRL_CONTRACT =
            ITEMS.register("ship_girl_contract",
                    () -> new ShipGirlContractItem(new Item.Properties().stacksTo(1),
                            "tooltip.piranport.ship_girl_contract"));

    // ===== Taihou's Umbrella (Shield) =====
    public static final DeferredItem<TaihouUmbrellaItem> TAIHOU_UMBRELLA =
            ITEMS.register("taihou_umbrella",
                    () -> new TaihouUmbrellaItem(new Item.Properties().stacksTo(1)
                            .durability(1520)
                            .attributes(TaihouUmbrellaItem.createAttributes())));

    // ===== Eugen's Ship Shield =====
    public static final DeferredItem<EugenShieldItem> EUGEN_SHIELD =
            ITEMS.register("eugen_shield",
                    () -> new EugenShieldItem(new Item.Properties().stacksTo(1)
                            .durability(1200)
                            .attributes(EugenShieldItem.createAttributes())));

    // ===== Shoukaku's Scythe (策划 §3.5 表 3.5; 深海翔鹤/旗舰掉落) =====
    public static final DeferredItem<ShoukakuScytheItem> SHOUKAKU_SCYTHE =
            ITEMS.register("shoukaku_scythe",
                    () -> new ShoukakuScytheItem(new Item.Properties().stacksTo(1)
                            .durability(1024)));

    // ===== Props Tab Icon =====
    public static final DeferredItem<Item> HENTAI_TROPHY =
            ITEMS.registerSimpleItem("hentai_trophy");

    // ===== Football Superstar Set (足球巨星套装) =====
    public static final DeferredItem<FootballArmorItem> SPIDER_GLOVES =
            ITEMS.register("spider_gloves",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.HELMET,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(10))));
    public static final DeferredItem<FootballArmorItem> BLUE_JERSEY =
            ITEMS.register("blue_jersey",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(10))));
    public static final DeferredItem<FootballArmorItem> RED_BLACK_SOCKS =
            ITEMS.register("red_black_socks",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(10))));
    public static final DeferredItem<FootballArmorItem> MIRACLE_BOOTS =
            ITEMS.register("miracle_boots",
                    () -> new FootballArmorItem(ModArmorMaterials.FOOTBALL,
                            net.minecraft.world.item.ArmorItem.Type.BOOTS,
                            new Item.Properties().durability(
                                    net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(10))));

    // ===== Hatsuyuki's Main Gun (初雪的主炮) =====
    public static final DeferredItem<HatsuyukiMainGunItem> HATSUYUKI_MAIN_GUN =
            ITEMS.register("hatsuyuki_main_gun",
                    () -> new HatsuyukiMainGunItem(new Item.Properties().stacksTo(1)));

    // ===== Gungnir (冈格尼尔) =====
    public static final DeferredItem<GungnirItem> GUNGNIR =
            ITEMS.register("gungnir",
                    () -> new GungnirItem(new Item.Properties()
                            .durability(512)
                            .attributes(GungnirItem.createAttributes())
                            .stacksTo(1)));

    // ===== v0.0.11 Ruins — Reward Items =====
    // Abyssal Report (深海作战档案)
    public static final DeferredItem<AbyssalReportItem> ABYSSAL_REPORT =
            ITEMS.register("abyssal_report",
                    () -> new AbyssalReportItem(new Item.Properties().stacksTo(16),
                            "tooltip.piranport.abyssal_report"));

    // Chaos Shards (无序意志碎片 α~ι)
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ALPHA =
            ITEMS.register("chaos_shard_alpha",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_BETA =
            ITEMS.register("chaos_shard_beta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_GAMMA =
            ITEMS.register("chaos_shard_gamma",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_DELTA =
            ITEMS.register("chaos_shard_delta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_EPSILON =
            ITEMS.register("chaos_shard_epsilon",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ZETA =
            ITEMS.register("chaos_shard_zeta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ETA =
            ITEMS.register("chaos_shard_eta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_THETA =
            ITEMS.register("chaos_shard_theta",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_IOTA =
            ITEMS.register("chaos_shard_iota",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.chaos_shard"));

    // Portal Activation Core (传送门激活核心)
    public static final DeferredItem<TooltipItem> PORTAL_ACTIVATION_CORE =
            ITEMS.register("portal_activation_core",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.portal_activation_core"));

    // National Flags (各国国旗)
    public static final DeferredItem<TooltipItem> FLAG_J =
            ITEMS.register("flag_j",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_E =
            ITEMS.register("flag_e",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_U =
            ITEMS.register("flag_u",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_G =
            ITEMS.register("flag_g",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_F =
            ITEMS.register("flag_f",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_I =
            ITEMS.register("flag_i",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));
    public static final DeferredItem<TooltipItem> FLAG_C =
            ITEMS.register("flag_c",
                    () -> new TooltipItem(new Item.Properties(),
                            "tooltip.piranport.flag"));

    // Experience Shell (经验炮弹)
    public static final DeferredItem<ExperienceShellItem> EXP_SHELL =
            ITEMS.register("exp_shell",
                    () -> new ExperienceShellItem(new Item.Properties(),
                            "tooltip.piranport.exp_shell"));

    // ===== Deep Ocean Spawn Eggs (深海生成蛋) =====
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_SUPPLY_SPAWN_EGG =
            ITEMS.register("deep_ocean_supply_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_SUPPLY,
                            0x2D2D3D, 0x8888AA, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_ARCHIVIST_SPAWN_EGG =
            ITEMS.register("deep_ocean_archivist_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_ARCHIVIST,
                            0x1C2638, 0xB8A6FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_ENGINEER_SPAWN_EGG =
            ITEMS.register("deep_ocean_engineer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_ENGINEER,
                            0x202A2D, 0x66D6C8, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_NAVIGATOR_SPAWN_EGG =
            ITEMS.register("deep_ocean_navigator_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_NAVIGATOR,
                            0x17223D, 0x77B7FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_QUARTERMASTER_SPAWN_EGG =
            ITEMS.register("deep_ocean_quartermaster_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_QUARTERMASTER,
                            0x242735, 0xE3B85A, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_DESTROYER_SPAWN_EGG =
            ITEMS.register("deep_ocean_destroyer_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_DESTROYER,
                            0x2D2D3D, 0xCC4444, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_LIGHT_CRUISER_SPAWN_EGG =
            ITEMS.register("deep_ocean_light_cruiser_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER,
                            0x2D2D3D, 0xDD8844, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_HEAVY_CRUISER_SPAWN_EGG =
            ITEMS.register("deep_ocean_heavy_cruiser_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER,
                            0x2D2D3D, 0xAA6622, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_BATTLE_CRUISER_SPAWN_EGG =
            ITEMS.register("deep_ocean_battle_cruiser_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER,
                            0x2D2D3D, 0x884488, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_BATTLESHIP_SPAWN_EGG =
            ITEMS.register("deep_ocean_battleship_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_BATTLESHIP,
                            0x2D2D3D, 0x444444, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_LIGHT_CARRIER_SPAWN_EGG =
            ITEMS.register("deep_ocean_light_carrier_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER,
                            0x2D2D3D, 0x44AA44, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_CARRIER_SPAWN_EGG =
            ITEMS.register("deep_ocean_carrier_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_CARRIER,
                            0x2D2D3D, 0x2288AA, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_SUBMARINE_SPAWN_EGG =
            ITEMS.register("deep_ocean_submarine_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_SUBMARINE,
                            0x2D2D3D, 0x334466, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_FLAGSHIP_SPAWN_EGG =
            ITEMS.register("deep_ocean_flagship_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.DEEP_OCEAN_FLAGSHIP,
                            0x161624, 0xD9D1FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> SHIP_GIRL_SPAWN_EGG =
            ITEMS.register("ship_girl_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.SHIP_GIRL,
                            0xFFDDCC, 0x4488FF, new Item.Properties()));

    // ===== Phase 28: Shipgirl Food Expansion — Crops (produce) =====
    public static final DeferredItem<Item> LABLAB_BEAN =
            ITEMS.register("lablab_bean", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> ORMOSIA =
            ITEMS.register("ormosia", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> CELERY =
            ITEMS.register("celery", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> RYE =
            ITEMS.registerSimpleItem("rye");
    public static final DeferredItem<Item> PEACH =
            ITEMS.register("peach", () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));

    // ===== Phase 28: Seeds =====
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> LABLAB_BEAN_SEEDS =
            ITEMS.register("lablab_bean_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.LABLAB_BEAN_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> ORMOSIA_SEEDS =
            ITEMS.register("ormosia_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.ORMOSIA_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> CELERY_SEEDS =
            ITEMS.register("celery_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.CELERY_CROP.get(), new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> RYE_SEEDS =
            ITEMS.register("rye_seeds", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.RYE_CROP.get(), new Item.Properties()));

    // ===== Phase 28: Peach tree block items =====
    public static final DeferredItem<BlockItem> PEACH_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEACH_LOG);
    public static final DeferredItem<BlockItem> PEACH_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEACH_LEAVES);
    public static final DeferredItem<BlockItem> PEACH_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.PEACH_SAPLING);

    public static final DeferredItem<BlockItem> MAIDENHAIR_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAIDENHAIR_LOG);
    public static final DeferredItem<BlockItem> MAIDENHAIR_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAIDENHAIR_LEAVES);
    public static final DeferredItem<BlockItem> MAIDENHAIR_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAIDENHAIR_SAPLING);

    public static final DeferredItem<BlockItem> SAGO_PALM_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.SAGO_PALM_LOG);
    public static final DeferredItem<BlockItem> SAGO_PALM_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.SAGO_PALM_LEAVES);
    public static final DeferredItem<BlockItem> SAGO_PALM_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.SAGO_PALM_SAPLING);

    public static final DeferredItem<BlockItem> GARDENIA_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.GARDENIA_LOG);
    public static final DeferredItem<BlockItem> GARDENIA_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.GARDENIA_LEAVES);
    public static final DeferredItem<BlockItem> GARDENIA_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.GARDENIA_SAPLING);

    public static final DeferredItem<BlockItem> CHINESE_PLUM_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHINESE_PLUM_LOG);
    public static final DeferredItem<BlockItem> CHINESE_PLUM_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHINESE_PLUM_LEAVES);
    public static final DeferredItem<BlockItem> CHINESE_PLUM_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHINESE_PLUM_SAPLING);

    public static final DeferredItem<BlockItem> MAPPLE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAPPLE_LOG);
    public static final DeferredItem<BlockItem> MAPPLE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAPPLE_LEAVES);
    public static final DeferredItem<BlockItem> MAPPLE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.MAPPLE_SAPLING);

    public static final DeferredItem<BlockItem> CHORUS_TREE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHORUS_TREE_LOG);
    public static final DeferredItem<BlockItem> CHORUS_TREE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHORUS_TREE_LEAVES);
    public static final DeferredItem<BlockItem> CHORUS_TREE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHORUS_TREE_SAPLING);

    public static final DeferredItem<BlockItem> SLIME_TREE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.SLIME_TREE_LOG);
    public static final DeferredItem<BlockItem> SLIME_TREE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.SLIME_TREE_LEAVES);
    public static final DeferredItem<BlockItem> SLIME_TREE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.SLIME_TREE_SAPLING);

    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_LOG =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_SLIME_TREE_LOG);
    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_SLIME_TREE_LEAVES);
    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_SLIME_TREE_SAPLING);

    // ===== Phase 28: New Ingredients/Condiments =====
    public static final DeferredItem<Item> MILK_ICE_CREAM      = ITEMS.registerSimpleItem("milk_ice_cream");
    public static final DeferredItem<Item> WALNUT              = ITEMS.registerSimpleItem("walnut");
    public static final DeferredItem<Item> WALNUT_POWDER       = ITEMS.registerSimpleItem("walnut_powder");
    public static final DeferredItem<Item> EMBRYO_OF_APPLE_PIE = ITEMS.registerSimpleItem("embryo_of_apple_pie");
    public static final DeferredItem<Item> EMBRYO_OF_SALAMI_PIZZA = ITEMS.registerSimpleItem("embryo_of_salami_pizza");
    public static final DeferredItem<Item> BLACK_PEPPER        = ITEMS.registerSimpleItem("black_pepper");
    public static final DeferredItem<Item> WHITE_PEPPER        = ITEMS.registerSimpleItem("white_pepper");
    public static final DeferredItem<Item> CURRY_POWDER        = ITEMS.registerSimpleItem("curry_powder");
    public static final DeferredItem<Item> GINGER              = ITEMS.registerSimpleItem("ginger");
    public static final DeferredItem<Item> BLACK_TEA           = ITEMS.registerSimpleItem("black_tea");
    public static final DeferredItem<Item> SALAMI              = ITEMS.registerSimpleItem("salami");
    public static final DeferredItem<Item> SLICED_SALAMI       = ITEMS.registerSimpleItem("sliced_salami");
    public static final DeferredItem<Item> ALMOND              = ITEMS.registerSimpleItem("almond");
    public static final DeferredItem<Item> ALMOND_POWDER       = ITEMS.registerSimpleItem("almond_powder");
    public static final DeferredItem<Item> WOODEN_BOWL         = ITEMS.registerSimpleItem("wooden_bowl");
    public static final DeferredItem<Item> WOODEN_BARREL       = ITEMS.registerSimpleItem("wooden_barrel");

    // ===== Phase 28: Intermediate Products =====
    public static final DeferredItem<Item> BEANS_CAN           = ITEMS.register("beans_can",
            () -> new Item(new Item.Properties().food(fp(4, 5f).build())));
    public static final DeferredItem<Item> CATCHUP             = ITEMS.register("catchup",
            () -> new Item(new Item.Properties().food(fp(1, 1.5f).build())));
    public static final DeferredItem<Item> BOLOGNESE           = ITEMS.register("bolognese",
            () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));
    public static final DeferredItem<Item> BAGEL               = ITEMS.register("bagel",
            () -> new Item(new Item.Properties().food(fp(5, 6.3f).build())));
    public static final DeferredItem<Item> LABLAB_SOUP         = ITEMS.register("lablab_soup",
            () -> new Item(new Item.Properties().stacksTo(16).food(fp(4, 5f).build())));
    public static final DeferredItem<Item> ROAST_PASTRY_OF_PIE = ITEMS.register("roast_pastry_of_pie",
            () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));

    // ===== Phase 28: Juices and Jams (bottled, returns glass bottle) =====
    public static final DeferredItem<BottleFoodItem> APPLE_JUICE =
            ITEMS.register("apple_juice", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> APPLE_JAM =
            ITEMS.register("apple_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> WATERMELON_JUICE =
            ITEMS.register("watermelon_juice", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> WATERMELON_JAM =
            ITEMS.register("watermelon_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> PINEAPPLE_JAM =
            ITEMS.register("pineapple_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));
    public static final DeferredItem<BottleFoodItem> CHORUS_FRUIT_JAM =
            ITEMS.register("chorus_fruit_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));

    // ===== Phase 28: New Dishes =====
    /** Taptap冰激凌 — 幸运 I 5min + 抗火 I 2min */
    public static final DeferredItem<ModFoodItem> TAPTAP_ICE_CREAM = ITEMS.register("taptap_ice_cream",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(1, 1f)
                            .effect(() -> new MobEffectInstance(MobEffects.LUCK, 6000, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    /** 合味道 — 生命提升 II × 20min，流浪商人购买 */
    public static final DeferredItem<ModFoodItem> HE_WEI_DAO = ITEMS.register("he_wei_dao",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(12, 16f)
                            .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 24000, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    /** 咸豆花 — 力量 I + 水下呼吸 I × 3min，碗装 */
    public static final DeferredItem<ModFoodItem> SALTY_BEAN_CURD = ITEMS.register("salty_bean_curd",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    /** 盘装皇家海军咸牛肉 — 生命提升 I × 3min，碗右键方块形式获得 */
    public static final DeferredItem<ModFoodItem> PLATED_ROYAL_NAVAL_SALTED_BEEF = ITEMS.register("plated_royal_naval_salted_beef",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    /** 仰望星空派 — 凋零 II 2s + 反胃 IV 14s + 急迫 III 40s */
    public static final DeferredItem<ModFoodItem> STARGAZY_PIE = ITEMS.register("stargazy_pie",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 800, 2), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    /** 鸡蛋三明治 */
    public static final DeferredItem<ModFoodItem> EGG_SANDWICH = ITEMS.register("egg_sandwich",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 培根三明治 */
    public static final DeferredItem<ModFoodItem> BACON_SANDWICH = ITEMS.register("bacon_sandwich",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 切片萨拉米披萨 — 力量 I × 2min */
    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA_PIECES = ITEMS.register("salami_pizza_pieces",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    /** 番茄肉酱意面 — 速度 I × 3min */
    public static final DeferredItem<ModFoodItem> BOLOGNESE_LINGUINE_RECIPE = ITEMS.register("bolognese_linguine_recipe",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
}
