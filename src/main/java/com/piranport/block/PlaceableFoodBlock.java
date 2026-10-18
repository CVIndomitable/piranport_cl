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
     * 专属模型食物方块（龙田烧）：几块金黄酥脆的炸鸡堆叠，
     * 上面压两条青椒，前面摆两片柠檬。
     */
    public static class ChickenTatsuta extends PlaceableFoodBlock {
        public static final MapCodec<ChickenTatsuta> CODEC = simpleCodec(ChickenTatsuta::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public ChickenTatsuta(BlockBehaviour.Properties props) { super(Block.box(0.8, 0, 4.6, 12.5, 2.9, 12.6), props); }
    }

    /**
     * 专属模型食物方块（烤猪肘）：红亮脆皮烤猪肘，骨头朝上，
     * 底下垫浅黄酸菜，右侧一小碟深色肉汁。
     */
    public static class Schweinshaxe extends PlaceableFoodBlock {
        public static final MapCodec<Schweinshaxe> CODEC = simpleCodec(Schweinshaxe::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Schweinshaxe(BlockBehaviour.Properties props) { super(Block.box(1.4, 0, 4.2, 15.7, 8.0, 14.0), props); }
    }

    /**
     * 专属模型食物方块（红茶三明治）：双层金架下午茶点架，上下两层
     * 摆手指三明治（黄瓜/红色夹心），左侧一套红茶杯碟。
     */
    public static class BlackTeaSandwich extends PlaceableFoodBlock {
        public static final MapCodec<BlackTeaSandwich> CODEC = simpleCodec(BlackTeaSandwich::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public BlackTeaSandwich(BlockBehaviour.Properties props) { super(Block.box(0.6, 0, 1.6, 15.4, 6.7, 13.6), props); }
    }

    /**
     * 专属模型食物方块（美式汉堡）：芝麻面包顶 + 芝士片 + 牛肉饼 +
     * 番茄片 + 生菜 + 底面包的经典叠层。
     */
    public static class AmericanBurger extends PlaceableFoodBlock {
        public static final MapCodec<AmericanBurger> CODEC = simpleCodec(AmericanBurger::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public AmericanBurger(BlockBehaviour.Properties props) { super(Block.box(1.4, 0, 2.3, 14.6, 6.6, 13.7), props); }
    }

    /**
     * 专属模型食物方块（甜豆花）：白瓷碗盛嫩豆花，浇琥珀糖浆，
     * 配红豆与黄绿蜜豆。
     */
    public static class BeetBlossom extends PlaceableFoodBlock {
        public static final MapCodec<BeetBlossom> CODEC = simpleCodec(BeetBlossom::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public BeetBlossom(BlockBehaviour.Properties props) { super(Block.box(1.4, 0, 1.4, 14.6, 5.6, 14.6), props); }
    }

    /**
     * 专属模型食物方块（咸豆花）：白瓷碗盛嫩豆花，浇酱色汤汁，
     * 配深色紫菜/榨菜、葱花与肉末。
     */
    public static class SaltyBeanCurd extends PlaceableFoodBlock {
        public static final MapCodec<SaltyBeanCurd> CODEC = simpleCodec(SaltyBeanCurd::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public SaltyBeanCurd(BlockBehaviour.Properties props) { super(Block.box(1.4, 0, 1.4, 14.6, 5.6, 14.6), props); }
    }

    /**
     * 专属模型食物方块（金镶玉包子）：圆形竹蒸笼，垫着竹编笼底，
     * 里面摆三个带放射褶和粉点的白胖包子。
     */
    public static class DeluxeBaozi extends PlaceableFoodBlock {
        public static final MapCodec<DeluxeBaozi> CODEC = simpleCodec(DeluxeBaozi::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public DeluxeBaozi(BlockBehaviour.Properties props) { super(Block.box(0.9, 0, 0.9, 15.1, 5.6, 15.1), props); }
    }

    /**
     * 专属模型食物方块（什锦叉烧炒饭）：白色方形浅盘（盘沿有蓝纹），
     * 中间一座炒饭，混着叉烧丁、蛋碎、青豆与葱花。
     */
    public static class AssortedCharSiuFriedRice extends PlaceableFoodBlock {
        public static final MapCodec<AssortedCharSiuFriedRice> CODEC = simpleCodec(AssortedCharSiuFriedRice::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public AssortedCharSiuFriedRice(BlockBehaviour.Properties props) { super(Block.box(0.8, 0, 0.8, 15.2, 2.4, 15.2), props); }
    }

    /**
     * 专属模型食物方块（辣条）：米白方形浅盘，红亮油润的辣条
     * 长短条分三层交错叠放。
     */
    public static class Latiao extends PlaceableFoodBlock {
        public static final MapCodec<Latiao> CODEC = simpleCodec(Latiao::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Latiao(BlockBehaviour.Properties props) { super(Block.box(0.8, 0, 0.8, 15.2, 1.95, 15.2), props); }
    }

    /**
     * 专属模型食物方块（天妇罗）：黑色方盘垫白纸，四只炸虾天妇罗
     * 竖立成一簇（红虾尾朝上），配深褐蔬菜天妇罗、萝卜泥和绿叶。
     */
    public static class Tempura extends PlaceableFoodBlock {
        public static final MapCodec<Tempura> CODEC = simpleCodec(Tempura::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Tempura(BlockBehaviour.Properties props) { super(Block.box(0.8, 0, 0.8, 15.2, 5.5, 15.2), props); }
    }

    /**
     * 专属模型食物方块（羊羹）：淡蓝灰长方瓷盘上，一条切成四片的
     * 暗红棕羊羹，盘面散着几粒红豆。
     */
    public static class Yokan extends PlaceableFoodBlock {
        public static final MapCodec<Yokan> CODEC = simpleCodec(Yokan::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public Yokan(BlockBehaviour.Properties props) { super(Block.box(1.0, 0, 1.5, 15.0, 3.2, 14.5), props); }
    }

    /**
     * 专属模型食物方块（皇家海军咸牛肉）：鼓腹橡木桶，两道深灰铁箍，
     * 桶口露出暗红咸牛肉块。
     */
    public static class RoyalNavalSaltedBeef extends PlaceableFoodBlock {
        public static final MapCodec<RoyalNavalSaltedBeef> CODEC = simpleCodec(RoyalNavalSaltedBeef::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public RoyalNavalSaltedBeef(BlockBehaviour.Properties props) { super(Block.box(1.5, 0, 1.5, 14.5, 11.5, 14.5), props); }
    }

    /**
     * 专属模型食物方块（仰望星空派）：深灰波浪边派盘，金黄酥皮压着
     * 橙色五角星，六条沙丁鱼的头/尾从馅里竖出来。
     */
    public static class StargazyPie extends PlaceableFoodBlock {
        public static final MapCodec<StargazyPie> CODEC = simpleCodec(StargazyPie::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public StargazyPie(BlockBehaviour.Properties props) { super(Block.box(1.0, 0, 1.0, 15.0, 5.9, 15.0), props); }
    }

    /**
     * 专属模型食物方块（麻婆豆腐）：白瓷椭圆浅盘，中央红亮辣油里
     * 浮着十二块豆腐，撒青葱与深色肉末/花椒。
     */
    public static class MapoToFu extends PlaceableFoodBlock {
        public static final MapCodec<MapoToFu> CODEC = simpleCodec(MapoToFu::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public MapoToFu(BlockBehaviour.Properties props) { super(Block.box(0.6, 0, 0.8, 15.4, 2.3, 15.2), props); }
    }

    /**
     * 专属模型食物方块（海军咖喱）：白色椭圆盘左前方深棕咖喱酱
     * 带土豆/胡萝卜/肉块，右后方一座三层白米饭。
     */
    public static class NavalCurry extends PlaceableFoodBlock {
        public static final MapCodec<NavalCurry> CODEC = simpleCodec(NavalCurry::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public NavalCurry(BlockBehaviour.Properties props) { super(Block.box(0.5, 0, 1.0, 15.5, 2.5, 15.0), props); }
    }

    /**
     * 专属模型食物方块（炸鱼薯条）：揉皱的报纸垫底、白纸托着金黄炸鱼柳、
     * 一堆粗薯条、一片柠檬和一小枝欧芹。
     */
    public static class FriedFishAndChips extends PlaceableFoodBlock {
        public static final MapCodec<FriedFishAndChips> CODEC = simpleCodec(FriedFishAndChips::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public FriedFishAndChips(BlockBehaviour.Properties props) { super(Block.box(0.5, 0, 0.5, 15.5, 2.3, 15.5), props); }
    }

    /**
     * 专属模型食物方块（苹果派）：深灰金属派盘，圆形金黄苹果馅上铺
     * 四横四纵的编织酥皮条。
     */
    public static class ApplePie extends PlaceableFoodBlock {
        public static final MapCodec<ApplePie> CODEC = simpleCodec(ApplePie::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public ApplePie(BlockBehaviour.Properties props) { super(Block.box(1.25, 0, 1.25, 14.75, 2.0, 14.75), props); }
    }

    /**
     * 专属模型食物方块（图林根肠啤酒）：白色椭圆盘盛两根焦褐烤肠、
     * 三根浅色香料肠、土豆泥、西兰花与番茄酱挤线，右侧一杯带把啤酒。
     */
    public static class ThuringerRostbratwurstUndBier extends PlaceableFoodBlock {
        public static final MapCodec<ThuringerRostbratwurstUndBier> CODEC = simpleCodec(ThuringerRostbratwurstUndBier::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public ThuringerRostbratwurstUndBier(BlockBehaviour.Properties props) { super(Block.box(0.2, 0, 1.0, 16.0, 7.4, 14.0), props); }
    }

    /**
     * 专属模型食物方块（白香肠面包圈）：巴伐利亚蓝白菱纹椭圆盘，
     * 盘上碱水面包圈 + 两根白香肠 + 肉丸 + 欧芹叶。
     */
    public static class WeisswurstMitDerBagel extends PlaceableFoodBlock {
        public static final MapCodec<WeisswurstMitDerBagel> CODEC = simpleCodec(WeisswurstMitDerBagel::new);
        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public WeisswurstMitDerBagel(BlockBehaviour.Properties props) { super(Block.box(0.5, 0, 1.5, 15.5, 3.3, 14.5), props); }
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
