package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.FlareLightBlock;
import com.piranport.block.ReloadFacilityBlock;
import com.piranport.block.PlaceableFoodBlock;
import com.piranport.block.SaltChipBlock;
import com.piranport.block.SmokeScreenBlock;
import com.piranport.block.YubariWaterBucketBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
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

}
