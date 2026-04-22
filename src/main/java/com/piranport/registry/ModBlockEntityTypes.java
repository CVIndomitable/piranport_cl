package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import com.piranport.block.entity.ReloadFacilityBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PiranPort.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReloadFacilityBlockEntity>> RELOAD_FACILITY =
            BLOCK_ENTITY_TYPES.register("reload_facility", () ->
                    BlockEntityType.Builder.of(ReloadFacilityBlockEntity::new, ModBlocks.RELOAD_FACILITY.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.block.entity.ModelDebugBlockEntity>> MODEL_DEBUG =
            BLOCK_ENTITY_TYPES.register("model_debug", () ->
                    BlockEntityType.Builder.of(com.piranport.block.entity.ModelDebugBlockEntity::new,
                            ModBlocks.MODEL_DEBUG.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceableFoodBlockEntity>> PLACEABLE_FOOD =
            BLOCK_ENTITY_TYPES.register("placeable_food", () ->
                    BlockEntityType.Builder.of(PlaceableFoodBlockEntity::new,
                            ModBlocks.PLATE_FOOD.get(),
                            ModBlocks.BOWL_FOOD.get(),
                            ModBlocks.CAKE_FOOD.get())
                            .build(null));
}
