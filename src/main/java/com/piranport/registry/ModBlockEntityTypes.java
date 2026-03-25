package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.entity.StoneMillBlockEntity;
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
}
