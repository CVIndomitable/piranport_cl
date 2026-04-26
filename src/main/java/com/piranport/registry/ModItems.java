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
import com.piranport.item.FootballArmorItem;
import com.piranport.item.GungnirItem;
import com.piranport.item.HatsuyukiMainGunItem;
import com.piranport.item.CommandSwordItem;
import com.piranport.item.MysteriousWeaponItem;
import com.piranport.item.SmokeCandleItem;
import com.piranport.item.EugenShieldItem;
import com.piranport.item.TaihouUmbrellaItem;
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

    // ===== Decorative Blocks (from sheropshire) =====
    public static final DeferredItem<BlockItem> CONFIDENTIAL_CARGO =
            ITEMS.registerSimpleBlockItem(ModBlocks.CONFIDENTIAL_CARGO);
    public static final DeferredItem<BlockItem> ABYSS_RED_SPIDER_LILY =
            ITEMS.registerSimpleBlockItem(ModBlocks.ABYSS_RED_SPIDER_LILY);
    public static final DeferredItem<BlockItem> ITALIAN_DISH_KIT =
            ITEMS.registerSimpleBlockItem(ModBlocks.ITALIAN_DISH_KIT);
    public static final DeferredItem<BlockItem> B25_MODEL =
            ITEMS.registerSimpleBlockItem(ModBlocks.B25_MODEL);

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
    public static final DeferredItem<Item> SINGLE_SMALL_GUN =
            ITEMS.register("single_small_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 6f, 30, 1));
    public static final DeferredItem<Item> SMALL_GUN =
            ITEMS.register("small_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 6f, 30, 2));
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.register("medium_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 12f, 50, 1));
    public static final DeferredItem<Item> LARGE_GUN =
            ITEMS.register("large_gun", () -> new CannonItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.CANNON), 20f, 80, 3));

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
    public static final DeferredItem<Item> EDIBLE_OIL      = ITEMS.registerSimpleItem("edible_oil");
    public static final DeferredItem<Item> BUTTER          = ITEMS.registerSimpleItem("butter");
    public static final DeferredItem<Item> CREAM           = ITEMS.registerSimpleItem("cream");
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
    public static final DeferredItem<Item> QUICKLIME       = ITEMS.registerSimpleItem("quicklime");

    // ===== Functional Block Items (Phase 12-14) =====
    public static final DeferredItem<BlockItem> RELOAD_FACILITY =
            ITEMS.registerSimpleBlockItem(ModBlocks.RELOAD_FACILITY);

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

    public static final DeferredItem<ModFoodItem> APPLE_PIE = ITEMS.register("apple_pie",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> ASSORTED_CHAR_SIU_FRIED_RICE = ITEMS.register("assorted_char_siu_fried_rice",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

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

    public static final DeferredItem<ModFoodItem> MUSSOLINIS_OO = ITEMS.register("mussolinis_oo",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SCHWEINSHAXE = ITEMS.register("schweinshaxe",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> SALAMI_PIZZA = ITEMS.register("salami_pizza",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    public static final DeferredItem<ModFoodItem> OKROSHKA = ITEMS.register("okroshka",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

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

    public static final DeferredItem<ModFoodItem> YORKSHIRE_PUDDING = ITEMS.register("yorkshire_pudding",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(5, 6f).build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));

    // ===== Named Aircraft =====

    // --- 鱼雷机 ---
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
                                    new AircraftInfo(AircraftInfo.AircraftType.ASW,
                                            1200, 4, 0, 8f, 0.9f, 18, AircraftInfo.BombingMode.LEVEL))));

    /** 空中海盗（鱼雷）— 4×533鱼雷12, HP9, 64节 */
    public static final DeferredItem<AircraftItem> SKY_PIRATE_TORPEDO =
            ITEMS.register("sky_pirate_torpedo",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                                            1200, 4, 0, 12f, 1.1f, 22, AircraftInfo.BombingMode.DIVE))));

    // --- 俯冲轰炸机 ---
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

    // --- 水平轰炸机 ---
    /** B25（轰炸）— 水平轰炸30, HP15, 64节 */
    public static final DeferredItem<AircraftItem> B25_BOMBER =
            ITEMS.register("b25_bomber",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.LEVEL_BOMBER,
                                            1200, 32, 0, 30f, 1.1f, 24, AircraftInfo.BombingMode.LEVEL))));

    // --- 战斗机 ---
    /** F6F地狱猫（火箭弹）— 2/咬+6×火箭弹6, HP6, 72节 */
    public static final DeferredItem<AircraftItem> F6F_HELLCAT_ROCKET =
            ITEMS.register("f6f_hellcat_rocket",
                    () -> new AircraftItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.WEAPON_CATEGORY.get(), WeaponCategory.AIRCRAFT)
                            .component(ModDataComponents.AIRCRAFT_INFO.get(),
                                    new AircraftInfo(AircraftInfo.AircraftType.FIGHTER,
                                            1200, 6, 0, 6f, 1.3f, 14, AircraftInfo.BombingMode.DIVE))));

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

    // ===== Aviation Ammo (Phase 18) =====
    public static final DeferredItem<Item> AVIATION_FUEL =
            ITEMS.register("aviation_fuel",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aviation_fuel"));
    public static final DeferredItem<Item> AERIAL_BOMB =
            ITEMS.register("aerial_bomb",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_bomb"));
    public static final DeferredItem<Item> AERIAL_TORPEDO =
            ITEMS.register("aerial_torpedo",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.aerial_torpedo"));
    // 深水炸弹
    public static final DeferredItem<Item> DEPTH_CHARGE =
            ITEMS.register("depth_charge",
                    () -> new AmmoItem(new Item.Properties(), "tooltip.piranport.ammo_type.depth_charge"));

    // 弹丸渲染用隐藏物品（不加入创造模式标签页）
    public static final DeferredItem<Item> PROJECTILE_BULLET =
            ITEMS.register("projectile_bullet", () -> new Item(new Item.Properties()));

    // ===== Phase 19: Floating Target =====
    public static final DeferredItem<com.piranport.item.FloatingTargetItem> FLOATING_TARGET =
            ITEMS.register("floating_target",
                    () -> new com.piranport.item.FloatingTargetItem(new Item.Properties().stacksTo(16)));

    // ===== Phase 23: Guidebook =====
    public static final DeferredItem<com.piranport.item.GuidebookItem> GUIDEBOOK =
            ITEMS.register("guidebook",
                    () -> new com.piranport.item.GuidebookItem(new Item.Properties().stacksTo(1)));

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

    // ===== Missile / Rocket Ammo =====
    public static final DeferredItem<MissileItem> SY1_MISSILE =
            ITEMS.register("sy1_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP, 30f, 6f));
    public static final DeferredItem<MissileItem> HARPOON_MISSILE =
            ITEMS.register("harpoon_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_SHIP, 24f));
    public static final DeferredItem<MissileItem> TERRIER_MISSILE =
            ITEMS.register("terrier_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR, 9f));
    public static final DeferredItem<MissileItem> ANTI_AIR_MISSILE =
            ITEMS.register("anti_air_missile",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ANTI_AIR, 6f));
    public static final DeferredItem<MissileItem> ROCKET_AMMO =
            ITEMS.register("rocket_ammo",
                    () -> new MissileItem(new Item.Properties().stacksTo(16),
                            com.piranport.entity.MissileEntity.MissileType.ROCKET, 6f));

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

    // ===== Mysterious Weapon =====
    public static final DeferredItem<MysteriousWeaponItem> MYSTERIOUS_WEAPON =
            ITEMS.register("mysterious_weapon",
                    () -> new MysteriousWeaponItem(new Item.Properties().stacksTo(1).durability(128)));

    // ===== Richelieu's Command Sword =====
    public static final DeferredItem<CommandSwordItem> RICHELIEU_COMMAND_SWORD =
            ITEMS.register("richelieu_command_sword",
                    () -> new CommandSwordItem(new Item.Properties().stacksTo(1)));

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

    // ===== Phase 28: New Ingredients/Condiments =====
    public static final DeferredItem<Item> MILK_ICE_CREAM      = ITEMS.registerSimpleItem("milk_ice_cream");
    public static final DeferredItem<Item> EMBRYO_OF_APPLE_PIE = ITEMS.registerSimpleItem("embryo_of_apple_pie");
    public static final DeferredItem<Item> EMBRYO_OF_SALAMI_PIZZA = ITEMS.registerSimpleItem("embryo_of_salami_pizza");
    public static final DeferredItem<Item> BLACK_PEPPER        = ITEMS.registerSimpleItem("black_pepper");
    public static final DeferredItem<Item> WHITE_PEPPER        = ITEMS.registerSimpleItem("white_pepper");
    public static final DeferredItem<Item> GINGER              = ITEMS.registerSimpleItem("ginger");
    public static final DeferredItem<Item> BLACK_TEA           = ITEMS.registerSimpleItem("black_tea");
    public static final DeferredItem<Item> SALAMI              = ITEMS.registerSimpleItem("salami");
    public static final DeferredItem<Item> SLICED_SALAMI       = ITEMS.registerSimpleItem("sliced_salami");
    public static final DeferredItem<Item> WOODEN_BOWL         = ITEMS.registerSimpleItem("wooden_bowl");
    public static final DeferredItem<Item> WOODEN_BARREL       = ITEMS.registerSimpleItem("wooden_barrel");

    // ===== Phase 28: Intermediate Products =====
    public static final DeferredItem<Item> BEANS_CAN           = ITEMS.register("beans_can",
            () -> new Item(new Item.Properties().food(fp(4, 5f).build())));
    public static final DeferredItem<Item> BAGEL               = ITEMS.register("bagel",
            () -> new Item(new Item.Properties().food(fp(5, 6.3f).build())));
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
    public static final DeferredItem<BottleFoodItem> CHORUS_FRUIT_JAM =
            ITEMS.register("chorus_fruit_jam", () -> new BottleFoodItem(
                    new Item.Properties().stacksTo(16).food(fp(2, 2.5f).build())));

    // ===== Phase 28: New Dishes =====
    /** Taptap冰激凌 — 幸运 I 5min + 抗火 I 2min */
    public static final DeferredItem<ModFoodItem> TAPTAP_ICE_CREAM = ITEMS.register("taptap_ice_cream",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(1, 1f)
                            .effect(new MobEffectInstance(MobEffects.LUCK, 6000, 0), 1.0f)
                            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    /** 合味道 — 生命提升 II × 20min，流浪商人购买 */
    public static final DeferredItem<ModFoodItem> HE_WEI_DAO = ITEMS.register("he_wei_dao",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(12, 16f)
                            .effect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 24000, 1), 1.0f)
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
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 1))));

    /** 番茄肉酱意面 — 速度 I × 3min */
    public static final DeferredItem<ModFoodItem> BOLOGNESE_LINGUINE_RECIPE = ITEMS.register("bolognese_linguine_recipe",
            () -> new ModFoodItem(new Item.Properties()
                    .food(fp(8, 10f)
                            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0), 1.0f)
                            .build())
                    .component(ModDataComponents.PLACEABLE_INFO.get(), new PlaceableInfo("plate", 2))));
}
