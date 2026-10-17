package com.piranport.block;

import com.mojang.serialization.MapCodec;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PlaceableFoodBlock extends BaseEntityBlock {
    private final VoxelShape shape;

    public PlaceableFoodBlock(VoxelShape shape, BlockBehaviour.Properties props) {
        super(props);
        this.shape = shape;
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException("PlaceableFoodBlock is not world-gen serializable");
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlaceableFoodBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlaceableFoodBlockEntity foodBE && !foodBE.isEmpty()) {
            if (!level.isClientSide) {
                foodBE.eat(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // === Subclasses with shape-specific codecs ===

    public static class Plate extends PlaceableFoodBlock {
        public static final MapCodec<Plate> CODEC = simpleCodec(Plate::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Plate(BlockBehaviour.Properties props) { super(Block.box(0, 0, 0, 16, 4, 16), props); }
    }

    public static class Bowl extends PlaceableFoodBlock {
        public static final MapCodec<Bowl> CODEC = simpleCodec(Bowl::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Bowl(BlockBehaviour.Properties props) { super(Block.box(2, 0, 2, 14, 6, 14), props); }
    }

    public static class Cake extends PlaceableFoodBlock {
        public static final MapCodec<Cake> CODEC = simpleCodec(Cake::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Cake(BlockBehaviour.Properties props) { super(Block.box(1, 0, 1, 15, 8, 15), props); }
    }

    /**
     * 专属模型食物方块（吐司面包）：模型自带盘+食物本体，碰撞箱贴合模型外框。
     * 与三容器的区别：不依赖浮空物品图标展示内容物，渲染器跳过叠影（见 PlaceableFoodRenderer）。
     */
    public static class Toast extends PlaceableFoodBlock {
        public static final MapCodec<Toast> CODEC = simpleCodec(Toast::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Toast(BlockBehaviour.Properties props) { super(Block.box(1, 0, 1, 15, 6, 15), props); }
    }

    /**
     * 专属模型食物方块（海军烘豆子）：模型自带盘+茄汁豆堆+两片番茄+一小撮橄榄，
     * 碰撞箱取整盘外框。
     */
    public static class NavalBakedBeans extends PlaceableFoodBlock {
        public static final MapCodec<NavalBakedBeans> CODEC = simpleCodec(NavalBakedBeans::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public NavalBakedBeans(BlockBehaviour.Properties props) { super(Block.box(1, 0, 1, 15, 3.5, 15), props); }
    }

    /**
     * 专属模型食物方块（俄式肉冻）：平盘上琥珀色肉冻块堆成金字塔
     * （5 底 + 3 中 + 1 顶，独立小方块），一角露绿色香草。
     */
    public static class Okroshka extends PlaceableFoodBlock {
        public static final MapCodec<Okroshka> CODEC = simpleCodec(Okroshka::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Okroshka(BlockBehaviour.Properties props) { super(Block.box(1, 0, 1, 15, 9.8, 15), props); }
    }

    /**
     * 专属模型食物方块（俄式饺子）：浅棕碗（四沿围合）盛奶油色小饺子堆，
     * 一角露绿色香草。
     */
    public static class RussianDumpling extends PlaceableFoodBlock {
        public static final MapCodec<RussianDumpling> CODEC = simpleCodec(RussianDumpling::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public RussianDumpling(BlockBehaviour.Properties props) { super(Block.box(2, 0, 2, 14, 5, 14), props); }
    }

    /**
     * 专属模型食物方块（黑麦面包扁豆汤）：编织篮装黑列巴（深棕芝麻面包）
     * + 旁置小白碗扁豆汤。
     */
    public static class PeaSoupWithRyeBread extends PlaceableFoodBlock {
        public static final MapCodec<PeaSoupWithRyeBread> CODEC = simpleCodec(PeaSoupWithRyeBread::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public PeaSoupWithRyeBread(BlockBehaviour.Properties props) { super(Block.box(1, 0, 3, 14.5, 6.5, 11), props); }
    }

    /**
     * 专属模型食物方块（马赛鱼汤）：浅汤盘+橙红浓汤+汤面鱼块/肉丸+盘沿面包片。
     */
    public static class Bouillabaisse extends PlaceableFoodBlock {
        public static final MapCodec<Bouillabaisse> CODEC = simpleCodec(Bouillabaisse::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Bouillabaisse(BlockBehaviour.Properties props) { super(Block.box(1, 0, 1, 15, 3.6, 15), props); }
    }

    /**
     * 专属模型食物方块（黑森林蛋糕）：白瓷方盘上的方形巧克力蛋糕，
     * 顶面四坨裱花奶油各顶一颗带梗红樱桃。
     */
    public static class BlackForestGateau extends PlaceableFoodBlock {
        public static final MapCodec<BlackForestGateau> CODEC = simpleCodec(BlackForestGateau::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public BlackForestGateau(BlockBehaviour.Properties props) { super(Block.box(0.8, 0, 0.8, 15.2, 10.2, 15.2), props); }
    }

    /**
     * 专属模型食物方块（罗宋汤）：白瓷底碟+高脚宽口汤盘盛甜菜红汤，
     * 汤面中央一坨酸奶油、左侧欧芹叶。
     */
    public static class Borscht extends PlaceableFoodBlock {
        public static final MapCodec<Borscht> CODEC = simpleCodec(Borscht::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Borscht(BlockBehaviour.Properties props) { super(Block.box(0.5, 0, 0.5, 15.5, 5.4, 15.5), props); }
    }

    /**
     * 专属模型食物方块（萨拉米披萨）：木板托盘（带手柄）+整张披萨+凸起萨拉米片，
     * 碰撞箱取托盘外框。
     */
    public static class SalamiPizza extends PlaceableFoodBlock {
        public static final MapCodec<SalamiPizza> CODEC = simpleCodec(SalamiPizza::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public SalamiPizza(BlockBehaviour.Properties props) { super(Block.box(1, 0, 2, 15, 2.9, 16), props); }
    }

    /**
     * 专属模型食物方块（马卡龙）：三层甜点架模型（底盘+中盘+顶盘+金支柱+顶环，
     * 各层摆放马卡龙），碰撞箱取整架外框。
     */
    public static class Macaron extends PlaceableFoodBlock {
        public static final MapCodec<Macaron> CODEC = simpleCodec(Macaron::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Macaron(BlockBehaviour.Properties props) { super(Block.box(2, 0, 2, 14, 13.2, 14), props); }
    }

    /**
     * 专属模型食物方块（墨索里尼的屁股）：意式军粮罐头模型（橄榄绿罐身+白字带+银压边），
     * 碰撞箱贴合罐体外框。
     */
    public static class MussolinisOo extends PlaceableFoodBlock {
        public static final MapCodec<MussolinisOo> CODEC = simpleCodec(MussolinisOo::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public MussolinisOo(BlockBehaviour.Properties props) { super(Block.box(3.5, 0, 3.5, 12.5, 8, 12.5), props); }
    }

    /**
     * 专属模型食物方块（鲱鱼罐头）：模型是一罐竖放的罐头（罐身+盖沿），碰撞箱贴合罐体外框。
     */
    public static class Surstromming extends PlaceableFoodBlock {
        public static final MapCodec<Surstromming> CODEC = simpleCodec(Surstromming::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Surstromming(BlockBehaviour.Properties props) { super(Block.box(3.5, 0, 3.5, 12.5, 8, 12.5), props); }
    }

    /**
     * 专属模型食物方块（鱼雷果汁）：模型自带玻璃杯 + 液面 + 杯口斜搭的青柠片。
     * 碰撞箱拆成"杯体"与"青柠片"两块，避免整块虚拟方框把杯口上方的空气也算进去。
     */
    public static class TorpedoJuice extends PlaceableFoodBlock {
        public static final MapCodec<TorpedoJuice> CODEC = simpleCodec(TorpedoJuice::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public TorpedoJuice(BlockBehaviour.Properties props) {
            super(Shapes.or(
                    Block.box(4.5, 0, 4.5, 11.5, 10, 11.5),
                    Block.box(2.0, 8.5, 7.4, 7.0, 13.5, 8.6)
            ), props);
        }
    }
}
