package com.piranport.block;

import com.piranport.block.entity.ModelDebugBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Debug-only block used by /ppd model_debug to render a static entity model for orientation calibration. */
public class ModelDebugBlock extends Block implements EntityBlock {

    public ModelDebugBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .sound(SoundType.METAL)
                .strength(-1f, 3600000f)
                .noOcclusion()
                .noLootTable()
                .isRedstoneConductor((bs, br, bp) -> false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /**
     * 强制走实体渲染路径。
     *
     * <p>WHY 必须显式覆写：方块模型（{@code blockstates/model_debug.json}）是一个空模型，
     * 而默认的 {@code RenderShape.MODEL} 会在空模型上再叠一次方块烘焙层的"无几何"渲染。
     * 更关键的是调试方块本身没有可见几何，所有可见内容都来自 BER；返回 {@code INVISIBLE}
     * 把方块模型这一层彻底摘掉，只留 BER，避免空模型偶尔漏出的残影/粒子面
     * （{@code "parent": "block/air"} 在部分光影/资源包下不是真的什么都不画）。
     */
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModelDebugBlockEntity(pos, state);
    }
}
