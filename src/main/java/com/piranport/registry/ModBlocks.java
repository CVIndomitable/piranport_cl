package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.CookingPotBlock;
import com.piranport.block.CuttingBoardBlock;
import com.piranport.block.FlareLightBlock;
import com.piranport.block.ReloadFacilityBlock;
import com.piranport.block.FourStageCropBlock;
import com.piranport.block.PlaceableFoodBlock;
import com.piranport.block.RiceCropBlock;
import com.piranport.block.SaltChipBlock;
import com.piranport.block.SmokeScreenBlock;
import com.piranport.block.StoneMillBlock;
import com.piranport.block.ThreeStageCropBlock;
import com.piranport.block.YubariWaterBucketBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PiranPort.MOD_ID);

    public static final DeferredBlock<Block> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> ALUMINUM_BLOCK = BLOCKS.registerSimpleBlock("aluminum_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<Block> SALT_BLOCK = BLOCKS.registerSimpleBlock("salt_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5f)
                    .sound(SoundType.SAND));

    public static final DeferredBlock<SaltChipBlock> SALT_CHIP = BLOCKS.register("salt_chip",
            () -> new SaltChipBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.3f)
                    .sound(SoundType.SAND)
                    .noOcclusion()));

    // ===== Crop Blocks (Phase 11) =====
    private static BlockBehaviour.Properties cropProps() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT);
    }

    public static final DeferredBlock<FourStageCropBlock> TOMATO_CROP =
            BLOCKS.register("tomato_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.TOMATO_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> SOYBEAN_CROP =
            BLOCKS.register("soybean_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.SOYBEAN_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> CHILI_CROP =
            BLOCKS.register("chili_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.CHILI_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> ONION_CROP =
            BLOCKS.register("onion_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.ONION_SEEDS.get()));
    public static final DeferredBlock<RiceCropBlock> RICE_CROP =
            BLOCKS.register("rice_crop", () -> new RiceCropBlock(cropProps(), () -> ModItems.RICE_SEEDS.get()));
    public static final DeferredBlock<ThreeStageCropBlock> LETTUCE_CROP =
            BLOCKS.register("lettuce_crop", () -> new ThreeStageCropBlock(cropProps(), () -> ModItems.LETTUCE_SEEDS.get()));
    public static final DeferredBlock<ThreeStageCropBlock> GARLIC_CROP =
            BLOCKS.register("garlic_crop", () -> new ThreeStageCropBlock(cropProps(), () -> ModItems.GARLIC_SEEDS.get()));
    // Phase 27: Pineapple crop
    public static final DeferredBlock<FourStageCropBlock> PINEAPPLE_CROP =
            BLOCKS.register("pineapple_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.PINEAPPLE_SEED.get()));

    // ===== Functional Blocks (Phase 12) =====
    public static final DeferredBlock<StoneMillBlock> STONE_MILL =
            BLOCKS.register("stone_mill", () -> new StoneMillBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5f, 10.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));

    // ===== Functional Blocks (Phase 13) =====
    public static final DeferredBlock<CuttingBoardBlock> CUTTING_BOARD =
            BLOCKS.register("cutting_board", () -> new CuttingBoardBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

    // ===== Functional Blocks (Phase 14) =====
    public static final DeferredBlock<CookingPotBlock> COOKING_POT =
            BLOCKS.register("cooking_pot", () -> new CookingPotBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    // ===== Placeable Food Blocks (Phase 16) =====
    private static BlockBehaviour.Properties foodBlockProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_WHITE)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noCollission();
    }

    public static final DeferredBlock<PlaceableFoodBlock.Plate> PLATE_FOOD =
            BLOCKS.register("plate_food", () -> new PlaceableFoodBlock.Plate(foodBlockProps()));
    public static final DeferredBlock<PlaceableFoodBlock.Bowl> BOWL_FOOD =
            BLOCKS.register("bowl_food", () -> new PlaceableFoodBlock.Bowl(foodBlockProps()));
    public static final DeferredBlock<PlaceableFoodBlock.Cake> CAKE_FOOD =
            BLOCKS.register("cake_food", () -> new PlaceableFoodBlock.Cake(foodBlockProps()));

    // ===== Reload Facility =====
    public static final DeferredBlock<ReloadFacilityBlock> RELOAD_FACILITY =
            BLOCKS.register("reload_facility", () -> new ReloadFacilityBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    // ===== Yubari Water Bucket =====
    public static final DeferredBlock<YubariWaterBucketBlock> YUBARI_WATER_BUCKET =
            BLOCKS.register("yubari_water_bucket", () -> new YubariWaterBucketBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

    // ===== Ammo Workbench =====
    public static final DeferredBlock<com.piranport.block.AmmoWorkbenchBlock> AMMO_WORKBENCH =
            BLOCKS.register("ammo_workbench", () -> new com.piranport.block.AmmoWorkbenchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    // ===== Weapon Workbench =====
    public static final DeferredBlock<com.piranport.block.WeaponWorkbenchBlock> WEAPON_WORKBENCH =
            BLOCKS.register("weapon_workbench", () -> new com.piranport.block.WeaponWorkbenchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    // ===== Smoke Screen =====
    public static final DeferredBlock<SmokeScreenBlock> SMOKE_SCREEN =
            BLOCKS.register("smoke_screen", () -> new SmokeScreenBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(-1.0f, 3600000.0f)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .noCollission()
                            .noLootTable()
                            .isViewBlocking((s, g, p) -> true)
                            .isSuffocating((s, g, p) -> false)
                            .isRedstoneConductor((s, g, p) -> false)));

    // ===== Flare Light =====
    public static final DeferredBlock<FlareLightBlock> FLARE_LIGHT =
            BLOCKS.register("flare_light", () -> new FlareLightBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NONE)
                            .noCollission()
                            .noOcclusion()
                            .noLootTable()
                            .replaceable()
                            .lightLevel(state -> 14)
                            .sound(SoundType.WOOL)
                            .instabreak()));

    // ===== Abyssal Portal (v0.0.11) =====
    public static final DeferredBlock<com.piranport.block.PortalFrameBlock> ABYSSAL_PORTAL_FRAME =
            BLOCKS.register("abyssal_portal_frame",
                    () -> new com.piranport.block.PortalFrameBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(5.0f, 1200.0f)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 3)));

    public static final DeferredBlock<com.piranport.block.AbyssalPortalBlock> ABYSSAL_PORTAL =
            BLOCKS.register("abyssal_portal",
                    () -> new com.piranport.block.AbyssalPortalBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .noCollission()
                                    .noOcclusion()
                                    .noLootTable()
                                    .strength(-1.0f, 3600000.0f)
                                    .lightLevel(state -> 11)
                                    .sound(SoundType.GLASS)));

    // ===== Abyssal Spawner (v0.0.11) =====
    public static final DeferredBlock<com.piranport.block.AbyssalSpawnerBlock> ABYSSAL_SPAWNER =
            BLOCKS.register("abyssal_spawner",
                    () -> new com.piranport.block.AbyssalSpawnerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DEEPSLATE)
                                    .strength(-1.0f, 3600000.0f)
                                    .noLootTable()
                                    .noOcclusion()));

    // ===== Decorative Blocks (from sheropshire) =====
    public static final DeferredBlock<com.piranport.block.ConfidentialCargoBlock> CONFIDENTIAL_CARGO =
            BLOCKS.register("confidential_cargo",
                    () -> new com.piranport.block.ConfidentialCargoBlock());

    public static final DeferredBlock<com.piranport.block.AbyssRedSpiderLilyBlock> ABYSS_RED_SPIDER_LILY =
            BLOCKS.register("abyss_red_spider_lily",
                    () -> new com.piranport.block.AbyssRedSpiderLilyBlock());

    public static final DeferredBlock<com.piranport.block.ItalianDishKitBlock> ITALIAN_DISH_KIT =
            BLOCKS.register("italian_dish_kit",
                    () -> new com.piranport.block.ItalianDishKitBlock());

    public static final DeferredBlock<com.piranport.block.B25ModelBlock> B25_MODEL =
            BLOCKS.register("b25_model",
                    () -> new com.piranport.block.B25ModelBlock());

    public static final DeferredBlock<com.piranport.block.ModelDebugBlock> MODEL_DEBUG =
            BLOCKS.register("model_debug",
                    () -> new com.piranport.block.ModelDebugBlock());

    // ===== Dungeon System (v0.0.8) =====
    public static final DeferredBlock<com.piranport.dungeon.block.DungeonLecternBlock> DUNGEON_LECTERN =
            BLOCKS.register("dungeon_lectern",
                    () -> new com.piranport.dungeon.block.DungeonLecternBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DEEPSLATE)
                                    .strength(5.0f, 1200.0f)
                                    .sound(SoundType.DEEPSLATE)
                                    .noOcclusion()));
}
