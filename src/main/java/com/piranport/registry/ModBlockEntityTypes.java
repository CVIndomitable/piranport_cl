package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.entity.ModelDebugBlockEntity;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import com.piranport.block.entity.ReloadFacilityBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PiranPort.MOD_ID);

    public static final RegistryObject<BlockEntityType<ReloadFacilityBlockEntity>> RELOAD_FACILITY =
            BLOCK_ENTITY_TYPES.register("reload_facility", () ->
                    BlockEntityType.Builder.of(ReloadFacilityBlockEntity::new, ModBlocks.RELOAD_FACILITY.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<ModelDebugBlockEntity>> MODEL_DEBUG =
            BLOCK_ENTITY_TYPES.register("model_debug", () ->
                    BlockEntityType.Builder.of(ModelDebugBlockEntity::new, ModBlocks.MODEL_DEBUG.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<PlaceableFoodBlockEntity>> PLACEABLE_FOOD =
            BLOCK_ENTITY_TYPES.register("placeable_food", () ->
                    BlockEntityType.Builder.of(PlaceableFoodBlockEntity::new,
                            ModBlocks.PLATE_FOOD.get(),
                            ModBlocks.BOWL_FOOD.get(),
                            ModBlocks.CAKE_FOOD.get())
                            .build(null));

    private ModBlockEntityTypes() {}
}
