package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.AbyssalSeepBlock;
import com.piranport.block.BlueprintChestBlock;
import com.piranport.block.CookingPotBlock;
import com.piranport.block.CuttingBoardBlock;
import com.piranport.block.FlareLightBlock;
import com.piranport.block.ReloadFacilityBlock;
import com.piranport.block.FourStageCropBlock;
import com.piranport.block.PlaceableFoodBlock;
import com.piranport.block.RiceCropBlock;
import com.piranport.block.SaltChipBlock;
import com.piranport.block.SeasonalLeavesBlock;
import com.piranport.block.SmokeScreenBlock;
import com.piranport.block.StoneMillBlock;
import com.piranport.block.StoveBlock;
import com.piranport.block.ThreeStageCropBlock;
import com.piranport.block.WildGardenBlock;
import com.piranport.block.YubariWaterBucketBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

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

    public static final DeferredBlock<WildGardenBlock> WILD_GARDEN =
            BLOCKS.register("wild_garden", () -> new WildGardenBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)
                            .noCollission()
                            .instabreak()
                            .sound(SoundType.GRASS)));

    // ===== Phase 28: Shipgirl Food Expansion Crops =====
    public static final DeferredBlock<FourStageCropBlock> LABLAB_BEAN_CROP =
            BLOCKS.register("lablab_bean_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.LABLAB_BEAN_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> ORMOSIA_CROP =
            BLOCKS.register("ormosia_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.ORMOSIA_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> CELERY_CROP =
            BLOCKS.register("celery_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.CELERY_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> RYE_CROP =
            BLOCKS.register("rye_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.RYE_SEEDS.get()));

    // ===== Phase 28: Peach Tree =====
    private static ResourceKey<ConfiguredFeature<?, ?>> treeFeatureKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, name));
    }

    private static DeferredBlock<RotatedPillarBlock> registerTreeLog(String name) {
        return BLOCKS.register(name, () -> new RotatedPillarBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    }

    private static DeferredBlock<SeasonalLeavesBlock> registerSeasonalLeaves(String name) {
        return BLOCKS.register(name, () -> new SeasonalLeavesBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    }

    private static DeferredBlock<SaplingBlock> registerTreeSapling(
            String name,
            String growerName,
            ResourceKey<ConfiguredFeature<?, ?>> featureKey) {
        return BLOCKS.register(name, () -> new SaplingBlock(
                new TreeGrower("piranport:" + growerName,
                        Optional.empty(),
                        Optional.of(featureKey),
                        Optional.empty()),
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    }

    public static final ResourceKey<ConfiguredFeature<?, ?>> PEACH_TREE_FEATURE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "peach_tree"));

    public static final DeferredBlock<RotatedPillarBlock> PEACH_LOG =
            BLOCKS.register("peach_log", () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<SeasonalLeavesBlock> PEACH_LEAVES =
            registerSeasonalLeaves("peach_leaves");

    public static final DeferredBlock<SaplingBlock> PEACH_SAPLING =
            BLOCKS.register("peach_sapling", () -> new SaplingBlock(
                    new TreeGrower("piranport:peach",
                            Optional.empty(),
                            Optional.of(PEACH_TREE_FEATURE_KEY),
                            Optional.empty()),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    public static final ResourceKey<ConfiguredFeature<?, ?>> MAIDENHAIR_TREE_FEATURE_KEY =
            treeFeatureKey("maidenhair_tree");
    public static final DeferredBlock<RotatedPillarBlock> MAIDENHAIR_LOG =
            registerTreeLog("maidenhair_log");
    public static final DeferredBlock<SeasonalLeavesBlock> MAIDENHAIR_LEAVES =
            registerSeasonalLeaves("maidenhair_leaves");
    public static final DeferredBlock<SaplingBlock> MAIDENHAIR_SAPLING =
            registerTreeSapling("maidenhair_sapling", "maidenhair", MAIDENHAIR_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> SAGO_PALM_TREE_FEATURE_KEY =
            treeFeatureKey("sago_palm_tree");
    public static final DeferredBlock<RotatedPillarBlock> SAGO_PALM_LOG =
            registerTreeLog("sago_palm_log");
    public static final DeferredBlock<SeasonalLeavesBlock> SAGO_PALM_LEAVES =
            registerSeasonalLeaves("sago_palm_leaves");
    public static final DeferredBlock<SaplingBlock> SAGO_PALM_SAPLING =
            registerTreeSapling("sago_palm_sapling", "sago_palm", SAGO_PALM_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> GARDENIA_TREE_FEATURE_KEY =
            treeFeatureKey("gardenia_tree");
    public static final DeferredBlock<RotatedPillarBlock> GARDENIA_LOG =
            registerTreeLog("gardenia_log");
    public static final DeferredBlock<SeasonalLeavesBlock> GARDENIA_LEAVES =
            registerSeasonalLeaves("gardenia_leaves");
    public static final DeferredBlock<SaplingBlock> GARDENIA_SAPLING =
            registerTreeSapling("gardenia_sapling", "gardenia", GARDENIA_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> CHINESE_PLUM_TREE_FEATURE_KEY =
            treeFeatureKey("chinese_plum_tree");
    public static final DeferredBlock<RotatedPillarBlock> CHINESE_PLUM_LOG =
            registerTreeLog("chinese_plum_log");
    public static final DeferredBlock<SeasonalLeavesBlock> CHINESE_PLUM_LEAVES =
            registerSeasonalLeaves("chinese_plum_leaves");
    public static final DeferredBlock<SaplingBlock> CHINESE_PLUM_SAPLING =
            registerTreeSapling("chinese_plum_sapling", "chinese_plum", CHINESE_PLUM_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> MAPPLE_TREE_FEATURE_KEY =
            treeFeatureKey("mapple_tree");
    public static final DeferredBlock<RotatedPillarBlock> MAPPLE_LOG =
            registerTreeLog("mapple_log");
    public static final DeferredBlock<SeasonalLeavesBlock> MAPPLE_LEAVES =
            registerSeasonalLeaves("mapple_leaves");
    public static final DeferredBlock<SaplingBlock> MAPPLE_SAPLING =
            registerTreeSapling("mapple_sapling", "mapple", MAPPLE_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> CHORUS_TREE_FEATURE_KEY =
            treeFeatureKey("chorus_tree");
    public static final DeferredBlock<RotatedPillarBlock> CHORUS_TREE_LOG =
            registerTreeLog("chorus_tree_log");
    public static final DeferredBlock<SeasonalLeavesBlock> CHORUS_TREE_LEAVES =
            registerSeasonalLeaves("chorus_tree_leaves");
    public static final DeferredBlock<SaplingBlock> CHORUS_TREE_SAPLING =
            registerTreeSapling("chorus_tree_sapling", "chorus_tree", CHORUS_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> SLIME_TREE_FEATURE_KEY =
            treeFeatureKey("slime_tree");
    public static final DeferredBlock<RotatedPillarBlock> SLIME_TREE_LOG =
            registerTreeLog("slime_tree_log");
    public static final DeferredBlock<SeasonalLeavesBlock> SLIME_TREE_LEAVES =
            registerSeasonalLeaves("slime_tree_leaves");
    public static final DeferredBlock<SaplingBlock> SLIME_TREE_SAPLING =
            registerTreeSapling("slime_tree_sapling", "slime_tree", SLIME_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> LAVA_SLIME_TREE_FEATURE_KEY =
            treeFeatureKey("lava_slime_tree");
    public static final DeferredBlock<RotatedPillarBlock> LAVA_SLIME_TREE_LOG =
            registerTreeLog("lava_slime_tree_log");
    public static final DeferredBlock<SeasonalLeavesBlock> LAVA_SLIME_TREE_LEAVES =
            registerSeasonalLeaves("lava_slime_tree_leaves");
    public static final DeferredBlock<SaplingBlock> LAVA_SLIME_TREE_SAPLING =
            registerTreeSapling("lava_slime_tree_sapling", "lava_slime_tree", LAVA_SLIME_TREE_FEATURE_KEY);

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

    public static final DeferredBlock<StoveBlock> STOVE =
            BLOCKS.register("stove", () -> new StoveBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)
                            .noOcclusion()
                            .lightLevel(state -> 8)));

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

    // ===== Ship Core Modifier =====
    public static final DeferredBlock<com.piranport.block.ShipCoreModifierBlock> SHIP_CORE_MODIFIER =
            BLOCKS.register("ship_core_modifier", () -> new com.piranport.block.ShipCoreModifierBlock(
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

    public static final DeferredBlock<BlueprintChestBlock> BLUEPRINT_CHEST =
            BLOCKS.register("blueprint_chest", () -> new BlueprintChestBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5f, 4.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

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

    public static final DeferredBlock<AbyssalSeepBlock> ABYSSAL_SEEP =
            BLOCKS.register("abyssal_seep",
                    () -> new AbyssalSeepBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(4.0f, 9.0f)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 6)));

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
