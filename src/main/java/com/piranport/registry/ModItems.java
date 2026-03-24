package com.piranport.registry;

import com.piranport.PiranPort;
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
    public static final DeferredItem<Item> SMALL_SHIP_CORE =
            ITEMS.registerSimpleItem("small_ship_core", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MEDIUM_SHIP_CORE =
            ITEMS.registerSimpleItem("medium_ship_core", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LARGE_SHIP_CORE =
            ITEMS.registerSimpleItem("large_ship_core", new Item.Properties().stacksTo(1));

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
}
