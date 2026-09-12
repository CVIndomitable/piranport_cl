package com.piranport.registry;

import com.piranport.component.WeaponCategory;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.FloatingTargetItem;
import com.piranport.item.GuidebookItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 材料/方块/装饰类物品注册中心。
 * 与 {@link ModItems} 共享同一个 {@link DeferredRegister.Items}，
 * 故两处声明的注册 ID 不会冲突；调用方可通过 ModItems.* 兼容访问。
 */
public final class MaterialItems {
    private MaterialItems() {}

    /** 复用 ModItems 的 DeferredRegister，避免双注册。 */
    private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

    // ===== Block Items =====
    public static final DeferredItem<BlockItem> BAUXITE_ORE =
            ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE);
    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.ALUMINUM_BLOCK);
    public static final DeferredItem<BlockItem> SALT_BLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.SALT_BLOCK);
    public static final DeferredItem<BlockItem> SALT_CHIP =
            ITEMS.registerSimpleBlockItem(ModBlocks.SALT_CHIP);

    // ===== Abyssal Blocks (v0.0.11) =====
    public static final DeferredItem<BlockItem> ABYSSAL_PORTAL_FRAME =
            ITEMS.registerSimpleBlockItem(ModBlocks.ABYSSAL_PORTAL_FRAME);
    public static final DeferredItem<BlockItem> ABYSSAL_SPAWNER =
            ITEMS.registerSimpleBlockItem(ModBlocks.ABYSSAL_SPAWNER);
    public static final DeferredItem<BlockItem> ABYSSAL_SEEP =
            ITEMS.registerSimpleBlockItem(ModBlocks.ABYSSAL_SEEP);

    // ===== Decorative Blocks (from sheropshire) =====
    public static final DeferredItem<BlockItem> CONFIDENTIAL_CARGO =
            ITEMS.registerSimpleBlockItem(ModBlocks.CONFIDENTIAL_CARGO);
    public static final DeferredItem<BlockItem> ABYSS_RED_SPIDER_LILY =
            ITEMS.registerSimpleBlockItem(ModBlocks.ABYSS_RED_SPIDER_LILY);
    public static final DeferredItem<BlockItem> ITALIAN_DISH_KIT =
            ITEMS.registerSimpleBlockItem(ModBlocks.ITALIAN_DISH_KIT);

    public static final DeferredItem<BlockItem> PIRATE_CHAIR =
            ITEMS.registerSimpleBlockItem(ModBlocks.PIRATE_CHAIR);

    public static final DeferredItem<BlockItem> PIRATE_TABLE =
            ITEMS.registerSimpleBlockItem(ModBlocks.PIRATE_TABLE);
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

    // ===== Functional Block Items (Phase 12-14) =====
    public static final DeferredItem<BlockItem> STONE_MILL =
            ITEMS.registerSimpleBlockItem(ModBlocks.STONE_MILL);
    public static final DeferredItem<BlockItem> CUTTING_BOARD =
            ITEMS.registerSimpleBlockItem(ModBlocks.CUTTING_BOARD);
    public static final DeferredItem<BlockItem> COOKING_POT =
            ITEMS.registerSimpleBlockItem(ModBlocks.COOKING_POT);
    public static final DeferredItem<BlockItem> STOVE =
            ITEMS.registerSimpleBlockItem(ModBlocks.STOVE);
    public static final DeferredItem<BlockItem> RELOAD_FACILITY =
            ITEMS.registerSimpleBlockItem(ModBlocks.RELOAD_FACILITY);
    public static final DeferredItem<BlockItem> SHIP_CORE_MODIFIER =
            ITEMS.registerSimpleBlockItem(ModBlocks.SHIP_CORE_MODIFIER);
    public static final DeferredItem<BlockItem> YUBARI_WATER_BUCKET =
            ITEMS.registerSimpleBlockItem(ModBlocks.YUBARI_WATER_BUCKET);

    public static final DeferredItem<BlockItem> AMMO_WORKBENCH =
            ITEMS.registerSimpleBlockItem(ModBlocks.AMMO_WORKBENCH);
    public static final DeferredItem<BlockItem> WEAPON_WORKBENCH =
            ITEMS.registerSimpleBlockItem(ModBlocks.WEAPON_WORKBENCH);
    public static final DeferredItem<BlockItem> BLUEPRINT_CHEST =
            ITEMS.registerSimpleBlockItem(ModBlocks.BLUEPRINT_CHEST);

    // ===== Blueprints =====
    public static final DeferredItem<Item> MEDIUM_GUN_BLUEPRINT =
            ITEMS.registerSimpleItem("medium_gun_blueprint", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LARGE_GUN_BLUEPRINT =
            ITEMS.registerSimpleItem("large_gun_blueprint", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CREATIVE_BLUEPRINT =
            ITEMS.registerSimpleItem("creative_blueprint",
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // ===== Intermediate Products (Phase 13/16) =====
    public static final DeferredItem<Item> SAUSAGE            = ITEMS.registerSimpleItem("sausage");
    public static final DeferredItem<Item> SLICED_SAUSAGE     = ITEMS.registerSimpleItem("sliced_sausage");
    public static final DeferredItem<Item> BACON              = ITEMS.registerSimpleItem("bacon");
    public static final DeferredItem<Item> TOAST_BREAD_SLICES = ITEMS.registerSimpleItem("toast_bread_slices");
    public static final DeferredItem<Item> BEER               = ITEMS.registerSimpleItem("beer");
    public static final DeferredItem<Item> ROUND_BUN          = ITEMS.registerSimpleItem("round_bun");

    // 内部使用：PINEAPPLE 系列的可食用构造
    private static FoodProperties.Builder fp(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation / (nutrition * 2f))
                .alwaysEdible();
    }

    // ===== Phase 19: Floating Target =====
    public static final DeferredItem<FloatingTargetItem> FLOATING_TARGET =
            ITEMS.register("floating_target",
                    () -> new FloatingTargetItem(new Item.Properties().stacksTo(16)));

    // ===== Phase 23: Guidebook =====
    public static final DeferredItem<GuidebookItem> GUIDEBOOK =
            ITEMS.register("guidebook",
                    () -> new GuidebookItem(new Item.Properties().stacksTo(1)));

    // ===== Phase 27: Pineapple chain =====
    public static final DeferredItem<ItemNameBlockItem> PINEAPPLE_SEED =
            ITEMS.register("pineapple_seed", () -> new ItemNameBlockItem(
                    ModBlocks.PINEAPPLE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> PINEAPPLE =
            ITEMS.register("pineapple",
                    () -> new Item(new Item.Properties().food(fp(3, 3.8f).build())));
    public static final DeferredItem<Item> PINEAPPLE_JUICE =
            ITEMS.register("pineapple_juice",
                    () -> new Item(new Item.Properties().food(fp(2, 2.5f).build())));
}