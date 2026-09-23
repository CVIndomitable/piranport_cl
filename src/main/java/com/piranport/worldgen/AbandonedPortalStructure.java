package com.piranport.worldgen;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.block.PortalStructureHelper;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.UUID;

/**
 * 废弃传送门结构：在水面上方自然生成小岛，作为副本教学触点。
 *
 * <p>结构组成：
 * <ul>
 *   <li>底部：石砖平台（5×5，贴地）</li>
 *   <li>岛基：草方块+泥土+圆石堆砌的小岛，高出水面 1~2 格</li>
 *   <li>中心：{@code dungeon_lectern}（讲台），预设教学关卡钥匙（{@code t-1}）</li>
 *   <li>前方：2×3 深渊传送门（{@code abyssal_portal_frame} 框架 + {@code abyssal_portal} 方块）</li>
 * </ul>
 *
 * <p>策划依据：《副本/09》讲台+钥匙合并模式、《副本/17》野外讲台作为教学触点。
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

        // 检测上方空间是否足够（结构需要 HEIGHT 格空间）
        BlockPos platformBase = new BlockPos(origin.getX(), platformBaseY, origin.getZ());
        if (!level.ensureCanWrite(platformBase) || !hasSpace(level, platformBase)) {
            return false;
        }

        // 生成岛基（草方块+泥土+圆石）
        placeIsland(level, platformBase, random);

        // 放置底部石砖平台
        placePlatform(level, platformBase, random);

        // 放置讲台（带预设钥匙）
        placeLectern(level, platformBase.offset(LECTERN_OFFSET));

        // 放置传送门（框架 + 传送门方块）
        placePortal(level, platformBase.offset(PORTAL_OFFSET));

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
     * 检测结构上方是否有足够空间（空气或水）。
     */
    private static boolean hasSpace(WorldGenLevel level, BlockPos base) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
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
     * 放置讲台，并预插入教学关卡钥匙（stage_id = "t-1"）。
     */
    private static void placeLectern(WorldGenLevel level, BlockPos pos) {
        // 放置讲台方块
        BlockState lecternState = ModBlocks.DUNGEON_LECTERN.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH);
        level.setBlock(pos, lecternState, 2);

        // 获取 BlockEntity 并插入教学钥匙
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DungeonLecternBlockEntity lecternBE) {
            // 创建教学关卡钥匙
            ItemStack key = new ItemStack(com.piranport.registry.ModItems.DUNGEON_KEY.get());
            key.set(com.piranport.registry.ModDataComponents.DUNGEON_STAGE_ID.get(), "t-1");
            key.set(com.piranport.registry.ModDataComponents.DUNGEON_INSTANCE_ID.get(), UUID.randomUUID());
            key.set(com.piranport.registry.ModDataComponents.DUNGEON_PROGRESS.get(), DungeonProgress.EMPTY);

            // 使用公开方法设置钥匙（无需反射）
            lecternBE.setKeyStack(key);
        }
    }

    /**
     * 放置传送门：统一走 {@link PortalStructureHelper} 构建，不再自己写裸 setBlock 循环。
     *
     * <p>旧实现有个致命 bug：先在 [0,1,0][0,2,0][1,1,0][1,2,0] 放框架方块，
     * 紧接着又用传送门方块覆盖同样四格，等于把门柱自己拆了；而且用的是已废弃的
     * {@code abyssal_portal_frame}/{@code abyssal_portal} 旧口径。
     * 现在改用 {@code dungeon_portal} 框架 + 前景单层 2×3 空气开口（甲），
     * 由 helper 保证几何与运行时判定完全一致。</p>
     */
    private static void placePortal(WorldGenLevel level, BlockPos center) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            // 世界生成阶段拿不到 ServerLevel 时退化为直接摆框架块（几何仍与 helper 一致）。
            placePortalBlocksDirect(level, center);
            return;
        }
        // helper 需要 instanceId/nodeId；教学门的实例尚未创建，先构建纯结构，
        // 数据由玩家首次进入时经讲台权威入口写入。
        PortalStructureHelper.buildPortalStructure(serverLevel, center,
                new UUID(0L, 0L), "t-1");
    }

    /** 无 ServerLevel 时的退化路径：按 helper 的几何摆放，开口留空气。 */
    private static void placePortalBlocksDirect(WorldGenLevel level, BlockPos origin) {
        BlockState frame = ModBlocks.DUNGEON_PORTAL.get().defaultBlockState();
        // 单朝向（南北向）足够，形状对齐 helper 的 FRAME_WIDTH/HEIGHT/DEPTH。
        for (int lx = 0; lx < PortalStructureHelper.FRAME_WIDTH; lx++) {
            for (int ly = 0; ly < PortalStructureHelper.FRAME_HEIGHT; ly++) {
                for (int lz = 0; lz < PortalStructureHelper.FRAME_DEPTH; lz++) {
                    BlockPos pos = origin.offset(lx, ly, lz);
                    if (PortalStructureHelper.isInOpening(lx, ly, lz)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, frame, 2);
                    }
                }
            }
        }
    }
}
