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
 * 废弃传送门结构：在世界海洋中自然生成，作为副本教学触点。
 *
 * <p>结构组成（5×4×5）：
 * <ul>
 *   <li>中心：{@code dungeon_lectern}（讲台），预设教学关卡钥匙（{@code t-1}）</li>
 *   <li>前方：2×3 深渊传送门（{@code abyssal_portal_frame} 框架 + {@code abyssal_portal} 方块）</li>
 *   <li>底部：石砖平台，带苔石/裂纹石砖装饰</li>
 * </ul>
 *
 * <p>策划依据：《副本/09》讲台+钥匙合并模式、《副本/17》野外讲台作为教学触点。
 */
public class AbandonedPortalStructure extends Feature<NoneFeatureConfiguration> {

    /** 结构尺寸：宽×高×深 */
    private static final int WIDTH = 5;
    private static final int HEIGHT = 4;
    private static final int DEPTH = 5;

    /** 讲台相对结构原点的偏移 */
    private static final BlockPos LECTERN_OFFSET = new BlockPos(2, 1, 2);

    /** 传送门框架相对讲台的偏移（讲台前方 1 格） */
    private static final BlockPos PORTAL_OFFSET = new BlockPos(2, 1, 3);

    public AbandonedPortalStructure() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 找到海床高度
        int seaFloor = findSeaFloor(level, origin);
        if (seaFloor < level.getMinBuildHeight() + 1) {
            return false;
        }

        BlockPos base = new BlockPos(origin.getX(), seaFloor, origin.getZ());

        // 检测是否适合放置（海床不能是空气，上方需要 4 格空间）
        if (!level.ensureCanWrite(base) || !hasSpace(level, base)) {
            return false;
        }

        // 放置底部石砖平台
        placePlatform(level, base, random);

        // 放置讲台（带预设钥匙）
        placeLectern(level, base.offset(LECTERN_OFFSET));

        // 放置传送门（框架 + 传送门方块）
        placePortal(level, base.offset(PORTAL_OFFSET));

        return true;
    }

    /**
     * 在海床位置向上寻找足够空间。
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
     * 找到海床 Y 坐标。
     */
    private static int findSeaFloor(WorldGenLevel level, BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        int minY = level.getMinBuildHeight() + 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = Math.min(top, level.getMaxBuildHeight() - 2); y >= minY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()
                    && !state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                    && level.getFluidState(cursor.above()).is(net.minecraft.tags.FluidTags.WATER)) {
                return y;
            }
        }
        return minY - 1;
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
