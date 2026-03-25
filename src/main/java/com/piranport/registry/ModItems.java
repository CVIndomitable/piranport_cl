package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
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
            ITEMS.registerSimpleItem("small_gun", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MEDIUM_GUN =
            ITEMS.registerSimpleItem("medium_gun", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LARGE_GUN =
            ITEMS.registerSimpleItem("large_gun", new Item.Properties().stacksTo(1));

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
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1), 2, 10));
    public static final DeferredItem<ArmorPlateItem> MEDIUM_ARMOR_PLATE =
            ITEMS.register("medium_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1), 4, 20));
    public static final DeferredItem<ArmorPlateItem> LARGE_ARMOR_PLATE =
            ITEMS.register("large_armor_plate",
                    () -> new ArmorPlateItem(new Item.Properties().stacksTo(1), 6, 30));

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

    // ===== Functional Block Items (Phase 12) =====
    public static final DeferredItem<BlockItem> STONE_MILL =
            ITEMS.registerSimpleBlockItem(ModBlocks.STONE_MILL);

    // ===== Torpedo Launchers =====
    public static final DeferredItem<TorpedoLauncherItem> TWIN_TORPEDO_LAUNCHER =
            ITEMS.register("twin_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(64),
                            533, 2, 100));
    public static final DeferredItem<TorpedoLauncherItem> TRIPLE_TORPEDO_LAUNCHER =
            ITEMS.register("triple_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(48),
                            533, 3, 100));
    public static final DeferredItem<TorpedoLauncherItem> QUAD_TORPEDO_LAUNCHER =
            ITEMS.register("quad_torpedo_launcher",
                    () -> new TorpedoLauncherItem(
                            new Item.Properties().stacksTo(1).durability(32),
                            610, 4, 120));
}
