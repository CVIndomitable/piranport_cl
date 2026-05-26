package com.piranport.registry.items;

import com.piranport.registry.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块物品注册工厂 — 所有 BlockItem 注册。
 */
public class BlockItems {

    private BlockItems() {}

    // ===== Block Items =====
    public static DeferredItem<BlockItem> createBauxiteOre(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE); }
    public static DeferredItem<BlockItem> createAluminumBlock(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.ALUMINUM_BLOCK); }
    public static DeferredItem<BlockItem> createSaltBlock(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.SALT_BLOCK); }
    public static DeferredItem<BlockItem> createSaltChip(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.SALT_CHIP); }

    // ===== Abyssal Blocks =====
    public static DeferredItem<BlockItem> createAbyssalPortalFrame(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.ABYSSAL_PORTAL_FRAME); }
    public static DeferredItem<BlockItem> createAbyssalSpawner(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.ABYSSAL_SPAWNER); }

    // ===== Decorative Blocks =====
    public static DeferredItem<BlockItem> createConfidentialCargo(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.CONFIDENTIAL_CARGO); }
    public static DeferredItem<BlockItem> createAbyssRedSpiderLily(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.ABYSS_RED_SPIDER_LILY); }
    public static DeferredItem<BlockItem> createItalianDishKit(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.ITALIAN_DISH_KIT); }
    public static DeferredItem<BlockItem> createB25Model(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.B25_MODEL); }

    // ===== Functional Block Items =====
    public static DeferredItem<BlockItem> createStoneMill(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.STONE_MILL); }
    public static DeferredItem<BlockItem> createCuttingBoard(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.CUTTING_BOARD); }
    public static DeferredItem<BlockItem> createCookingPot(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.COOKING_POT); }
    public static DeferredItem<BlockItem> createReloadFacility(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.RELOAD_FACILITY); }
    public static DeferredItem<BlockItem> createShipCoreModifier(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.SHIP_CORE_MODIFIER); }
    public static DeferredItem<BlockItem> createYubariWaterBucket(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.YUBARI_WATER_BUCKET); }
    public static DeferredItem<BlockItem> createAmmoWorkbench(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.AMMO_WORKBENCH); }
    public static DeferredItem<BlockItem> createWeaponWorkbench(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.WEAPON_WORKBENCH); }

    // ===== Phase 28: Peach tree =====
    public static DeferredItem<BlockItem> createPeachLog(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.PEACH_LOG); }
    public static DeferredItem<BlockItem> createPeachLeaves(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.PEACH_LEAVES); }
    public static DeferredItem<BlockItem> createPeachSapling(DeferredRegister.Items registry) { return registry.registerSimpleBlockItem(ModBlocks.PEACH_SAPLING); }
}
