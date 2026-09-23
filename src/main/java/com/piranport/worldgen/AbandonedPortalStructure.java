package com.piranport.worldgen;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.PortalStructureHelper;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 废弃传送门结构：在水面上方自然生成小岛，作为副本教学触点。
 *
 * <p>结构组成：
 * <ul>
 *   <li>岛基：草方块+泥土+圆石堆砌的小岛，高出水面 1~2 格</li>
 *   <li>平台：5×5 石砖台面（边缘苔石砖、次边缘裂纹石砖、中心普通石砖）</li>
 *   <li>传送门：{@code dungeon_portal} 构成的 4×5×4 框架，正面单层 2×3 开口</li>
 *   <li>讲台：{@code dungeon_lectern}（<b>空台面</b>），贴在框架底边外侧</li>
 *   <li>告示牌：写明"讲台插钥匙后走进门框"</li>
 * </ul>
 *
 * <p><b>朝向固定为 NORTH，且框架会往 -Z 方向溢出台面。</b>见 {@link #LECTERN_OFFSET} 与
 * {@link #hasSpace} 的说明：{@code right = facing.getClockWise()} 会让局部 lz 轴映射到
 * 世界 +X、lx 轴映射到世界 -Z，所以框架相对平台原点占 {@code x∈[1,4] y∈[1,5] z∈[0,3]}。
 * 改动朝向或偏移量前必须先重算这个包围盒，否则讲台会重新落回门框内部
 * （{@code findKeyedLectern} 判 null → 门永远不响应）或让 {@link #hasSpace} 漏检。</p>
 *
 * <p>策划依据：《副本/09》讲台+钥匙合并模式、《副本/17》§三"野外自然生成废弃传送门（包含讲台）
 * 作教学触点——首次右键空讲台 → 提示需要钥匙 → 引导到钥匙获取路径"。
 *
 * <p>旧版类注释写的是 {@code abyssal_portal_frame} / {@code abyssal_portal}，那是已废弃口径；
 * 本结构一律使用 {@code dungeon_*} 系列方块。
 */
public class AbandonedPortalStructure extends Feature<NoneFeatureConfiguration> {

    /** 结构宽度（不含岛基） */
    private static final int WIDTH = 5;
    /** 结构高度（不含岛基） */
    private static final int HEIGHT = 4;
    /** 结构深度（不含岛基） */
    private static final int DEPTH = 5;

    /** 岛基半径 */
    private static final int ISLAND_RADIUS = 4;
    /** 岛基高度（高出水面） */
    private static final int ISLAND_HEIGHT = 2;

    /**
     * {@link #findWaterLevel} 从 {@code origin.y} 向上探的最大格数，用于兜住
     * "origin 落在海床下方、水面在其上方"的情况。
     */
    private static final int SCAN_UP_LIMIT = 8;

    /**
     * 讲台相对平台原点的偏移。
     *
     * <p>讲台必须<b>贴在框架底边外侧</b>（《副本/17》定稿 + 项目所有者 2026-09-23 决策“讲台贴着的
     * 传送门框架生效”），否则门不工作。旧值 (2,1,2) 把讲台放在门前一格、与框架不相邻，是
     * 教学门点了没反应的原因之一。</p>
     *
     * <p><b>为什么是 (0,1,1) 而不是曾经的 (3,1,1)：</b>{@code placeFrameGeometry} 的坐标映射是
     * {@code worldX = cornerX + facing.stepX*lx + right.stepX*lz}、{@code worldZ = cornerZ + facing.stepZ*lx + right.stepZ*lz}，
     * 其中 {@code right = facing.getClockWise()}。facing=NORTH 时 {@code facing.step=(0,-1)}（Z 轴）、
     * {@code right=EAST} 且 {@code right.step=(1,0)}，于是框架世界坐标是
     * {@code x = cornerX + lz}、{@code z = cornerZ - lx}——<b>Z 是反的</b>。把 LECTERN_OFFSET(3,1,1)
     * 代入得世界 (3,1,1)，它等于框架局部 {@code (lx=2, lz=2)}，<b>落在门框体积内部</b>；
     * {@code DungeonPortalBlockEntity.findKeyedLectern} 会用 {@code isInsideFrame} 把它排除，
     * 于是该方法永远返回 null，玩家走进门框只会看到 waiting_for_activation，<b>根本走不到
     * DungeonEntryService.enter</b>——"副本无法进入"因此原样复现。</p>
     *
     * <p>(0,1,1) 落在框架<b>西侧</b>（局部 {@code lx=0, lz=0} 那格的正 X 方向一格，
     * 即世界 (0,1,1)），与局部 (1,0,0) 这格（框架实体、位于 ly==0 底边层）正交相邻，
     * 既满足"贴底边外侧"的挂载口径，又完全在框架体积之外；同时它也在 {@link #hasSpace}
     * 的扫描盒内。这格是底边层 16 格里唯一与讲台正交相邻的一格，判定无歧义。</p>
     *
     * <p>讲台高度不匹配：框架底边在 y=1，台面在 y=0，讲台本身占 y=1（与底边同高），
     * 因为框架 ly=0 那一层就坐在台面上。</p>
     */
    private static final BlockPos LECTERN_OFFSET = new BlockPos(0, 1, 1);

    /** 传送门框架原点相对平台原点的偏移（框架沿 X 轴展开 4 宽、Z 轴 4 深）。 */
    private static final BlockPos PORTAL_OFFSET = new BlockPos(1, 1, 3);

    /**
     * 告示牌相对平台原点的偏移。
     *
     * <p>立在讲台同列（x=base+0）、台面最外沿（z=base+4），即世界 {@code (base.x, base.y+1, base.z+4)}。
     * 玩家面对讲台时一眼可见，且不挡门面、不挡讲台交互面。</p>
     *
     * <p><b>为什么不能再放在 (2,1,1)：</b>那个位置的世界坐标是 {@code (base.x+2, base.y+1, base.z+1)}，
     * 换算成框架局部坐标是 {@code (lx=2, lz=1)}——<b>正好落在框架体积内部</b>。而 {@code place()}
     * 里告示牌是先于框架放的，{@link PortalStructureHelper#placeFrameGeometry} 的预检又是
     * "任一格非空气且不可替换就 {@code return null} 且一格不放"，于是那格 {@code oak_sign}
     * 让它放弃整座门——实测结果是<b>台面、讲台、告示牌都在，框架 60 格全是空气</b>，
     * 玩家看到一座"没有门只有牌子"的岛。</p>
     *
     * <p>框架 footprint 由 {@code facing=NORTH} + {@link #PORTAL_OFFSET} 决定，实测为
     * {@code x∈[base.x+1, base.x+4]}、{@code z∈[base.z, base.z+3]}、{@code y∈[base.y+1, base.y+5]}。
     * 改动任一偏移量后必须重新核对这条边界，否则会重现"牌子吃掉门"。</p>
     */
    private static final BlockPos SIGN_OFFSET = new BlockPos(0, 1, 4);

    public AbandonedPortalStructure() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 找到水面高度（从海床向上扫描）
        int waterLevel = findWaterLevel(level, origin);
        if (waterLevel <= level.getMinBuildHeight()) {
            return false;
        }

        // 平台基准高度：水面 + ISLAND_HEIGHT（岛高出水面）
        int platformBaseY = waterLevel + ISLAND_HEIGHT;

        // 检测上方空间是否足够：按"平台原点 + 框架实际溢出方向"取范围。
        // 框架在 facing=NORTH 下占 x∈[1,4]、y∈[1,5]、z∈[0,3]（详见 hasSpace 的 Javadoc）。
        BlockPos platformBase = new BlockPos(origin.getX(), platformBaseY, origin.getZ());
        if (!hasSpace(level, platformBase)) {
            return false;
        }

        // 生成岛基（草方块+泥土+圆石）
        placeIsland(level, platformBase, random);

        // 放置底部石砖平台
        placePlatform(level, platformBase, random);

        // 讲台与告示牌必须在建完台面之后、<b>摆框架之前</b>放：框架的预检是"整体有全无"的，
        // 任何一格非空气都会让它放弃整座门；反过来，先把框架摆好再放讲台/牌子又会把框架块盖掉。
        // 讲台 (0,1,1) 与牌子 (0,1,4) 都已核对在框架体积外（见两个常量的 Javadoc），
        // 先放不会挡住预检。
        placeLectern(level, platformBase.offset(LECTERN_OFFSET));
        placeSign(level, platformBase.offset(SIGN_OFFSET));

        // 最后放置传送门（框架 + 传送门方块）
        placePortal(level, platformBase.offset(PORTAL_OFFSET));

        return true;
    }

    /**
     * 从给定高度向下扫描，找到"当前是水、上方是空气"的那一格，即水面。
     *
     * <p><b>为什么不能靠 {@code OCEAN_FLOOR_WG} 高度图定起点：</b>那个 Types 的
     * {@code Usage} 是 {@code WORLDGEN}，{@code keepAfterWorldgen()} 返回 false——
     * 它只在区块<b>生成过程中</b>被维护，生成完就被丢弃。活体 {@code ServerLevel} 上查它
     * 一律返回 {@code getMinBuildHeight()}。旧写法把返回的最小值当成扫描起点，于是循环
     * 从基岩层开始、根本扫不到水柱，{@code waterLevel} 恒为 minY，feature 直接放弃生成。
     * （{@link AbyssalSeepFeature} 同样查这个 Types，但它从下往上找"实心且上方是水"的海床，
     * 起点落到底部反而碰巧命中，所以那个 feature 看起来正常——这是巧合，不是正确性。）</p>
     *
     * <p>现在以调用方给的 {@code origin.y} 为起点向下扫。{@code origin.y} 由 placed_feature
     * 的 {@code minecraft:heightmap} 修饰符决定，世界生成时它<b>确实是</b>海床高度；
     * 手动 {@code /place feature} 时由指令参数给出，因此两种路径都可用。
     * 另外把搜索上界放到 {@code origin.y + 上限}，兼容 {@code origin} 落在海床略下方的情况。</p>
     *
     * @return 水面 y 坐标；找不到则返回 {@code level.getMinBuildHeight()}，调用方据此放弃生成
     */
    private static int findWaterLevel(WorldGenLevel level, BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();
        int minY = level.getMinBuildHeight();
        // 从 origin.y 起，允许向上探一点（origin 可能落在海床下）、向下扫到基岩。
        int startY = Math.min(origin.getY() + SCAN_UP_LIMIT, level.getMaxBuildHeight() - 2);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= minY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            // 水面：当前是水，上方是空气
            if (state.is(Blocks.WATER) && level.getBlockState(cursor.above()).isAir()) {
                return y;
            }
        }
        return minY;
    }

    /**
     * 检测"框架将要占的每一格"与"讲台/告示牌将要占的格"是否都是可替换空间（空气或水）。
     *
     * <p><b>范围不再手写数字，而是直接用 {@link #PORTAL_OFFSET} 与
     * {@link PortalStructureHelper} 的框架常量把 {@code placeFrameGeometry} 的映射复算一遍。</b>
     * 映射是 {@code worldX = cornerX + facing.stepX*lx + right.stepX*lz}、
     * {@code worldZ = cornerZ + facing.stepZ*lx + right.stepZ*lz}，{@code right = facing.getClockWise()}，
     * facing 固定 NORTH 时 {@code facing.step=(0,-1)}、{@code right.step=(1,0)}，即
     * {@code x = cornerX + lz}、{@code z = cornerZ - lx}。</p>
     *
     * <p><b>历史 bug（本方法的第三版）：</b>第一版只扫 {@code x∈[0,WIDTH)、z∈[0,DEPTH)}
     * 这种"只往正方向"的盒子，于是整条 {@code z=-1} 柱（20 格）根本没被检查，框架会被
     * 悄悄建到台面外面而预检仍返回 true。第二版改成"正负双向大盒子"，把已知的溢出方向包了进去，
     * 但它<b>同时把框架体积外的格子也算进来</b>——只要框架前方那几格被岛基泥土或台面以外的
     * 地形占住，整个 feature 就放弃生成（表现为"讲台和牌子都在、门凭空消失"，或整座岛都不出现）。
     * 现在这一版按真实体积判定：{@code hasSpace} 通过 ⟺ {@code placeFrameGeometry} 的预检也会通过。</p>
     *
     * <p>讲台/告示牌的位置必须一起查：它们的摆放时机在框架<b>之前</b>，若它们与框架体积重合，
     * 框架的预检会因为那一格非空气而放弃整座门——实测过，表现为"台面/讲台/牌子都在、框架 60 格
     * 全空气"（告示牌曾被放在框架局部 {@code (lx=2,lz=1)} 上，整座门因此消失）。
     * 这里把这条约束写成断言式的检查，日后谁挪了偏移量都会在这里立刻暴露。</p>
     */
    private static boolean hasSpace(WorldGenLevel level, BlockPos base) {
        BlockPos corner = base.offset(PORTAL_OFFSET);
        Direction facing = Direction.NORTH;
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // 1) 框架 4×5×4 的体积（含开口格；开口格之后会填空气，同样要求当前可替换）
        for (int lx = 0; lx < PortalStructureHelper.FRAME_WIDTH; lx++) {
            for (int ly = 0; ly < PortalStructureHelper.FRAME_HEIGHT; ly++) {
                for (int lz = 0; lz < PortalStructureHelper.FRAME_DEPTH; lz++) {
                    cursor.set(corner.getX() + facing.getStepX() * lx + right.getStepX() * lz,
                            corner.getY() + ly,
                            corner.getZ() + facing.getStepZ() * lx + right.getStepZ() * lz);
                    if (!isReplaceable(level.getBlockState(cursor))) {
                        return false;
                    }
                }
            }
        }

        // 2) 讲台与告示牌的落点（必须在框架体积之外，否则框架预检会失败）
        for (BlockPos p : new BlockPos[]{base.offset(LECTERN_OFFSET), base.offset(SIGN_OFFSET)}) {
            if (!isReplaceable(level.getBlockState(p))) {
                return false;
            }
        }
        return true;
    }

    /** {@code placeFrameGeometry} 的可替换判定口径，保持一致。 */
    private static boolean isReplaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(Blocks.WATER);
    }

    /**
     * 生成岛基：以平台中心为圆心，ISLAND_RADIUS 为半径，
     * 用草方块、泥土、圆石堆砌高出水面 ISLAND_HEIGHT 格的小岛。
     */
    private static void placeIsland(WorldGenLevel level, BlockPos platformBase, RandomSource random) {
        int cx = platformBase.getX() + WIDTH / 2;
        int cz = platformBase.getZ() + DEPTH / 2;
        int islandTopY = platformBase.getY() - 1; // 岛基顶部略低于平台
        int islandBottomY = islandTopY - ISLAND_HEIGHT + 1;

        for (int y = islandBottomY; y <= islandTopY; y++) {
            // 半径随高度递减，形成锥形
            int radius = ISLAND_RADIUS - (islandTopY - y);
            if (radius < 1) radius = 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    BlockPos pos = new BlockPos(cx + dx, y, cz + dz);
                    if (y == islandTopY) {
                        // 顶层：草方块（随机少量泥土）
                        level.setBlock(pos, random.nextFloat() < 0.3f ? Blocks.DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    } else if (y == islandBottomY) {
                        // 底层：圆石
                        level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                    } else {
                        // 中间层：泥土
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /**
     * 放置底部石砖平台（5×5，中心 3×3）。
     */
    private static void placePlatform(WorldGenLevel level, BlockPos base, RandomSource random) {
        BlockState main = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();

        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                BlockPos pos = base.offset(x, 0, z);
                if (x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1) {
                    // 边缘：苔石砖
                    level.setBlock(pos, mossy, 2);
                } else if (x == 1 || x == WIDTH - 2 || z == 1 || z == DEPTH - 2) {
                    // 次边缘：裂纹石砖
                    level.setBlock(pos, cracked, 2);
                } else {
                    // 中心：普通石砖
                    level.setBlock(pos, main, 2);
                }
            }
        }
    }

    /**
     * 放置讲台。<b>刻意不预插钥匙</b>。
     *
     * <p>《副本/17》§三定稿的教学触点是"<b>首次右键空讲台 → 提示需要钥匙 → 引导到钥匙获取路径</b>"。
     * 旧实现在这里 {@code setKeyStack()} 预插了一把 {@code t-1} 钥匙，等于把教学流程的第一步
     * 直接跳过去了：玩家右键看到的不是"需要钥匙"的提示，而是"进入副本"的对话框，
     * 同时那把钥匙还带着一个随机的、不对应任何实例的 UUID。空讲台 + 告示牌才是定稿口径。</p>
     */
    private static void placeLectern(WorldGenLevel level, BlockPos pos) {
        BlockState lecternState = ModBlocks.DUNGEON_LECTERN.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH);
        level.setBlock(pos, lecternState, 2);
        // 不再 setKeyStack：教学门必须从"空台面 + 提示需要钥匙"开始（《副本/17》§三）。
        // 钥匙由玩家走正常获取路径拿到后自己插上，此时 BlockEntity 的 HAS_KEY 会驱动模型切换。
    }

    /**
     * 放置传送门：只摆框架几何，不碰副本实例数据。
     *
     * <p><b>旧实现为什么一个字都放不下：</b>它调 {@link PortalStructureHelper#buildPortalStructure}，
     * 而那个入口的第一件事是 {@code DungeonInstanceManager.getInstance(instanceId)}，野外教学门
     * 传入的是 {@code new UUID(0L, 0L)} 占位符——实例必然不存在，于是 <b>函数直接 return null，
     * 方块一个都没摆</b>。野外因此永远看不到门，只剩孤零零的讲台和牌子。</p>
     *
     * <p>现在改走 {@link PortalStructureHelper#placeFrameGeometry}：纯几何，不查实例。
     * 实例数据由玩家首次经讲台权威入口 {@code DungeonEntryService.enter} 写入，
     * 这与《副本/17》"门常开、玩家不携带钥匙进副本"的口径一致。</p>
     *
     * <p>朝向刻意固定为 {@link Direction#NORTH}：世界生成阶段没有玩家上下文，随机的面朝方向
     * 会让门正面朝海或朝岛内，玩家找不到开口。facing=NORTH 下开口面朝 -Z，讲台落在框架西侧
     * （见 {@link #LECTERN_OFFSET}），仍是"贴底边外侧"的合法挂载。</p>
     */
    private static void placePortal(WorldGenLevel level, BlockPos cornerPos) {
        BlockPos r = PortalStructureHelper.placeFrameGeometry(level, cornerPos, Direction.NORTH);
    }

    /**
     * 立一块告示牌写明用法。
     *
     * <p>用竖直的 {@code oak_sign} 而非墙上的 {@code oak_wall_sign}，免得再算一次依附面朝向。</p>
     *
     * <p><b>为什么不能碰 {@code SignBlockEntity.updateText} / 任何 setter：</b>
     * 那些 setter 内部都会走 {@code markUpdated()} → {@code this.level.sendBlockUpdated(...)}，
     * 而这里的 {@code level} 是 {@link WorldGenLevel}（世界生成期的 {@code WorldGenRegion}）。
     * 该阶段 {@code setBlock} 只把方块与 BE 写进 {@code ChunkAccess}，<b>BE 的 {@code level}
     * 字段要等区块晋升为 LEVELCHUNK 时才被赋值</b>，所以此刻 {@code this.level == null}。</p>
     *
     * <p>后果不是"文字没写上"这么轻：{@code sendBlockUpdated} 直接抛 NPE，被
     * {@code ChunkStatusTasks.generateFeatures} 的调用方包成 {@code ReportedException: Feature placement}，
     * <b>整个 feature 连带已放好的岛、台面、门框一起回滚</b>。这就是"野外教学门有时整座不出现"
     * 的直接原因——此前它被 {@code placePortal} 的预检失败掩盖着（旧顺序里告示牌落在门框体积内，
     * 预检先 return null，门消失、告示牌照样 NPE，两个错误叠在一起）。</p>
     *
     * <p>正确做法：把 {@code front_text} 用 {@link net.minecraft.world.level.block.entity.SignText#DIRECT_CODEC}
     * 编成 NBT，再交给 {@code BlockEntity.load()} 灌进 BE。{@code loadAdditional} 只写字段、
     * 不调 {@code markUpdated}，所以不依赖 {@code level} 是否已就绪，落盘时文字随 BE 一起序列化。</p>
     */
    private static void placeSign(WorldGenLevel level, BlockPos pos) {
        BlockState signState = Blocks.OAK_SIGN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8);
        level.setBlock(pos, signState, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign)) {
            return;
        }

        // 先按"空台面"的现有 SignText 取样式基底，再逐行换上文案（setMessage 返回新实例，不改原对象）
        var text = sign.getFrontText()
                .setMessage(0, Component.literal("深渊传送门"))
                .setMessage(1, Component.literal("讲台插钥匙后"))
                .setMessage(2, Component.literal("走进门框即进入"))
                .setMessage(3, Component.literal("右键门只会提示"));

        // 用 codec 编出 front_text 的 NBT，再 load 进 BE —— 全程不触碰会调 markUpdated 的 setter
        var ops = net.minecraft.nbt.NbtOps.INSTANCE;
        var encoded = net.minecraft.world.level.block.entity.SignText.DIRECT_CODEC
                .encodeStart(ops, text)
                .resultOrPartial(err -> PiranPort.LOGGER.error("告示牌文字编码失败: {}", err));
        if (encoded.isEmpty()) {
            return;
        }
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.put("front_text", encoded.get());
        sign.loadWithComponents(tag, level.registryAccess());
    }
}
