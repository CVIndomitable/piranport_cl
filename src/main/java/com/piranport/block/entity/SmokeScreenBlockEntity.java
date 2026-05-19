package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 烟雾方块的 BlockEntity，仅用于触发自定义渲染器。
 * 不存储数据，不需要 tick。
 */
public class SmokeScreenBlockEntity extends BlockEntity {
    public SmokeScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SMOKE_SCREEN.get(), pos, state);
    }

    // 渲染包围盒由 BlockEntity 基类自动计算，无需覆写
}
