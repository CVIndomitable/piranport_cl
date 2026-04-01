package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.component.AircraftInfo;
import com.piranport.component.PlaceableInfo;
import com.piranport.component.WeaponCategory;
import com.piranport.item.AircraftItem;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.CannonItem;
import com.piranport.item.BottleFoodItem;
import com.piranport.item.ModFoodItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
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

    // ===== Materials =====
    public static final DeferredItem<Item> ALUMINUM_INGOT =
            ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> SALT =
            ITEMS.registerSimpleItem("salt");

    // ===== Ship Cores =====
    public static final DeferredItem<ShipCoreItem> SMALL_SHIP_CORE =
            ITEMS.register("small_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1), ShipCoreItem.ShipType.SMALL));
    public static final DeferredItem<ShipCoreItem> MEDIUM_SHIP_CORE =
            ITEMS.register("medium_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1), ShipCoreItem.ShipType.MEDIUM));
    public static final DeferredItem<ShipCoreItem> LARGE_SHIP_CORE =
            ITEMS.register("large_ship_core",
                    () -> new ShipCoreItem(new Item.Properties().stacksTo(1), ShipCoreItem.ShipType.LARGE));

    // ===== HE Shells =====
    public static final DeferredItem<Item> SMALL_HE_SHELL =
            ITEMS.registerSimpleItem("small_he_shell");
    public static final DeferredItem<Item> MEDIUM_HE_SHELL =
            ITEMS.registerSimpleItem("medium_he_shell");
    public static final DeferredItem<Item> LARGE_HE_SHELL =
            ITEMS.registerSimpleItem("large_he_shell");

    // ===== AP Shells =====
    public static final DeferredItem<Item> SMALL_AP_SHELL =
            ITEMS.registerSimpleItem("small_ap_shell");
    public static final DeferredItem<Item> MEDIUM_AP_SHELL =
            ITEMS.registerSimpleItem("medium_ap_shell");
    public static final DeferredItem<Item> LARGE_AP_SHELL =
            ITEMS.registerSimpleItem("large_ap_shell");

    // ===== Guns =====
    public static final DeferredItem<Item> SMALL_GUN =
            ITEMS.register("small_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)));
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.register("medium_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)));
    public static final DeferredItem<Item> LARGE_GUN =
            ITEMS.register("large_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON)));

    // ===== Torpedo Ammo =====
    public static final DeferredItem<TorpedoItem> TORPEDO_533MM =
            ITEMS.register("torpedo_533mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 533));
    public static final DeferredItem<TorpedoItem> TORPEDO_610MM =
            ITEMS.register("torpedo_610mm",
                    () -> new TorpedoItem(new Item.Properties().stacksTo(16), 610));

    // ===== Armor Plates =====
    public static final DeferredItem<ArmorPlateItem> SMALL_ARMOR_PLATE =
            ITEMS.register("small_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 2, 10));
    public static final DeferredItem<ArmorPlateItem> MEDIUM_ARMOR_PLATE =
            ITEMS.register("medium_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 4, 20));
    public static final DeferredItem<ArmorPlateItem> LARGE_ARMOR_PLATE =
            ITEMS.register("large_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.ARMOR), 6, 30));

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
                                            1200, 8, 0, 24f, 1.4f, 16, AircraftInfo.BombingMode.DIVE))));

    public static final DeferredItem<AircraftItem> TORPEDO_BOMBER_SQUADRON =
            ITEMS.register("torpedo_bomber_squadron",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 6, 0, 30f, 1.2f, 20, AircraftInfo.BombingMode.DIVE))));

    public static final DeferredItem<AircraftItem> LEVEL_BOMBER_SQUADRON =
            ITEMS.register("level_bomber_squadron",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 8, 0, 36f, 1.0f, 24, AircraftInfo.BombingMode.LEVEL))));

    public static final DeferredItem<AircraftItem> RECON_SQUADRON =
            ITEMS.register("recon_squadron",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.RECON,
                                            1500, 0, 0, 0f, 1.5f, 8, AircraftInfo.BombingMode.DIVE))));

    // ===== Aviation Ammo (Phase 18) =====
    public static final DeferredItem<Item> AVIATION_FUEL =
            ITEMS.registerSimpleItem("aviation_fuel");
    public static final DeferredItem<Item> AERIAL_BOMB_SMALL =
            ITEMS.registerSimpleItem("aerial_bomb_small");
    public static final DeferredItem<Item> AERIAL_BOMB_MEDIUM =
            ITEMS.registerSimpleItem("aerial_bomb_medium");
    public static final DeferredItem<Item> AERIAL_TORPEDO =
            ITEMS.registerSimpleItem("aerial_torpedo");
    // Unified aerial bomb (replaces small/medium distinction)
    public static final DeferredItem<Item> AERIAL_BOMB =
            ITEMS.registerSimpleItem("aerial_bomb");
    // Fighter ammo (子弹)
    public static final DeferredItem<Item> FIGHTER_AMMO =
            ITEMS.registerSimpleItem("fighter_ammo");

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
}
