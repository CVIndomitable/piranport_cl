package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.block.entity.CuttingBoardBlockEntity;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import com.piranport.block.entity.ReloadFacilityBlockEntity;
import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.block.entity.YubariWaterBucketBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PiranPort.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneMillBlockEntity>> STONE_MILL =
            BLOCK_ENTITY_TYPES.register("stone_mill", () ->
                    BlockEntityType.Builder.of(StoneMillBlockEntity::new, ModBlocks.STONE_MILL.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD =
            BLOCK_ENTITY_TYPES.register("cutting_board", () ->
                    BlockEntityType.Builder.of(CuttingBoardBlockEntity::new, ModBlocks.CUTTING_BOARD.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CookingPotBlockEntity>> COOKING_POT =
            BLOCK_ENTITY_TYPES.register("cooking_pot", () ->
                    BlockEntityType.Builder.of(CookingPotBlockEntity::new, ModBlocks.COOKING_POT.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReloadFacilityBlockEntity>> RELOAD_FACILITY =
            BLOCK_ENTITY_TYPES.register("reload_facility", () ->
                    BlockEntityType.Builder.of(ReloadFacilityBlockEntity::new, ModBlocks.RELOAD_FACILITY.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<YubariWaterBucketBlockEntity>> YUBARI_WATER_BUCKET =
            BLOCK_ENTITY_TYPES.register("yubari_water_bucket", () ->
                    BlockEntityType.Builder.of(YubariWaterBucketBlockEntity::new, ModBlocks.YUBARI_WATER_BUCKET.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceableFoodBlockEntity>> PLACEABLE_FOOD =
            BLOCK_ENTITY_TYPES.register("placeable_food", () ->
                    BlockEntityType.Builder.of(PlaceableFoodBlockEntity::new,
                            ModBlocks.PLATE_FOOD.get(),
                            ModBlocks.BOWL_FOOD.get(),
                            ModBlocks.CAKE_FOOD.get())
                            .build(null));
}
