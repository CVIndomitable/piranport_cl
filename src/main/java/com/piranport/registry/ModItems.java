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

    // ===== Food Ingredients (Phase 11a) (extracted to FoodItems) =====
    public static final DeferredItem<Item> FLOUR = FoodItems.FLOUR;
    public static final DeferredItem<Item> RICE_FLOUR = FoodItems.RICE_FLOUR;
    public static final DeferredItem<Item> CHILI_POWDER = FoodItems.CHILI_POWDER;
    public static final DeferredItem<Item> PORK_PASTE = FoodItems.PORK_PASTE;
    public static final DeferredItem<Item> EDIBLE_OIL = FoodItems.EDIBLE_OIL;
    public static final DeferredItem<Item> BUTTER = FoodItems.BUTTER;
    public static final DeferredItem<Item> CREAM = FoodItems.CREAM;
    public static final DeferredItem<Item> SOYBEAN_MILK = FoodItems.SOYBEAN_MILK;
    public static final DeferredItem<Item> TOFU = FoodItems.TOFU;
    public static final DeferredItem<Item> CHEESE = FoodItems.CHEESE;
    public static final DeferredItem<Item> YEAST = FoodItems.YEAST;
    public static final DeferredItem<Item> SOY_SAUCE = FoodItems.SOY_SAUCE;
    public static final DeferredItem<Item> VINEGAR = FoodItems.VINEGAR;
    public static final DeferredItem<Item> COOKING_WINE = FoodItems.COOKING_WINE;
    public static final DeferredItem<Item> MISO = FoodItems.MISO;
    public static final DeferredItem<Item> BRINE = FoodItems.BRINE;
    public static final DeferredItem<Item> PIE_CRUST = FoodItems.PIE_CRUST;
    public static final DeferredItem<Item> RAW_PASTA = FoodItems.RAW_PASTA;
    public static final DeferredItem<Item> FERMENTED_FISH = FoodItems.FERMENTED_FISH;
    public static final DeferredItem<Item> PIZZA_BASE = FoodItems.PIZZA_BASE;
    public static final DeferredItem<Item> GYPSUM_CHIP = FoodItems.GYPSUM_CHIP;
    public static final DeferredItem<Item> QUICKLIME = FoodItems.QUICKLIME;

    // ===== Crop Produce (Phase 11b) (extracted to FoodItems) =====
    public static final DeferredItem<Item> TOMATO = FoodItems.TOMATO;
    public static final DeferredItem<Item> SOYBEAN = FoodItems.SOYBEAN;
    public static final DeferredItem<Item> CHILI = FoodItems.CHILI;
    public static final DeferredItem<Item> LETTUCE = FoodItems.LETTUCE;
    public static final DeferredItem<Item> RICE = FoodItems.RICE;
    public static final DeferredItem<Item> ONION = FoodItems.ONION;
    public static final DeferredItem<Item> GARLIC = FoodItems.GARLIC;

    // ===== Crop Seeds (Phase 11b) (extracted to FoodItems) =====
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> TOMATO_SEEDS = FoodItems.TOMATO_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> SOYBEAN_SEEDS = FoodItems.SOYBEAN_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> CHILI_SEEDS = FoodItems.CHILI_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> ONION_SEEDS = FoodItems.ONION_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> RICE_SEEDS = FoodItems.RICE_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> LETTUCE_SEEDS = FoodItems.LETTUCE_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> GARLIC_SEEDS = FoodItems.GARLIC_SEEDS;
    public static final DeferredItem<BlockItem> WILD_GARDEN = FoodItems.WILD_GARDEN;

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

    // ===== Food Items (Phase 16) (extracted to FoodItems) =====
    public static final DeferredItem<ModFoodItem> TOAST_BREAD = FoodItems.TOAST_BREAD;
    public static final DeferredItem<ModFoodItem> NAVAL_BAKED_BEANS = FoodItems.NAVAL_BAKED_BEANS;
    public static final DeferredItem<ModFoodItem> LATIAO = FoodItems.LATIAO;
    public static final DeferredItem<ModFoodItem> MAPO_TOFU = FoodItems.MAPO_TOFU;
    public static final DeferredItem<ModFoodItem> NAVAL_CURRY = FoodItems.NAVAL_CURRY;
    public static final DeferredItem<ModFoodItem> FRIED_FISH_AND_CHIPS = FoodItems.FRIED_FISH_AND_CHIPS;
    public static final DeferredItem<ModFoodItem> SCONE = FoodItems.SCONE;
    public static final DeferredItem<ModFoodItem> APPLE_PIE = FoodItems.APPLE_PIE;
    public static final DeferredItem<ModFoodItem> ASSORTED_CHAR_SIU_FRIED_RICE = FoodItems.ASSORTED_CHAR_SIU_FRIED_RICE;
    public static final DeferredItem<ModFoodItem> SALTED_EGG_TOFU = FoodItems.SALTED_EGG_TOFU;
    public static final DeferredItem<ModFoodItem> SURSTROMMING = FoodItems.SURSTROMMING;
    public static final DeferredItem<ModFoodItem> AMERICAN_BURGER = FoodItems.AMERICAN_BURGER;
    public static final DeferredItem<Item> HOTDOG = FoodItems.HOTDOG;
    public static final DeferredItem<ModFoodItem> PASTA = FoodItems.PASTA;
    public static final DeferredItem<ModFoodItem> COOKED_RICE = FoodItems.COOKED_RICE;
    public static final DeferredItem<ModFoodItem> BEET_BLOSSOM = FoodItems.BEET_BLOSSOM;
    public static final DeferredItem<ModFoodItem> MISO_SOUP = FoodItems.MISO_SOUP;
    public static final DeferredItem<ModFoodItem> BARBECUE = FoodItems.BARBECUE;
    public static final DeferredItem<ModFoodItem> BLACK_FOREST_GATEAU = FoodItems.BLACK_FOREST_GATEAU;
    public static final DeferredItem<ModFoodItem> BLACK_TEA_SANDWICH = FoodItems.BLACK_TEA_SANDWICH;
    public static final DeferredItem<ModFoodItem> BLACK_TEA_SCONE = FoodItems.BLACK_TEA_SCONE;
    public static final DeferredItem<ModFoodItem> BORSCHT = FoodItems.BORSCHT;
    public static final DeferredItem<ModFoodItem> BOUILLABAISSE = FoodItems.BOUILLABAISSE;
    public static final DeferredItem<ModFoodItem> DELUXE_BAOZI = FoodItems.DELUXE_BAOZI;
    public static final DeferredItem<ModFoodItem> DONGPO_PORK = FoodItems.DONGPO_PORK;
    public static final DeferredItem<ModFoodItem> DOUBLE_SHELL_AMERICAN_BURGER = FoodItems.DOUBLE_SHELL_AMERICAN_BURGER;
    public static final DeferredItem<ModFoodItem> EGGS_BENEDICT = FoodItems.EGGS_BENEDICT;
    public static final DeferredItem<ModFoodItem> FRIED_FISH_MISO_SOUP = FoodItems.FRIED_FISH_MISO_SOUP;
    public static final DeferredItem<ModFoodItem> MACARON = FoodItems.MACARON;
    public static final DeferredItem<ModFoodItem> MUSSOLINIS_OO = FoodItems.MUSSOLINIS_OO;
    public static final DeferredItem<ModFoodItem> NEW_RYE_BREAD = FoodItems.NEW_RYE_BREAD;
    public static final DeferredItem<ModFoodItem> SCHWEINSHAXE = FoodItems.SCHWEINSHAXE;
    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA = FoodItems.SALAMI_PIZZA;
    public static final DeferredItem<ModFoodItem> RYE_BREAD = FoodItems.RYE_BREAD;
    public static final DeferredItem<ModFoodItem> OKROSHKA = FoodItems.OKROSHKA;
    public static final DeferredItem<ModFoodItem> PEA_SOUP_WITH_RYE_BREAD = FoodItems.PEA_SOUP_WITH_RYE_BREAD;
    public static final DeferredItem<ModFoodItem> ROYAL_NAVAL_SALTED_BEEF = FoodItems.ROYAL_NAVAL_SALTED_BEEF;
    public static final DeferredItem<ModFoodItem> RUSSIAN_DUMPLING = FoodItems.RUSSIAN_DUMPLING;
    public static final DeferredItem<ModFoodItem> SOBA_NOODLE = FoodItems.SOBA_NOODLE;
    public static final DeferredItem<ModFoodItem> TANGYUAN = FoodItems.TANGYUAN;
    public static final DeferredItem<ModFoodItem> TARTE_TATIN = FoodItems.TARTE_TATIN;
    public static final DeferredItem<ModFoodItem> TEMPURA_SOBA_NOODLE = FoodItems.TEMPURA_SOBA_NOODLE;
    public static final DeferredItem<ModFoodItem> THURINGER_ROSTBRATWURST_UND_BIER = FoodItems.THURINGER_ROSTBRATWURST_UND_BIER;
    public static final DeferredItem<ModFoodItem> THURINGER_ROSTBRATWURST = FoodItems.THURINGER_ROSTBRATWURST;
    public static final DeferredItem<ModFoodItem> TRIPLE_SHELL_AMERICAN_BURGER = FoodItems.TRIPLE_SHELL_AMERICAN_BURGER;
    public static final DeferredItem<ModFoodItem> VENICE_CUTTLEFISH_NOODLES = FoodItems.VENICE_CUTTLEFISH_NOODLES;
    public static final DeferredItem<ModFoodItem> WEISSWURST_MIT_DER_BAGEL = FoodItems.WEISSWURST_MIT_DER_BAGEL;
    public static final DeferredItem<ModFoodItem> YOKAN = FoodItems.YOKAN;
    public static final DeferredItem<ModFoodItem> YORKSHIRE_PUDDING = FoodItems.YORKSHIRE_PUDDING;

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

    // ===== Phase 27: Buff foods (extracted to FoodItems) =====
    public static final DeferredItem<ModFoodItem> CHICKEN_TATSUTA = FoodItems.CHICKEN_TATSUTA;
    public static final DeferredItem<BottleFoodItem> TORPEDO_JUICE = FoodItems.TORPEDO_JUICE;
    public static final DeferredItem<ModFoodItem> TEMPURA = FoodItems.TEMPURA;
    public static final DeferredItem<BottleFoodItem> KVASS = FoodItems.KVASS;

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

    // ===== Entity Cores (extracted to SpecialtyItems) =====
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_SUPPLY = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_SUPPLY;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_ARCHIVIST = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_ARCHIVIST;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_ENGINEER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_ENGINEER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_NAVIGATOR = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_NAVIGATOR;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_QUARTERMASTER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_QUARTERMASTER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_DESTROYER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_DESTROYER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_LIGHT_CRUISER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_LIGHT_CRUISER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_HEAVY_CRUISER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_HEAVY_CRUISER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_BATTLE_CRUISER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_BATTLE_CRUISER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_BATTLESHIP = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_BATTLESHIP;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_LIGHT_CARRIER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_LIGHT_CARRIER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_CARRIER = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_CARRIER;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_SUBMARINE = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_SUBMARINE;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_DEEP_OCEAN_FLAGSHIP = SpecialtyItems.ENTITY_CORE_DEEP_OCEAN_FLAGSHIP;
    public static final DeferredItem<EntityCoreItem> ENTITY_CORE_SHIP_GIRL = SpecialtyItems.ENTITY_CORE_SHIP_GIRL;

    // ===== Fuel (extracted to SpecialtyItems) =====
    public static final DeferredItem<Item> FUEL = SpecialtyItems.FUEL;

    // ===== Tools (extracted to SpecialtyItems) =====
    public static final DeferredItem<UnicornHarpItem> UNICORN_HARP = SpecialtyItems.UNICORN_HARP;

    // ===== 道具 (extracted to SpecialtyItems) =====
    public static final DeferredItem<Item> ELITE_DAMAGE_CONTROL = SpecialtyItems.ELITE_DAMAGE_CONTROL;
    public static final DeferredItem<DamageControlItem> DAMAGE_CONTROL = SpecialtyItems.DAMAGE_CONTROL;

    // ===== Quick Repair (extracted to SpecialtyItems) =====
    public static final DeferredItem<com.piranport.item.QuickRepairItem> QUICK_REPAIR = SpecialtyItems.QUICK_REPAIR;

    // ===== Config Inspector (extracted to SpecialtyItems) =====
    public static final DeferredItem<ConfigInspectorItem> CONFIG_INSPECTOR = SpecialtyItems.CONFIG_INSPECTOR;

    // ===== Artillery Config Tool (extracted to SpecialtyItems) =====
    public static final DeferredItem<ArtilleryConfigToolItem> ARTILLERY_CONFIG_TOOL = SpecialtyItems.ARTILLERY_CONFIG_TOOL;

    // ===== Smoke Candle (extracted to SpecialtyItems) =====
    public static final DeferredItem<SmokeCandleItem> SMOKE_CANDLE = SpecialtyItems.SMOKE_CANDLE;

    // ===== Flare Launcher (extracted to SpecialtyItems) =====
    public static final DeferredItem<FlareLauncherItem> FLARE_LAUNCHER = SpecialtyItems.FLARE_LAUNCHER;

    // ===== Repair Kit (extracted to SpecialtyItems) =====
    public static final DeferredItem<RepairKitItem> REPAIR_KIT = SpecialtyItems.REPAIR_KIT;

    // ===== Kirin Headband (extracted to SpecialtyItems) =====
    public static final DeferredItem<KirinHeadbandItem> KIRIN_HEADBAND = SpecialtyItems.KIRIN_HEADBAND;

    // ===== Mysterious Weapon (extracted to SpecialtyItems) =====
    public static final DeferredItem<MysteriousWeaponItem> MYSTERIOUS_WEAPON = SpecialtyItems.MYSTERIOUS_WEAPON;

    // ===== Richelieu's Command Sword (extracted to SpecialtyItems) =====
    public static final DeferredItem<CommandSwordItem> RICHELIEU_COMMAND_SWORD = SpecialtyItems.RICHELIEU_COMMAND_SWORD;

    // ===== Ship Girl Contract (extracted to SpecialtyItems) =====
    public static final DeferredItem<ShipGirlContractItem> SHIP_GIRL_CONTRACT = SpecialtyItems.SHIP_GIRL_CONTRACT;

    // ===== Taihou's Umbrella (Shield) (extracted to SpecialtyItems) =====
    public static final DeferredItem<TaihouUmbrellaItem> TAIHOU_UMBRELLA = SpecialtyItems.TAIHOU_UMBRELLA;

    // ===== Eugen's Ship Shield (extracted to SpecialtyItems) =====
    public static final DeferredItem<EugenShieldItem> EUGEN_SHIELD = SpecialtyItems.EUGEN_SHIELD;

    // ===== Shoukaku's Scythe (extracted to SpecialtyItems) =====
    public static final DeferredItem<ShoukakuScytheItem> SHOUKAKU_SCYTHE = SpecialtyItems.SHOUKAKU_SCYTHE;

    // ===== Props Tab Icon (extracted to SpecialtyItems) =====
    public static final DeferredItem<Item> HENTAI_TROPHY = SpecialtyItems.HENTAI_TROPHY;

    // ===== Football Superstar Set (足球巨星套装) (extracted to SpecialtyItems) =====
    public static final DeferredItem<FootballArmorItem> SPIDER_GLOVES = SpecialtyItems.SPIDER_GLOVES;
    public static final DeferredItem<FootballArmorItem> BLUE_JERSEY = SpecialtyItems.BLUE_JERSEY;
    public static final DeferredItem<FootballArmorItem> RED_BLACK_SOCKS = SpecialtyItems.RED_BLACK_SOCKS;
    public static final DeferredItem<FootballArmorItem> MIRACLE_BOOTS = SpecialtyItems.MIRACLE_BOOTS;

    // ===== Hatsuyuki's Main Gun (初雪的主炮) (extracted to SpecialtyItems) =====
    public static final DeferredItem<HatsuyukiMainGunItem> HATSUYUKI_MAIN_GUN = SpecialtyItems.HATSUYUKI_MAIN_GUN;

    // ===== Gungnir (冈格尼尔) (extracted to SpecialtyItems) =====
    public static final DeferredItem<GungnirItem> GUNGNIR = SpecialtyItems.GUNGNIR;

    // ===== v0.0.11 Ruins — Reward Items (extracted to SpecialtyItems) =====
    public static final DeferredItem<AbyssalReportItem> ABYSSAL_REPORT = SpecialtyItems.ABYSSAL_REPORT;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ALPHA = SpecialtyItems.CHAOS_SHARD_ALPHA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_BETA = SpecialtyItems.CHAOS_SHARD_BETA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_GAMMA = SpecialtyItems.CHAOS_SHARD_GAMMA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_DELTA = SpecialtyItems.CHAOS_SHARD_DELTA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_EPSILON = SpecialtyItems.CHAOS_SHARD_EPSILON;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ZETA = SpecialtyItems.CHAOS_SHARD_ZETA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_ETA = SpecialtyItems.CHAOS_SHARD_ETA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_THETA = SpecialtyItems.CHAOS_SHARD_THETA;
    public static final DeferredItem<TooltipItem> CHAOS_SHARD_IOTA = SpecialtyItems.CHAOS_SHARD_IOTA;
    public static final DeferredItem<TooltipItem> PORTAL_ACTIVATION_CORE = SpecialtyItems.PORTAL_ACTIVATION_CORE;
    public static final DeferredItem<TooltipItem> FLAG_J = SpecialtyItems.FLAG_J;
    public static final DeferredItem<TooltipItem> FLAG_E = SpecialtyItems.FLAG_E;
    public static final DeferredItem<TooltipItem> FLAG_U = SpecialtyItems.FLAG_U;
    public static final DeferredItem<TooltipItem> FLAG_G = SpecialtyItems.FLAG_G;
    public static final DeferredItem<TooltipItem> FLAG_F = SpecialtyItems.FLAG_F;
    public static final DeferredItem<TooltipItem> FLAG_I = SpecialtyItems.FLAG_I;
    public static final DeferredItem<TooltipItem> FLAG_C = SpecialtyItems.FLAG_C;
    public static final DeferredItem<ExperienceShellItem> EXP_SHELL = SpecialtyItems.EXP_SHELL;

    // ===== Deep Ocean Spawn Eggs (深海生成蛋) (extracted to SpecialtyItems) =====
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_SUPPLY_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_SUPPLY_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_ARCHIVIST_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_ARCHIVIST_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_ENGINEER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_ENGINEER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_NAVIGATOR_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_NAVIGATOR_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_QUARTERMASTER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_QUARTERMASTER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_DESTROYER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_DESTROYER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_LIGHT_CRUISER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_LIGHT_CRUISER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_HEAVY_CRUISER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_HEAVY_CRUISER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_BATTLE_CRUISER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_BATTLE_CRUISER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_BATTLESHIP_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_BATTLESHIP_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_LIGHT_CARRIER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_LIGHT_CARRIER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_CARRIER_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_CARRIER_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_SUBMARINE_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_SUBMARINE_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> DEEP_OCEAN_FLAGSHIP_SPAWN_EGG = SpecialtyItems.DEEP_OCEAN_FLAGSHIP_SPAWN_EGG;
    public static final DeferredItem<SpawnEggItem> SHIP_GIRL_SPAWN_EGG = SpecialtyItems.SHIP_GIRL_SPAWN_EGG;

    // ===== Phase 28: Shipgirl Food Expansion — Crops (produce) (extracted to FoodItems) =====
    public static final DeferredItem<Item> LABLAB_BEAN = FoodItems.LABLAB_BEAN;
    public static final DeferredItem<Item> ORMOSIA = FoodItems.ORMOSIA;
    public static final DeferredItem<Item> CELERY = FoodItems.CELERY;
    public static final DeferredItem<Item> RYE = FoodItems.RYE;
    public static final DeferredItem<Item> PEACH = FoodItems.PEACH;

    // ===== Phase 28: Seeds (extracted to FoodItems) =====
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> LABLAB_BEAN_SEEDS = FoodItems.LABLAB_BEAN_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> ORMOSIA_SEEDS = FoodItems.ORMOSIA_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> CELERY_SEEDS = FoodItems.CELERY_SEEDS;
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> RYE_SEEDS = FoodItems.RYE_SEEDS;

    // ===== Phase 28: Fruit/Tree block items (extracted to FoodItems) =====
    public static final DeferredItem<BlockItem> PEACH_LOG = FoodItems.PEACH_LOG;
    public static final DeferredItem<BlockItem> PEACH_LEAVES = FoodItems.PEACH_LEAVES;
    public static final DeferredItem<BlockItem> PEACH_SAPLING = FoodItems.PEACH_SAPLING;

    public static final DeferredItem<BlockItem> MAIDENHAIR_LOG = FoodItems.MAIDENHAIR_LOG;
    public static final DeferredItem<BlockItem> MAIDENHAIR_LEAVES = FoodItems.MAIDENHAIR_LEAVES;
    public static final DeferredItem<BlockItem> MAIDENHAIR_SAPLING = FoodItems.MAIDENHAIR_SAPLING;

    public static final DeferredItem<BlockItem> SAGO_PALM_LOG = FoodItems.SAGO_PALM_LOG;
    public static final DeferredItem<BlockItem> SAGO_PALM_LEAVES = FoodItems.SAGO_PALM_LEAVES;
    public static final DeferredItem<BlockItem> SAGO_PALM_SAPLING = FoodItems.SAGO_PALM_SAPLING;

    public static final DeferredItem<BlockItem> GARDENIA_LOG = FoodItems.GARDENIA_LOG;
    public static final DeferredItem<BlockItem> GARDENIA_LEAVES = FoodItems.GARDENIA_LEAVES;
    public static final DeferredItem<BlockItem> GARDENIA_SAPLING = FoodItems.GARDENIA_SAPLING;

    public static final DeferredItem<BlockItem> CHINESE_PLUM_LOG = FoodItems.CHINESE_PLUM_LOG;
    public static final DeferredItem<BlockItem> CHINESE_PLUM_LEAVES = FoodItems.CHINESE_PLUM_LEAVES;
    public static final DeferredItem<BlockItem> CHINESE_PLUM_SAPLING = FoodItems.CHINESE_PLUM_SAPLING;

    public static final DeferredItem<BlockItem> MAPPLE_LOG = FoodItems.MAPPLE_LOG;
    public static final DeferredItem<BlockItem> MAPPLE_LEAVES = FoodItems.MAPPLE_LEAVES;
    public static final DeferredItem<BlockItem> MAPPLE_SAPLING = FoodItems.MAPPLE_SAPLING;

    public static final DeferredItem<BlockItem> CHORUS_TREE_LOG = FoodItems.CHORUS_TREE_LOG;
    public static final DeferredItem<BlockItem> CHORUS_TREE_LEAVES = FoodItems.CHORUS_TREE_LEAVES;
    public static final DeferredItem<BlockItem> CHORUS_TREE_SAPLING = FoodItems.CHORUS_TREE_SAPLING;

    public static final DeferredItem<BlockItem> SLIME_TREE_LOG = FoodItems.SLIME_TREE_LOG;
    public static final DeferredItem<BlockItem> SLIME_TREE_LEAVES = FoodItems.SLIME_TREE_LEAVES;
    public static final DeferredItem<BlockItem> SLIME_TREE_SAPLING = FoodItems.SLIME_TREE_SAPLING;

    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_LOG = FoodItems.LAVA_SLIME_TREE_LOG;
    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_LEAVES = FoodItems.LAVA_SLIME_TREE_LEAVES;
    public static final DeferredItem<BlockItem> LAVA_SLIME_TREE_SAPLING = FoodItems.LAVA_SLIME_TREE_SAPLING;

    // ===== Phase 28: New Ingredients/Condiments (extracted to FoodItems) =====
    public static final DeferredItem<Item> MILK_ICE_CREAM = FoodItems.MILK_ICE_CREAM;
    public static final DeferredItem<Item> WALNUT = FoodItems.WALNUT;
    public static final DeferredItem<Item> WALNUT_POWDER = FoodItems.WALNUT_POWDER;
    public static final DeferredItem<Item> EMBRYO_OF_APPLE_PIE = FoodItems.EMBRYO_OF_APPLE_PIE;
    public static final DeferredItem<Item> EMBRYO_OF_SALAMI_PIZZA = FoodItems.EMBRYO_OF_SALAMI_PIZZA;
    public static final DeferredItem<Item> BLACK_PEPPER = FoodItems.BLACK_PEPPER;
    public static final DeferredItem<Item> WHITE_PEPPER = FoodItems.WHITE_PEPPER;
    public static final DeferredItem<Item> CURRY_POWDER = FoodItems.CURRY_POWDER;
    public static final DeferredItem<Item> GINGER = FoodItems.GINGER;
    public static final DeferredItem<Item> BLACK_TEA = FoodItems.BLACK_TEA;
    public static final DeferredItem<Item> SALAMI = FoodItems.SALAMI;
    public static final DeferredItem<Item> SLICED_SALAMI = FoodItems.SLICED_SALAMI;
    public static final DeferredItem<Item> ALMOND = FoodItems.ALMOND;
    public static final DeferredItem<Item> ALMOND_POWDER = FoodItems.ALMOND_POWDER;
    public static final DeferredItem<Item> WOODEN_BOWL = FoodItems.WOODEN_BOWL;
    public static final DeferredItem<Item> WOODEN_BARREL = FoodItems.WOODEN_BARREL;

    // ===== Phase 28: Intermediate Products (extracted to FoodItems) =====
    public static final DeferredItem<Item> BEANS_CAN = FoodItems.BEANS_CAN;
    public static final DeferredItem<Item> CATCHUP = FoodItems.CATCHUP;
    public static final DeferredItem<Item> BOLOGNESE = FoodItems.BOLOGNESE;
    public static final DeferredItem<Item> BAGEL = FoodItems.BAGEL;
    public static final DeferredItem<Item> LABLAB_SOUP = FoodItems.LABLAB_SOUP;
    public static final DeferredItem<Item> ROAST_PASTRY_OF_PIE = FoodItems.ROAST_PASTRY_OF_PIE;

    // ===== Phase 28: Juices and Jams (bottled, returns glass bottle) (extracted to FoodItems) =====
    public static final DeferredItem<BottleFoodItem> APPLE_JUICE = FoodItems.APPLE_JUICE;
    public static final DeferredItem<BottleFoodItem> APPLE_JAM = FoodItems.APPLE_JAM;
    public static final DeferredItem<BottleFoodItem> WATERMELON_JUICE = FoodItems.WATERMELON_JUICE;
    public static final DeferredItem<BottleFoodItem> WATERMELON_JAM = FoodItems.WATERMELON_JAM;
    public static final DeferredItem<BottleFoodItem> PINEAPPLE_JAM = FoodItems.PINEAPPLE_JAM;
    public static final DeferredItem<BottleFoodItem> CHORUS_FRUIT_JAM = FoodItems.CHORUS_FRUIT_JAM;

    // ===== Phase 28: New Dishes (extracted to FoodItems) =====
    public static final DeferredItem<ModFoodItem> TAPTAP_ICE_CREAM = FoodItems.TAPTAP_ICE_CREAM;
    public static final DeferredItem<ModFoodItem> HE_WEI_DAO = FoodItems.HE_WEI_DAO;
    public static final DeferredItem<ModFoodItem> SALTY_BEAN_CURD = FoodItems.SALTY_BEAN_CURD;
    public static final DeferredItem<ModFoodItem> PLATED_ROYAL_NAVAL_SALTED_BEEF = FoodItems.PLATED_ROYAL_NAVAL_SALTED_BEEF;
    public static final DeferredItem<ModFoodItem> STARGAZY_PIE = FoodItems.STARGAZY_PIE;
    public static final DeferredItem<ModFoodItem> EGG_SANDWICH = FoodItems.EGG_SANDWICH;
    public static final DeferredItem<ModFoodItem> BACON_SANDWICH = FoodItems.BACON_SANDWICH;
    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA_PIECES = FoodItems.SALAMI_PIZZA_PIECES;
    public static final DeferredItem<ModFoodItem> BOLOGNESE_LINGUINE_RECIPE = FoodItems.BOLOGNESE_LINGUINE_RECIPE;
}
