package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FuelData;
import com.piranport.component.PlaceableInfo;
import com.piranport.component.WeaponCategory;
import com.piranport.item.AircraftItem;
import com.piranport.item.AmmoItem;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.CannonItem;
import com.piranport.item.BottleFoodItem;
import com.piranport.item.ModFoodItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.SkinCoreItem;
import com.piranport.item.SonarItem;
import com.piranport.item.EngineItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.item.TorpedoReloadItem;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.FlareLauncherItem;
import com.piranport.item.DamageControlItem;
import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.SmokeCandleItem;
import com.piranport.item.UnicornHarpItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PiranPort.MOD_ID);

    // ===== Block Items =====
    public static final DeferredItem<BlockItem> BAUXITE_ORE =
            ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE);
    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.ALUMINUM_BLOCK);
    public static final DeferredItem<BlockItem> SALT_BLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.SALT_BLOCK);
    public static final DeferredItem<BlockItem> SALT_CHIP =
            ITEMS.registerSimpleBlockItem(ModBlocks.SALT_CHIP);

    // ===== Tab Icon (no components, no bar) =====
    public static final DeferredItem<Item> TAB_ICON =
            ITEMS.registerSimpleItem("tab_icon");

    // ===== Materials =====
    public static final DeferredItem<Item> RAW_ALUMINUM =
            ITEMS.registerSimpleItem("raw_aluminum");
    public static final DeferredItem<Item> ALUMINUM_INGOT =
            ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> SALT =
            ITEMS.registerSimpleItem("salt");

    // ===== Ship Cores =====
    public static final DeferredItem<ShipCoreItem> SMALL_SHIP_CORE =
            ITEMS.register("small_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipCoreItem.ShipType.SMALL.fuelCapacity)),
                            ShipCoreItem.ShipType.SMALL));
    public static final DeferredItem<ShipCoreItem> MEDIUM_SHIP_CORE =
            ITEMS.register("medium_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipCoreItem.ShipType.MEDIUM.fuelCapacity)),
                            ShipCoreItem.ShipType.MEDIUM));
    public static final DeferredItem<ShipCoreItem> LARGE_SHIP_CORE =
            ITEMS.register("large_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipCoreItem.ShipType.LARGE.fuelCapacity)),
                            ShipCoreItem.ShipType.LARGE));
    public static final DeferredItem<ShipCoreItem> SUBMARINE_CORE =
            ITEMS.register("submarine_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.SHIP_CORE_FUEL.get(),
                                    new FuelData(0, ShipCoreItem.ShipType.SUBMARINE.fuelCapacity)),
                            ShipCoreItem.ShipType.SUBMARINE));

    // ===== HE Shells =====
    public static final DeferredItem<Item> SMALL_HE_SHELL =
            ITEMS.register("small_he_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    public static final DeferredItem<Item> MEDIUM_HE_SHELL =
            ITEMS.register("medium_he_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));
    public static final DeferredItem<Item> LARGE_HE_SHELL =
            ITEMS.register("large_he_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.he_shell"));

    // ===== AP Shells =====
    public static final DeferredItem<Item> SMALL_AP_SHELL =
            ITEMS.register("small_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    public static final DeferredItem<Item> MEDIUM_AP_SHELL =
            ITEMS.register("medium_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));
    public static final DeferredItem<Item> LARGE_AP_SHELL =
            ITEMS.register("large_ap_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.ap_shell"));

    // ===== VT Shells (proximity fuze, small caliber only) =====
    public static final DeferredItem<Item> SMALL_VT_SHELL =
            ITEMS.register("small_vt_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.vt_shell"));

    // ===== Type 3 (Sanshiki) Shells =====
    public static final DeferredItem<Item> SMALL_TYPE3_SHELL =
            ITEMS.register("small_type3_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    public static final DeferredItem<Item> MEDIUM_TYPE3_SHELL =
            ITEMS.register("medium_type3_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));
    public static final DeferredItem<Item> LARGE_TYPE3_SHELL =
            ITEMS.register("large_type3_shell",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.type3_shell"));

    // ===== Guns =====
    public static final DeferredItem<Item> SMALL_GUN =
            ITEMS.register("small_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 6f, 30));
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.register("medium_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 12f, 50));
    public static final DeferredItem<Item> LARGE_GUN =
            ITEMS.register("large_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 20f, 80));

    // ===== Torpedo Ammo (legacy generic) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM =
            ITEMS.register("torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM =
            ITEMS.register("torpedo_610mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 610));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM =
            ITEMS.register("magnetic_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, true));
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM =
            ITEMS.register("wire_guided_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, true));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM =
            ITEMS.register("acoustic_torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533, false, false, true));

    // ===== Torpedo Ammo (named variants) =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_G7A =
            ITEMS.register("torpedo_533mm_g7a",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 27f, 18, 0.817f, false, false, false));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7A =
            ITEMS.register("magnetic_torpedo_533mm_g7a",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 27f, 18, 0.817f, true, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK17 =
            ITEMS.register("torpedo_533mm_mk17",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 55.5f, 49, 0.854f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE91 =
            ITEMS.register("torpedo_610mm_type91",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 49.5f, 30, 0.743f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK1 =
            ITEMS.register("torpedo_610mm_type93_mk1",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 49.5f, 60, 0.929f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE93_MK3 =
            ITEMS.register("torpedo_610mm_type93_mk3",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 78f, 90, 0.706f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_720MM_TYPE0 =
            ITEMS.register("torpedo_720mm_type0",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            720, 55.5f, 70, 0.743f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK14 =
            ITEMS.register("torpedo_533mm_mk14",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 25, 0.576f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM_MK16 =
            ITEMS.register("torpedo_533mm_mk16",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 55.5f, 47, 0.854f, false, false, false));
    public static final DeferredItem<TorpedoItem> MAGNETIC_TORPEDO_533MM_G7E =
            ITEMS.register("magnetic_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 25, 0.669f, true, false, false));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_G7E =
            ITEMS.register("acoustic_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 17, 0.446f, false, false, true));
    public static final DeferredItem<TorpedoItem> WIRE_GUIDED_TORPEDO_533MM_G7E =
            ITEMS.register("wire_guided_torpedo_533mm_g7e",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 21f, 15, 0.557f, false, true, false));
    public static final DeferredItem<TorpedoItem> ACOUSTIC_TORPEDO_533MM_MK27 =
            ITEMS.register("acoustic_torpedo_533mm_mk27",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            533, 24f, 25, 0.669f, false, false, true));
    public static final DeferredItem<TorpedoItem> TORPEDO_530MM_TYPE95 =
            ITEMS.register("torpedo_530mm_type95",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            530, 39f, 23, 0.854f, false, false, false));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM_TYPE95_MK2 =
            ITEMS.register("torpedo_610mm_type95_mk2",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16),
                            610, 55.5f, 45, 0.929f, false, false, false));

    // ===== Armor Plates =====
    public static final DeferredItem<ArmorPlateItem> SMALL_ARMOR_PLATE =
            ITEMS.register("small_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 2, 10, 3));
    public static final DeferredItem<ArmorPlateItem> MEDIUM_ARMOR_PLATE =
            ITEMS.register("medium_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 4, 20, 6));
    public static final DeferredItem<ArmorPlateItem> LARGE_ARMOR_PLATE =
            ITEMS.register("large_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 6, 30, 9));

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

    // ===== Functional Block Items (Phase 12-14) =====
    public static final DeferredItem<BlockItem> STONE_MILL =
            ITEMS.registerSimpleBlockItem(ModBlocks.STONE_MILL);
    public static final DeferredItem<BlockItem> CUTTING_BOARD =
            ITEMS.registerSimpleBlockItem(ModBlocks.CUTTING_BOARD);
    public static final DeferredItem<BlockItem> COOKING_POT =
            ITEMS.registerSimpleBlockItem(ModBlocks.COOKING_POT);
    public static final DeferredItem<BlockItem> RELOAD_FACILITY =
            ITEMS.registerSimpleBlockItem(ModBlocks.RELOAD_FACILITY);
    public static final DeferredItem<BlockItem> YUBARI_WATER_BUCKET =
            ITEMS.registerSimpleBlockItem(ModBlocks.YUBARI_WATER_BUCKET);

    public static final DeferredItem<BlockItem> AMMO_WORKBENCH =
            ITEMS.registerSimpleBlockItem(ModBlocks.AMMO_WORKBENCH);
    public static final DeferredItem<BlockItem> WEAPON_WORKBENCH =
            ITEMS.registerSimpleBlockItem(ModBlocks.WEAPON_WORKBENCH);

    // ===== Blueprints =====
    public static final DeferredItem<Item> MEDIUM_GUN_BLUEPRINT =
            ITEMS.registerSimpleItem("medium_gun_blueprint", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LARGE_GUN_BLUEPRINT =
            ITEMS.registerSimpleItem("large_gun_blueprint", new Item.Properties().stacksTo(1));

    // ===== Intermediate Products (Phase 13/16) =====
    public static final DeferredItem<Item> SAUSAGE            = ITEMS.registerSimpleItem("sausage");
    public static final DeferredItem<Item> SLICED_SAUSAGE     = ITEMS.registerSimpleItem("sliced_sausage");
    public static final DeferredItem<Item> BACON              = ITEMS.registerSimpleItem("bacon");
    public static final DeferredItem<Item> TOAST_BREAD_SLICES = ITEMS.registerSimpleItem("toast_bread_slices");
    public static final DeferredItem<Item> BEER               = ITEMS.registerSimpleItem("beer");
    public static final DeferredItem<Item> ROUND_BUN          = ITEMS.registerSimpleItem("round_bun");

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
                            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 2), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> MAPO_TOFU = ITEMS.register("mapo_tofu",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> NAVAL_CURRY = ITEMS.register("naval_curry",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f)
                            .effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 3))));

    public static final DeferredItem<ModFoodItem> FRIED_FISH_AND_CHIPS = ITEMS.register("fried_fish_and_chips",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6.3f)
                            .effect(new MobEffectInstance(MobEffects.JUMP, 3600, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SCONE = ITEMS.register("scone",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 4))));

    public static final DeferredItem<ModFoodItem> SALTED_EGG_TOFU = ITEMS.register("salted_egg_tofu",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    public static final DeferredItem<ModFoodItem> SURSTROMMING = ITEMS.register("surstromming",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.CONFUSION, 280, 3), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> AMERICAN_BURGER = ITEMS.register("american_burger",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 1), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<Item> HOTDOG = ITEMS.register("hotdog",
            () -> new Item(new Item.Properties()
                    .food(fp(4, 5f)
                            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0), 1.0f)
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
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 1))));

    public static final DeferredItem<ModFoodItem> MISO_SOUP = ITEMS.register("miso_soup",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("bowl", 2))));

    // ===== Aircraft Squadrons (Phase 18) =====
    public static final DeferredItem<AircraftItem> FIGHTER_SQUADRON =
            ITEMS.register("fighter_squadron",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 18f, 1.8f, 12, AircraftInfo.BombingMode.DIVE))));

    public static final DeferredItem<AircraftItem> DIVE_BOMBER_SQUADRON =
            ITEMS.register("dive_bomber_squadron",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 24f, 1.4f, 16, AircraftInfo.BombingMode.DIVE))));

    public static final DeferredItem<AircraftItem> XTB2D =
            ITEMS.register("xtb2d",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 4, 0, 30f, 1.2f, 22, AircraftInfo.BombingMode.DIVE))));


    public static final DeferredItem<AircraftItem> RECON_SQUADRON =
            ITEMS.register("recon_squadron",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            1500, 0, 0, 0f, 1.5f, 8, AircraftInfo.BombingMode.DIVE))));

    // ===== Named Aircraft =====

    // --- 鱼雷机 ---
    public static final DeferredItem<AircraftItem> SWORDFISH_TORPEDO =
            ITEMS.register("swordfish_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 18f, 0.8f, 18, AircraftInfo.BombingMode.DIVE))));

    /** 剑鱼（反潜）— 6×深弹8, HP4, 52节 */
    public static final DeferredItem<AircraftItem> SWORDFISH_ASW =
            ITEMS.register("swordfish_asw",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 6, 0, 8f, 0.8f, 16, AircraftInfo.BombingMode.DIVE))));

    /** TBF（鱼雷）— 1×533鱼雷21, HP5, 56节 */
    public static final DeferredItem<AircraftItem> TBF_TORPEDO =
            ITEMS.register("tbf_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 21f, 0.9f, 20, AircraftInfo.BombingMode.DIVE))));

    /** TBF（反潜）— 4×深弹8, HP5, 56节 */
    public static final DeferredItem<AircraftItem> TBF_ASW =
            ITEMS.register("tbf_asw",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 4, 0, 8f, 0.9f, 18, AircraftInfo.BombingMode.DIVE))));

    /** 天山（鱼雷）— 610鱼雷24, HP4, 64�� */
    public static final DeferredItem<AircraftItem> TENZAN_TORPEDO =
            ITEMS.register("tenzan_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 24f, 1.1f, 20, AircraftInfo.BombingMode.DIVE))));

    /** 九七舰攻（鱼雷）— 610鱼雷21, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TYPE97_TORPEDO =
            ITEMS.register("type97_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 1, 0, 21f, 1.1f, 18, AircraftInfo.BombingMode.DIVE))));

    /** 空中海盗（鱼雷）— 4×533鱼雷12, HP9, 64节 */
    public static final DeferredItem<AircraftItem> SKY_PIRATE_TORPEDO =
            ITEMS.register("sky_pirate_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 4, 0, 12f, 1.1f, 22, AircraftInfo.BombingMode.DIVE))));

    // --- 俯冲轰炸机 ---
    /** 海燕（轰炸）— 1/咬+俯冲轰炸10, HP4, 56节 */
    public static final DeferredItem<AircraftItem> PETREL_BOMBER =
            ITEMS.register("petrel_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 10f, 0.9f, 14, AircraftInfo.BombingMode.DIVE))));

    /** 九九舰爆（轰炸）— 俯冲轰炸12, HP4, 64节 */
    public static final DeferredItem<AircraftItem> TYPE99_DIVE_BOMBER =
            ITEMS.register("type99_dive_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 12f, 1.1f, 16, AircraftInfo.BombingMode.DIVE))));

    /** SBD（轰炸）— 1/咬+俯冲轰炸12, HP5, 64节 */
    public static final DeferredItem<AircraftItem> SBD_DAUNTLESS =
            ITEMS.register("sbd_dauntless",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 12f, 1.1f, 16, AircraftInfo.BombingMode.DIVE))));

    /** 萤火虫AS.MK5（轰炸）— 2/咬+俯冲轰炸14, HP5, 64节 */
    public static final DeferredItem<AircraftItem> FIREFLY_AS_MK5 =
            ITEMS.register("firefly_as_mk5",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 14f, 1.1f, 18, AircraftInfo.BombingMode.DIVE))));

    /** 彗星（轰炸）— 俯冲轰炸18, HP4, 72节 */
    public static final DeferredItem<AircraftItem> SUISEI_BOMBER =
            ITEMS.register("suisei_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.DIVE_BOMBER,
                                            1200, 1, 0, 18f, 1.3f, 16, AircraftInfo.BombingMode.DIVE))));

    // --- 水平轰炸机 ---
    /** 景云（轰炸）— 水平轰炸26, HP6, 68节 */
    public static final DeferredItem<AircraftItem> SEIUN_BOMBER =
            ITEMS.register("seiun_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 26f, 1.2f, 16, AircraftInfo.BombingMode.LEVEL))));

    /** B25（轰炸）— 水平轰炸30, HP15, 64节 */
    public static final DeferredItem<AircraftItem> B25_BOMBER =
            ITEMS.register("b25_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 30f, 1.1f, 24, AircraftInfo.BombingMode.LEVEL))));

    /** XA2J（轰炸）— 水平轰炸46, HP15, 72节 */
    public static final DeferredItem<AircraftItem> XA2J_BOMBER =
            ITEMS.register("xa2j_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 46f, 1.3f, 20, AircraftInfo.BombingMode.LEVEL))));

    // --- 战斗机 ---
    /** F6F地狱猫（火箭弹）— 2/咬+6×火箭弹6, HP6, 72节 */
    public static final DeferredItem<AircraftItem> F6F_HELLCAT_ROCKET =
            ITEMS.register("f6f_hellcat_rocket",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 6, 0, 6f, 1.3f, 14, AircraftInfo.BombingMode.DIVE))));

    /** 海喷火 — 3/咬, HP5, 80节 */
    public static final DeferredItem<AircraftItem> SEAFIRE =
            ITEMS.register("seafire",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 3f, 1.5f, 12, AircraftInfo.BombingMode.DIVE))));

    /** 零战五二型 — 2/咬, HP5, 80节 */
    public static final DeferredItem<AircraftItem> ZERO_MODEL52 =
            ITEMS.register("zero_model52",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 2f, 1.5f, 10, AircraftInfo.BombingMode.DIVE))));

    /** F4F野猫 — 2/咬, HP5, 72节 */
    public static final DeferredItem<AircraftItem> F4F_WILDCAT =
            ITEMS.register("f4f_wildcat",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 2f, 1.3f, 12, AircraftInfo.BombingMode.DIVE))));

    /** F4U冰激凌 — 无伤害, HP5, 航速暂无 */
    public static final DeferredItem<AircraftItem> F4U_CORSAIR_ICE =
            ITEMS.register("f4u_corsair_ice",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 0, 0, 0f, 1.0f, 14, AircraftInfo.BombingMode.DIVE))));

    /** F4U海盗 — 3/咬+6×火箭弹6, HP5, 80节 */
    public static final DeferredItem<AircraftItem> F4U_CORSAIR =
            ITEMS.register("f4u_corsair",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 3f, 1.5f, 14, AircraftInfo.BombingMode.DIVE))));

    /** F2H女妖 — 5/咬, HP8, 100节 */
    public static final DeferredItem<AircraftItem> F2H_BANSHEE =
            ITEMS.register("f2h_banshee",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 64, 0, 5f, 2.0f, 14, AircraftInfo.BombingMode.DIVE))));

    // --- 侦察机 ---
    /** 零式水侦 — HP4, 航程10240, 160节 */
    public static final DeferredItem<AircraftItem> TYPE0_RECON =
            ITEMS.register("type0_recon",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            10240, 0, 0, 0f, 3.5f, 8, AircraftInfo.BombingMode.DIVE))));

    /** C-1侦察机 — HP5, 航程12800, 120节 */
    public static final DeferredItem<AircraftItem> C1_RECON =
            ITEMS.register("c1_recon",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            12800, 0, 0, 0f, 2.5f, 8, AircraftInfo.BombingMode.DIVE))));

    /** 彩云舰侦 — HP5, 航程25600, 200节 */
    public static final DeferredItem<AircraftItem> SAIUN_RECON =
            ITEMS.register("saiun_recon",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            25600, 0, 0, 0f, 4.5f, 8, AircraftInfo.BombingMode.DIVE))));

    // ===== Aviation Ammo (Phase 18) =====
    public static final DeferredItem<Item> AVIATION_FUEL =
            ITEMS.register("aviation_fuel",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aviation_fuel"));
    // Legacy items kept for world compatibility — unified into AERIAL_BOMB below
    @Deprecated public static final DeferredItem<Item> AERIAL_BOMB_SMALL =
            ITEMS.register("aerial_bomb_small",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    @Deprecated public static final DeferredItem<Item> AERIAL_BOMB_MEDIUM =
            ITEMS.register("aerial_bomb_medium",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    public static final DeferredItem<Item> AERIAL_TORPEDO =
            ITEMS.register("aerial_torpedo",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_torpedo"));
    // Unified aerial bomb (replaces small/medium distinction)
    public static final DeferredItem<Item> AERIAL_BOMB =
            ITEMS.register("aerial_bomb",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    // 深水炸弹
    public static final DeferredItem<Item> DEPTH_CHARGE =
            ITEMS.register("depth_charge",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.depth_charge"));
    // Fighter ammo (子弹)
    public static final DeferredItem<Item> FIGHTER_AMMO =
            ITEMS.register("fighter_ammo",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.fighter_ammo"));

    // ===== Phase 19: Floating Target =====
    public static final DeferredItem<com.piranport.item.FloatingTargetItem> FLOATING_TARGET =
            ITEMS.register("floating_target",
                    () -> new com.piranport.item.FloatingTargetItem(new Item.Properties().stacksTo(16)));

    // ===== Phase 23: Guidebook =====
    public static final DeferredItem<com.piranport.item.GuidebookItem> GUIDEBOOK =
            ITEMS.register("guidebook",
                    () -> new com.piranport.item.GuidebookItem(new Item.Properties().stacksTo(1)));

    // ===== Phase 27: Pineapple chain =====
    public static final DeferredItem<net.minecraft.world.item.ItemNameBlockItem> PINEAPPLE_SEED =
            ITEMS.register("pineapple_seed", () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.PINEAPPLE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> PINEAPPLE =
            ITEMS.register("pineapple",
                    () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));
    public static final DeferredItem<Item> PINEAPPLE_JUICE =
            ITEMS.register("pineapple_juice",
                    () -> new Item(new Item.Properties().food(fp(2, 2.5f).build())));

    // ===== Phase 27: Buff foods =====

    /** 龙田烧 — 装填加速 I × 180s; plate × 2 */
    public static final DeferredItem<ModFoodItem> CHICKEN_TATSUTA = ITEMS.register("chicken_tatsuta",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(new MobEffectInstance(ModMobEffects.RELOAD_BOOST, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 鱼雷果汁 — 饥饿 II 180s + 装填加速 II 300s + 抗火 I 300s; 食用后返还玻璃瓶 */
    public static final DeferredItem<BottleFoodItem> TORPEDO_JUICE = ITEMS.register("torpedo_juice",
            () -> new BottleFoodItem(new Item.Properties()
                    .food(fp(3, 3.8f)
                            .effect(new MobEffectInstance(MobEffects.HUNGER, 3600, 1), 1.0f)
                            .effect(new MobEffectInstance(ModMobEffects.RELOAD_BOOST, 6000, 1), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)
                            .build())));

    /** 炸鱼天妇罗 — 高速规避 I × 180s; plate × 2 */
    public static final DeferredItem<ModFoodItem> TEMPURA = ITEMS.register("tempura",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(6, 7.5f)
                            .effect(new MobEffectInstance(ModMobEffects.EVASION, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    /** 格瓦斯 — 缓慢 I + 缓降 I + 高速规避 II × 120s; 食用后返还玻璃瓶 */
    public static final DeferredItem<BottleFoodItem> KVASS = ITEMS.register("kvass",
            () -> new BottleFoodItem(new Item.Properties()
                    .food(fp(4, 5.0f)
                            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 0), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2400, 0), 1.0f)
                            .effect(new MobEffectInstance(ModMobEffects.EVASION, 2400, 1), 1.0f)
                            .build())));

    // ===== Sonar =====
    public static final DeferredItem<SonarItem> STANDARD_SONAR =
            ITEMS.register("standard_sonar",
                    () -> new SonarItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 10));

    // ===== Engines =====
    public static final DeferredItem<EngineItem> STANDARD_ENGINE =
            ITEMS.register("standard_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.05, 5));
    public static final DeferredItem<EngineItem> IMPROVED_ENGINE =
            ITEMS.register("improved_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.10, 10));
    public static final DeferredItem<EngineItem> ADVANCED_ENGINE =
            ITEMS.register("advanced_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.15, 15));
    public static final DeferredItem<EngineItem> HIGH_PRESSURE_BOILER =
            ITEMS.register("high_pressure_boiler",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.12, 20));
    public static final DeferredItem<EngineItem> DIESEL_ENGINE =
            ITEMS.register("diesel_engine",
                    () -> new EngineItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ENGINE), 0.08, 2));

    // ===== Torpedo Reload Enhancement =====
    public static final DeferredItem<TorpedoReloadItem> TORPEDO_RELOAD =
            ITEMS.register("torpedo_reload",
                    () -> new TorpedoReloadItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 8));

    // ===== Torpedo Launchers =====
    public static final DeferredItem<TorpedoLauncherItem> TWIN_TORPEDO_LAUNCHER =
            ITEMS.register("twin_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(64)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            533, 2, 100));
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_TORPEDO_LAUNCHER =
            ITEMS.register("triple_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(48)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            533, 3, 100));
    public static final DeferredItem<TorpedoLauncherItem> QUAD_TORPEDO_LAUNCHER =
            ITEMS.register("quad_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(32)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.TORPEDO),
                            610, 4, 120));

    // ===== Depth Charge Launchers =====
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER =
            ITEMS.register("depth_charge_launcher",
                    () -> new DepthChargeLauncherItem(
                            new Item.Properties().stacksTo(1).durability(64)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.DEPTH_CHARGE),
                            1, 60, DepthChargeLauncherItem.SpreadPattern.SINGLE));
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER_IMPROVED =
            ITEMS.register("depth_charge_launcher_improved",
                    () -> new DepthChargeLauncherItem(
                            new Item.Properties().stacksTo(1).durability(48)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.DEPTH_CHARGE),
                            2, 80, DepthChargeLauncherItem.SpreadPattern.FRONT_BACK));
    public static final DeferredItem<DepthChargeLauncherItem> DEPTH_CHARGE_LAUNCHER_ADVANCED =
            ITEMS.register("depth_charge_launcher_advanced",
                    () -> new DepthChargeLauncherItem(
                            new Item.Properties().stacksTo(1).durability(32)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.DEPTH_CHARGE),
                            3, 100, DepthChargeLauncherItem.SpreadPattern.TRIANGLE));

    // ===== Missile / Rocket Ammo =====
    public static final DeferredItem<MissileItem> SY1_MISSILE =
            ITEMS.register("sy1_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            MissileItem.MissileAmmoType.ANTI_SHIP, 30f, 6f));
    public static final DeferredItem<MissileItem> HARPOON_MISSILE =
            ITEMS.register("harpoon_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            MissileItem.MissileAmmoType.ANTI_SHIP, 24f));
    public static final DeferredItem<MissileItem> TERRIER_MISSILE =
            ITEMS.register("terrier_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            MissileItem.MissileAmmoType.ANTI_AIR, 9f));
    public static final DeferredItem<MissileItem> ANTI_AIR_MISSILE =
            ITEMS.register("anti_air_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            MissileItem.MissileAmmoType.ANTI_AIR, 6f));
    public static final DeferredItem<MissileItem> ROCKET_AMMO =
            ITEMS.register("rocket_ammo",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            MissileItem.MissileAmmoType.ROCKET, 6f));

    // ===== Missile Launchers =====
    // 上游一号（反舰导弹）: 伤害30+6穿甲, 连装2, 负重25
    public static final DeferredItem<MissileLauncherItem> SY1_LAUNCHER =
            ITEMS.register("sy1_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP,
                            30f, 6f, 0f, 2, 0,
                            () -> ModItems.SY1_MISSILE.get()));
    // MK14鱼叉（反舰导弹）: 伤害24, 连装4, 负重22
    public static final DeferredItem<MissileLauncherItem> MK14_HARPOON_LAUNCHER =
            ITEMS.register("mk14_harpoon_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP,
                            24f, 0f, 0f, 4, 0,
                            () -> ModItems.HARPOON_MISSILE.get()));
    // 小猎犬（防空导弹）: 伤害9, 冷却60s, 负重14
    public static final DeferredItem<MissileLauncherItem> TERRIER_LAUNCHER =
            ITEMS.register("terrier_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR,
                            9f, 0f, 2.0f, 1, 1200,
                            () -> ModItems.TERRIER_MISSILE.get()));
    // 舰载火箭弹: 伤害6, 连装6, 负重32
    public static final DeferredItem<MissileLauncherItem> SHIP_ROCKET_LAUNCHER =
            ITEMS.register("ship_rocket_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ROCKET,
                            6f, 0f, 2.0f, 6, 0,
                            () -> ModItems.ROCKET_AMMO.get()));
    // 箭型防空导弹（Sea Dart）: 伤害6, 冷却60s, 负重7
    public static final DeferredItem<MissileLauncherItem> SEA_DART_LAUNCHER =
            ITEMS.register("sea_dart_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR,
                            6f, 0f, 1.5f, 1, 1200,
                            () -> ModItems.ANTI_AIR_MISSILE.get()));
    // 海猫防空导弹（Seacat）: 伤害6, 冷却60s, 负重6
    public static final DeferredItem<MissileLauncherItem> SEACAT_LAUNCHER =
            ITEMS.register("seacat_launcher",
                    () -> new MissileLauncherItem(
                            new Item.Properties().stacksTo(1)
                                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.MISSILE),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR,
                            6f, 0f, 1.5f, 1, 1200,
                            () -> ModItems.ANTI_AIR_MISSILE.get()));

    // ===== Dungeon System (v0.0.8) =====
    public static final DeferredItem<com.piranport.dungeon.key.DungeonKeyItem> DUNGEON_KEY =
            ITEMS.register("dungeon_key",
                    () -> new com.piranport.dungeon.key.DungeonKeyItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.DUNGEON_STAGE_ID.get(), "")
                            .component(ModDataComponents.DUNGEON_PROGRESS.get(),
                                    com.piranport.dungeon.key.DungeonProgress.EMPTY)));

    public static final DeferredItem<com.piranport.dungeon.item.TownScrollItem> TOWN_SCROLL =
            ITEMS.register("town_scroll",
                    () -> new com.piranport.dungeon.item.TownScrollItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> DUNGEON_LECTERN =
            ITEMS.registerSimpleBlockItem(ModBlocks.DUNGEON_LECTERN);

    // ===== Skin Cores =====
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_1 =
            ITEMS.register("skin_core_1",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 1));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_2 =
            ITEMS.register("skin_core_2",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 2));
    public static final DeferredItem<SkinCoreItem> SKIN_CORE_3 =
            ITEMS.register("skin_core_3",
                    () -> new SkinCoreItem(new Item.Properties().stacksTo(1), 3));

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

    public static final DeferredItem<com.piranport.item.QuickRepairItem> QUICK_REPAIR =
            ITEMS.register("quick_repair",
                    () -> new com.piranport.item.QuickRepairItem(new Item.Properties().stacksTo(1)));

    // ===== Smoke Candle =====
    public static final DeferredItem<SmokeCandleItem> SMOKE_CANDLE =
            ITEMS.register("smoke_candle",
                    () -> new SmokeCandleItem(new Item.Properties().stacksTo(1).durability(16)));

    // ===== Flare Launcher =====
    public static final DeferredItem<FlareLauncherItem> FLARE_LAUNCHER =
            ITEMS.register("flare_launcher",
                    () -> new FlareLauncherItem(new Item.Properties().stacksTo(1).durability(4096)));

    // ===== Kirin Headband =====
    public static final DeferredItem<KirinHeadbandItem> KIRIN_HEADBAND =
            ITEMS.register("kirin_headband",
                    () -> new KirinHeadbandItem(new Item.Properties().stacksTo(1)));

    // ===== Props Tab Icon =====
    public static final DeferredItem<Item> HENTAI_TROPHY =
            ITEMS.registerSimpleItem("hentai_trophy");
}
