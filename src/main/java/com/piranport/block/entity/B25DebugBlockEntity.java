package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Marker BlockEntity for rendering a static B25 model. No persisted state. */
public class B25DebugBlockEntity extends BlockEntity {

    public B25DebugBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.B25_DEBUG.get(), pos, state);
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0);
    }
}
