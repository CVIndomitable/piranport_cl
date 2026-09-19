package com.piranport.worldgen;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.DungeonLecternBlockEntity;
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

    /** 讲台相对平台原点的偏移 */
    private static final BlockPos LECTERN_OFFSET = new BlockPos(2, 1, 2);

    /** 传送门框架相对平台原点的偏移（讲台前方 1 格） */
    private static final BlockPos PORTAL_OFFSET = new BlockPos(2, 1, 3);

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
     * 放置传送门（2×3 框架 + 内部传送门方块）。
     */
    private static void placePortal(WorldGenLevel level, BlockPos center) {
        BlockState frame = ModBlocks.ABYSSAL_PORTAL_FRAME.get().defaultBlockState();
        BlockState portal = ModBlocks.ABYSSAL_PORTAL.get().defaultBlockState();

        // 左柱（x=0）
        level.setBlock(center.offset(0, 0, 0), frame, 2);
        level.setBlock(center.offset(0, 1, 0), frame, 2);
        level.setBlock(center.offset(0, 2, 0), frame, 2);

        // 右柱（x=1）
        level.setBlock(center.offset(1, 0, 0), frame, 2);
        level.setBlock(center.offset(1, 1, 0), frame, 2);
        level.setBlock(center.offset(1, 2, 0), frame, 2);

        // 顶部横梁（y=3）
        level.setBlock(center.offset(0, 3, 0), frame, 2);
        level.setBlock(center.offset(1, 3, 0), frame, 2);

        // 内部传送门方块（y=1, y=2）
        level.setBlock(center.offset(0, 1, 0), portal, 2);
        level.setBlock(center.offset(1, 1, 0), portal, 2);
        level.setBlock(center.offset(0, 2, 0), portal, 2);
        level.setBlock(center.offset(1, 2, 0), portal, 2);
    }
}
