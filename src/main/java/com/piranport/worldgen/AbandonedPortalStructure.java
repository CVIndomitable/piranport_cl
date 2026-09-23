package com.piranport.worldgen;

import com.piranport.dungeon.block.PortalStructureHelper;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
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
     * 讲台相对平台原点的偏移。
     *
     * <p>讲台必须<b>贴在框架底边外侧</b>（《副本/17》定稿 + 项目所有者 2026-09-23 决策“讲台贴着的
     * 传送门框架生效”），否则门不工作。旧值 (2,1,2) 把讲台放在门前一格、与框架不相邻，正是
     * 教学门点了没反应的根因。</p>
     *
     * <p>框架原点为 PORTAL_OFFSET，底边层是 ly==0 的 4×4；讲台放在底边正前方
     * （门面一侧）一格，与门槛中央 (lx=1,lz=0)/(lx=2,lz=0) 正交相邻。</p>
     */
    private static final BlockPos LECTERN_OFFSET = new BlockPos(3, 1, 1);

    /** 传送门框架原点相对平台原点的偏移（框架沿 X 轴展开 4 宽、Z 轴 4 深）。 */
    private static final BlockPos PORTAL_OFFSET = new BlockPos(1, 1, 2);

    /**
     * 告示牌相对平台原点的偏移。
     *
     * <p>立在讲台的另一侧（讲台在 (3,1,1)，牌在 (2,1,1)），不挡门面也不挡讲台交互面。
     * 教学门此前最大的问题是玩家不知道"要往门里走"而不是"右键门"——告示牌把这条
     * 写死在场景里，不依赖玩家读过策划文档。</p>
     */
    private static final BlockPos SIGN_OFFSET = new BlockPos(2, 1, 1);

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

        // 检测上方空间是否足够：按"结构包围盒 + 传送门框架外扩"取范围，
        // 因为传送门从 PORTAL_OFFSET(1,1,2) 起向 +X/+Y/+Z 各展开 4/5/4 格，
        // 最高处 y=5、最远处 z=5，都超出了 WIDTH/HEIGHT/DEPTH 起的包围盒。
        BlockPos platformBase = new BlockPos(origin.getX(), platformBaseY, origin.getZ());
        if (!level.ensureCanWrite(platformBase) || !hasSpace(level, platformBase)) {
            return false;
        }

        // 生成岛基（草方块+泥土+圆石）
        placeIsland(level, platformBase, random);

        // 放置底部石砖平台
        placePlatform(level, platformBase, random);

        // 放置讲台（空台面，不预插钥匙，见 placeLectern 的 Javadoc）
        placeLectern(level, platformBase.offset(LECTERN_OFFSET));

        // 放置传送门（框架 + 传送门方块）
        placePortal(level, platformBase.offset(PORTAL_OFFSET));

        // 放置告示牌（写着怎么用），立在讲台旁边
        placeSign(level, platformBase.offset(SIGN_OFFSET));

        return true;
    }

    /**
     * 向上扫描找到第一个非空气且上方是空气的方块（水面高度）。
     */
    private static int findWaterLevel(WorldGenLevel level, BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = Math.min(top, level.getMaxBuildHeight() - 2); y >= minY; y--) {
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
     * 检测结构占位范围内是否全部为可替换空间（空气或水）。
     *
     * <p>扫描范围必须覆盖<b>所有真正会被写入的格子</b>，而不只是 {@code WIDTH×HEIGHT×DEPTH}
     * 这个名义包围盒：传送门框架从 {@code PORTAL_OFFSET(1,1,2)} 起算，向 +X 展 4 宽、
     * 向 +Y 展 5 高、向 +Z 展 4 深，于是它的占位是 {@code x∈[1,4]}、{@code y∈[1,5]}、
     * {@code z∈[2,5]}——y 与 z 各比名义包围盒多出 1 格（旧写法只查 y∈[0,3]、z∈[0,4]）。
     * 只要这两格恰好压在水下礁石或地形上，{@link PortalStructureHelper#placeFrameGeometry}
     * 的整体预检就会失败并<b>一格不放</b>，表现为"讲台和牌子都在、门凭空消失"。
     * 所以这里把范围扩到 y∈[0,5]、z∈[0,5]，宁可让整个 feature 放弃生成，也不要生成半截结构。</p>
     */
    private static boolean hasSpace(WorldGenLevel level, BlockPos base) {
        for (int y = 0; y <= HEIGHT + 1; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z <= DEPTH; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.WATER)) {
                        return false;
                    }
                }
            }
        }
        return true;
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
     * 会让门正面朝海或朝岛内，玩家找不到开口。讲台在框架的西侧（LECTERN_OFFSET），
     * 与门面同处一侧，保证"贴底边"的挂载判定成立。</p>
     */
    private static void placePortal(WorldGenLevel level, BlockPos cornerPos) {
        PortalStructureHelper.placeFrameGeometry(level, cornerPos, Direction.NORTH);
    }

    /**
     * 立一块告示牌写明用法。
     *
     * <p>1.21.1 的告示牌文字不是逐行 setter：正/反面各是一个不可变的 {@code SignText}，
     * 要整份换掉（{@code SignText.setMessage} 返回新实例，最后交给
     * {@code updateText(text, frontSide)} 落到 BE）。用竖直的 {@code oak_sign} 而非墙上的
     * {@code oak_wall_sign}，免得再算一次依附面朝向。</p>
     */
    private static void placeSign(WorldGenLevel level, BlockPos pos) {
        BlockState signState = Blocks.OAK_SIGN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8);
        level.setBlock(pos, signState, 2);
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
            var text = sign.getFrontText()
                    .setMessage(0, Component.literal("深渊传送门"))
                    .setMessage(1, Component.literal("讲台插钥匙后"))
                    .setMessage(2, Component.literal("走进门框即进入"))
                    .setMessage(3, Component.literal("右键门只会提示"));
            sign.updateText(ignored -> text, true);
        }
    }
}
