package com.piranport.dungeon.block;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.entity.DungeonPortalBlockEntity;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for checking multi-block portal structure completeness.
 * Validates that a 4×5×4 frame surrounds a 2×3 interior opening.
 *
 * <p><b>开口只有前景一层（lz==0）。</b>历史实现的 isInOpening 谓词只看 lx/ly，不看 lz，
 * 于是"2×3 开口"实际是贯穿前后 4 层的 2×3×4 隧道（24 格空气），前后两面没有门楣封口，
 * 外观不像下界门。按《副本/17》"讲台 + 传送门多方块 = 完整传送门"的定稿口径，
 * 开口应当是单层 2×3 空洞，其余位置一律是框架块。</p>
 *
 * <p><b>几何判定只在本类实现一次。</b>DungeonPortalBlockEntity 曾各自复写一份同样的循环，
 * 两份逻辑一旦不同步就会出现"客户端看着是门、服务端不认"的漂移。全部改走
 * {@link #isInOpening(int, int, int)} 与 {@link #isCompleteAt(Level, BlockPos, Direction)}。</p>
 */
public class PortalStructureHelper {

    // Portal frame structure dimensions
    public static final int FRAME_WIDTH = 4;
    public static final int FRAME_HEIGHT = 5;
    public static final int FRAME_DEPTH = 4;
    public static final int PORTAL_WIDTH = 2;
    public static final int PORTAL_HEIGHT = 3;

    /**
     * 给定框架局部坐标，判断该格是否属于开口（应当为空气）。
     *
     * <p>开口 = 前景单层（lz==0）上居中的 2 宽 × 3 高区域。这是全项目唯一的开口定义，
     * helper 与 BlockEntity 必须共用，避免再次漂移。</p>
     *
     * @param lx 沿 facing 轴的局部坐标（0..FRAME_WIDTH-1）
     * @param ly 垂直局部坐标（0..FRAME_HEIGHT-1）
     * @param lz 沿 facing.getClockWise() 的局部坐标（0..FRAME_DEPTH-1）
     */
    public static boolean isInOpening(int lx, int ly, int lz) {
        return lz == 0
                && lx >= (FRAME_WIDTH - PORTAL_WIDTH) / 2
                && lx < (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH
                && ly >= (FRAME_HEIGHT - PORTAL_HEIGHT) / 2
                && ly < (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT;
    }

    /** 开口沿 facing 轴的起始局部坐标（左边缘）。 */
    public static int openingMinX() { return (FRAME_WIDTH - PORTAL_WIDTH) / 2; }
    /** 开口垂直起始局部坐标（下边缘）。 */
    public static int openingMinY() { return (FRAME_HEIGHT - PORTAL_HEIGHT) / 2; }

    /**
     * 给定框架角点与朝向，判断该朝向下的完整框架是否成立。
     *
     * <p>这是几何判定的唯一权威入口；BlockEntity 从任意框架块反推角点后也调这里。</p>
     */
    public static boolean isCompleteAt(Level level, BlockPos origin, Direction facing) {
        if (level == null || origin == null) return false;
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    mutable.set(origin.getX() + facing.getStepX() * x + right.getStepX() * z,
                            origin.getY() + y,
                            origin.getZ() + facing.getStepZ() * x + right.getStepZ() * z);
                    BlockState state = level.getBlockState(mutable);
                    if (isInOpening(x, y, z)) {
                        // 甲：开口必须是空气，不允许任何填充方块
                        if (!state.isAir()) return false;
                    } else if (!state.is(com.piranport.registry.ModBlocks.DUNGEON_PORTAL.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 已解析出的框架方位：角点 + 正面朝向。 */
    public record OrientedFrame(BlockPos origin, Direction facing) {}

    /**
     * 从任意一个框架块出发，向四个水平朝向反推可能的角点，返回第一个成立的框架方位。
     *
     * <p>BlockEntity 不保证落在角点上（多方块结构里任何一块都可能是它的宿主），
     * 因此不能像旧实现那样直接把自身当角点遍历——那样只有恰好位于角点的 BE 才能判定成立。
     * 这里把自身代入 4×5×4 体积内的每一个局部坐标，逐个反推 origin，再交给
     * {@link #isCompleteAt} 裁决。</p>
     */
    public static OrientedFrame findFrame(Level level, BlockPos anyFrameBlock) {
        if (level == null || anyFrameBlock == null) return null;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Direction right = facing.getClockWise();
            for (int x = 0; x < FRAME_WIDTH; x++) {
                for (int y = 0; y < FRAME_HEIGHT; y++) {
                    for (int z = 0; z < FRAME_DEPTH; z++) {
                        BlockPos origin = anyFrameBlock
                                .offset(-facing.getStepX() * x - right.getStepX() * z,
                                        -y,
                                        -facing.getStepZ() * x - right.getStepZ() * z);
                        if (isCompleteAt(level, origin, facing)) {
                            return new OrientedFrame(origin, facing);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 开口的空间包围盒，用于 ticker 检测"玩家是否站进了门里"。
     *
     * <p>开口是空气、没有方块承载 {@code entityInside}，所以进门判定必须由框架侧主动扫描
     * 这个 AABB，而不能指望玩家碰到某个方块。</p>
     */
    public static AABB getOpeningAABB(BlockPos origin, Direction facing) {
        Direction right = facing.getClockWise();
        BlockPos min = origin.relative(facing, openingMinX()).above(openingMinY());
        BlockPos max = min.relative(facing, PORTAL_WIDTH).above(PORTAL_HEIGHT).relative(right, 1);
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    /**
     * Checks if a complete portal structure exists at the given position.
     * Tries multiple orientations to find a valid frame.
     *
     * @param level The level to check in
     * @param pos The position of one corner block of the frame
     * @return true if a complete portal structure is found
     */
    public static boolean isStructureComplete(Level level, BlockPos pos) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (isStructureCompleteAtOrientation(level, pos, facing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a portal structure exists with the given corner and facing direction.
     */
    private static boolean isStructureCompleteAtOrientation(Level level, BlockPos origin, Direction facing) {
        if (level == null || origin == null) {
            return false;
        }

        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    // Skip the interior opening (2×3, front layer only)
                    boolean isInOpening = isInOpening(x, y, z);

                    mutable.set(origin.getX() + facing.getStepX() * x + right.getStepX() * z,
                            origin.getY() + y,
                            origin.getZ() + facing.getStepZ() * x + right.getStepZ() * z);

                    BlockState state = level.getBlockState(mutable);

                    if (isInOpening) {
                        if (!state.isAir()) {
                            return false;
                        }
                    } else {
                        // Frame must be made of dungeon portal blocks
                        if (!state.is(com.piranport.registry.ModBlocks.DUNGEON_PORTAL.get())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Finds the center position of the portal interior (where players teleport).
     *
     * @param level The level to check in
     * @param cornerPos The position of the bottom corner block
     * @return The center position of the portal opening, or null if not found
     */
    public static BlockPos getPortalCenter(Level level, BlockPos cornerPos) {
        if (level == null || cornerPos == null) {
            return null;
        }

        // Try all orientations
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos center = getPortalCenterAtOrientation(level, cornerPos, facing);
            if (center != null) {
                return center;
            }
        }
        return null;
    }

    /**
     * Gets the center position for a specific orientation.
     */
    private static BlockPos getPortalCenterAtOrientation(Level level, BlockPos origin, Direction facing) {
        if (level == null || origin == null) {
            return null;
        }

        // Verify structure is complete first
        if (!isStructureCompleteAtOrientation(level, origin, facing)) {
            return null;
        }

        Direction right = facing.getClockWise();

        // Calculate center of the opening
        int openingCenterX = (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH / 2;
        int openingCenterY = (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT / 2;

        return origin.relative(facing, openingCenterX)
                .relative(Direction.UP, openingCenterY)
                .relative(right, (FRAME_DEPTH - 1) / 2);
    }

    /**
     * Gets the AABB of the portal interior (for collision/entity detection).
     */
    public static AABB getPortalInteriorAABB(BlockPos cornerPos, Direction facing) {
        if (cornerPos == null) {
            return null;
        }

        BlockPos min = cornerPos
                .relative(facing, (FRAME_WIDTH - PORTAL_WIDTH) / 2)
                .relative(Direction.UP, (FRAME_HEIGHT - PORTAL_HEIGHT) / 2);

        BlockPos max = min.relative(facing, PORTAL_WIDTH)
                .relative(Direction.UP, PORTAL_HEIGHT)
                .relative(facing.getClockWise(), FRAME_DEPTH);

        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ());
    }

    /**
     * 只摆出 {@code dungeon_portal} 框架几何（4×5×4，前景单层 2×3 开口留空气），不碰副本实例数据。
     *
     * <p><b>为什么需要这个入口：</b>{@link #buildPortalStructure} 承担两件事——建实例相关的门
     * （发奖钩子、传送门实体、BE 的 instanceId/nodeId），以及摆方块几何。野外/结构生成想要的是
     * 后者，但那个入口会先查 {@code getInstance(instanceId)}，实例不存在就整段返回 null，
     * 结果一个字方块都放不下——这正是"野外永远看不到废弃传送门"的直接原因之一。</p>
     *
     * <p>{@code facing} 必须由调用方给定：世界生成阶段没有玩家上下文，此前 buildPortalStructure
     * 靠遍历四个水平朝向碰运气，既依赖周围是否可替换，落到哪个朝向也不确定。</p>
     *
     * @param level     目标世界。参数类型刻意用 {@link net.minecraft.world.level.LevelAccessor} 而不是
     *                  {@code Level}：世界生成阶段拿到的是 {@code WorldGenLevel}，它<b>只实现了
     *                  LevelAccessor</b>，不继承 {@code Level}，用 {@code Level} 会编译不过。
     *                  本方法只用到 getBlockState/setBlock/getBlockEntity，这三者都在 LevelAccessor 上。
     * @param cornerPos 框架角点（局部 lx=0, ly=0, lz=0 那一格）
     * @param facing    框架正面朝向（决定开口在哪一面）
     * @return 开口中心坐标；若任一框架格被不可替换的方块占据则返回 null 且不放任何方块
     */
    public static BlockPos placeFrameGeometry(net.minecraft.world.level.LevelAccessor level,
                                              BlockPos cornerPos, Direction facing) {
        if (level == null || cornerPos == null || facing == null || !facing.getAxis().isHorizontal()) {
            return null;
        }
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // 先整体检查后整体放置：避免占位检查失败时留下半截框架。
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    if (isInOpening(x, y, z)) continue;
                    cursor.set(cornerPos.getX() + facing.getStepX() * x + right.getStepX() * z,
                            cornerPos.getY() + y,
                            cornerPos.getZ() + facing.getStepZ() * x + right.getStepZ() * z);
                    BlockState existing = level.getBlockState(cursor);
                    if (!existing.isAir() && !existing.canBeReplaced()) {
                        return null;
                    }
                }
            }
        }

        BlockState frame = com.piranport.registry.ModBlocks.DUNGEON_PORTAL.get().defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    cursor.set(cornerPos.getX() + facing.getStepX() * x + right.getStepX() * z,
                            cornerPos.getY() + y,
                            cornerPos.getZ() + facing.getStepZ() * x + right.getStepZ() * z);
                    level.setBlock(cursor, isInOpening(x, y, z) ? air : frame, Block.UPDATE_ALL);
                }
            }
        }

        // 让框架自带的 BE 立即复核结构，这样刚生成出来的门也处于"可判定"状态。
        BlockPos bePos = cornerPos.relative(facing, openingMinX()).relative(right, (FRAME_DEPTH - 1) / 2);
        if (level.getBlockEntity(bePos) instanceof DungeonPortalBlockEntity portalBE) {
            portalBE.checkStructure();
        }

        return cornerPos.relative(facing, openingMinX() + PORTAL_WIDTH / 2)
                .above(openingMinY() + PORTAL_HEIGHT / 2)
                .relative(right, (FRAME_DEPTH - 1) / 2);
    }

    /**
     * Builds a complete dungeon exit site at the given anchor.
     *
     * <p>出口布局（副本/新需求）：以 {@code cornerPos} 为锚点生成一个 3×3 石砖平台，
     * 平台中心向上竖一列海晶灯光柱便于远处辨认，
     * 平台一侧竖一副 4×5×4 的 {@code dungeon_portal} 门框（单层 2×3 开口朝外，类下界门），
     * 门内放 {@code DungeonPortalEntity} 作为"走进入口"的传送判定。
     * 节点奖励由 {@code settleNode} 直接发进玩家背包，这里不再放战利品箱。</p>
     *
     * <p>坐标约定：{@code cornerPos} 是门框局部 (lx=0,ly=0,lz=0) 那一格，
     * 与历史调用方保持一致（多数传 {@code anchor.below()}）。平台铺在门框开口前方一格，
     * 顶面与门框底面同高，玩家站上平台即可开箱或走进门。</p>
     */
    public static BlockPos buildPortalStructure(ServerLevel level, BlockPos cornerPos,
                                                UUID instanceId, String nodeId) {
        if (level == null || cornerPos == null || instanceId == null || nodeId == null) {
            return null;
        }

        // 此入口由已满足胜利条件的战斗/脚本调用；发奖不依赖玩家是否碰到出口。
        DungeonInstance instance = com.piranport.dungeon.instance.DungeonInstanceManager.get(level)
                .getInstance(instanceId);
        if (instance == null) return null;
        com.piranport.dungeon.event.DungeonEventHandler.onNodeCompleted(level, instance, nodeId);

        // 《副本/22》多后继节点：每个未清后继一扇门，玩家走进哪扇就去哪个节点。
        // 单后继/无后继保持原来的单门（target=null 时由 DungeonProgressionRouter 自动选路）。
        List<String> targets = doorTargets(instance, nodeId);
        if (targets.size() <= 1) {
            return buildOneExitSite(level, cornerPos, instanceId, nodeId,
                    targets.isEmpty() ? null : targets.get(0));
        }

        // 门沿 Z 轴横排，间距 14 格：4×5×4 门框 + 3×3 平台 + 前方通道不会互相压住。
        int spacing = 14;
        int start = -((targets.size() - 1) * spacing) / 2;
        BlockPos first = null;
        for (int i = 0; i < targets.size(); i++) {
            BlockPos anchor = cornerPos.offset(0, 0, start + i * spacing);
            BlockPos center = buildOneExitSite(level, anchor, instanceId, nodeId, targets.get(i));
            if (center == null) {
                // 多门放不下时退化为单门自动选路；已经放下的门保留，玩家仍有路可走。
                return buildOneExitSite(level, cornerPos, instanceId, nodeId, null);
            }
            if (first == null) first = center;
        }
        return first;
    }

    /** 该节点应生成哪些门：普通节点取全部未清直连后继；分支节点仍走单门自动带路。 */
    private static List<String> doorTargets(DungeonInstance instance, String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        if (node == null || node.hasBranches()) return List.of();
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        for (StageData.EdgeData edge : stage.edges()) {
            if (!edge.from().equals(nodeId)) continue;
            if (!stage.nodes().containsKey(edge.to())) continue;
            if (instance.getClearedNodes().contains(edge.to())) continue;
            targets.add(edge.to());
        }
        return List.copyOf(targets);
    }

    private static BlockPos buildOneExitSite(ServerLevel level, BlockPos cornerPos, UUID instanceId,
                                             String nodeId, String targetNodeId) {
        // 遍历四个水平朝向，选第一个能完整放下的：门框 + 前方 3×3 平台 + 光柱。
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos center = tryBuildExitSite(level, cornerPos, facing, instanceId, nodeId, targetNodeId);
            if (center != null) {
                return center;
            }
        }
        return tryBuildExitSite(level, cornerPos, Direction.NORTH, instanceId, nodeId, targetNodeId);
    }

    /**
     * 在 {@code facing} 朝向下尝试布置整套出口：3×3 平台、光柱、门框、传送门实体。
     * 返回开口中心坐标；放不下去则返回 null 且不残留半成品（门框预检沿用原逻辑，
     * 平台与箱子位在覆盖前只做"可替换"判定）。</p>
     */
    private static BlockPos tryBuildExitSite(ServerLevel level, BlockPos cornerPos,
                                             Direction facing, UUID instanceId, String nodeId,
                                             String targetNodeId) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        // 平台位于门框开口正前方一格，3×3，顶面与角点同 Y。
        BlockPos platformCenter = cornerPos.relative(back, 2).above(1)
                .relative(right, (FRAME_DEPTH - 1) / 2)
                .relative(facing, openingMinX() + PORTAL_WIDTH / 2);

        // 平台本身必须是可替换/空气（不许压住实心方块），否则整体换朝向。
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = platformCenter.offset(dx, 0, dz);
                BlockState existing = level.getBlockState(p);
                if (!existing.isAir() && !existing.canBeReplaced()) return null;
                // 光柱和门要占平台上方一格，也必须是空气/可替换。
                BlockPos above = p.above();
                BlockState aboveState = level.getBlockState(above);
                if (!aboveState.isAir() && !aboveState.canBeReplaced()) return null;
            }
        }

        BlockPos center = tryBuildPortalStructure(level, cornerPos, facing, instanceId, nodeId,
                targetNodeId);
        if (center == null) return null;

        // 平台：石砖主体，四角黑曜石包边——沿用副本要塞平台（STONE_BRICKS）+ 祭坛（OBSIDIAN）的方块语汇。
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = platformCenter.offset(dx, 0, dz);
                boolean edge = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                level.setBlock(p, edge
                        ? net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }

        // 光柱：平台中心向上一列海晶灯（10 格），让玩家在副本维度远处能看到出口方位。
        // 奖励已改回 settleNode 直接进背包，这里不再放战利品箱。
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos beam = platformCenter.above(dy);
            BlockState existing = level.getBlockState(beam);
            if (!existing.isAir() && !existing.canBeReplaced()) break;
            level.setBlock(beam,
                    net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }

        // 传送门实体放在开口中心偏下（玩家走入即触发），替代旧版放在角点顶上的位置。
        var portal = com.piranport.dungeon.entity.DungeonPortalEntity.create(level, instanceId, nodeId,
                targetNodeId, center.getX() + .5, center.getY(), center.getZ() + .5);
        if (portal != null) level.addFreshEntity(portal);
        return center;
    }

    /**
     * Attempts to build a portal structure with a specific facing direction.
     * Returns the center of the portal interior on success, null on failure.
     */
    private static BlockPos tryBuildPortalStructure(ServerLevel level, BlockPos cornerPos,
                                                     Direction facing, UUID instanceId, String nodeId,
                                                     String targetNodeId) {
        if (level == null || cornerPos == null) {
            return null;
        }

        // Check if we have enough space in all directions
        Direction right = facing.getClockWise();
        BlockPos maxPos = cornerPos.relative(facing, FRAME_WIDTH - 1)
                .relative(Direction.UP, FRAME_HEIGHT - 1)
                .relative(right, FRAME_DEPTH - 1);

        // Check all positions are loadable
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    BlockPos checkPos = cornerPos.relative(facing, x)
                            .relative(Direction.UP, y)
                            .relative(right, z);

                    // Skip interior for space check
                    boolean isInOpening = isInOpening(x, y, z);

                    if (!isInOpening) {
                        // Frame block should be replaceable
                        BlockState existing = level.getBlockState(checkPos);
                        if (!existing.isAir() && !existing.canBeReplaced()) {
                            return null;
                        }
                    }
                }
            }
        }

        // Place the frame blocks
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    boolean isInOpening = isInOpening(x, y, z);

                    BlockPos placePos = cornerPos.relative(facing, x)
                            .relative(Direction.UP, y)
                            .relative(right, z);

                    if (isInOpening) {
                        // 开口：清成空气（甲——只有前景单层 2×3 是空洞）
                        level.setBlock(placePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        // Place dungeon portal block
                        level.setBlock(placePos,
                                com.piranport.registry.ModBlocks.DUNGEON_PORTAL.get().defaultBlockState(), 3);
                    }
                }
            }
        }

        // Configure the BlockEntity at the center of the portal
        // The center block is at the middle of the interior opening
        int openingCenterX = (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH / 2;
        int openingCenterY = (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT / 2;
        int openingCenterZ = (FRAME_DEPTH - 1) / 2;

        BlockPos centerPos = cornerPos.relative(facing, openingCenterX)
                .relative(Direction.UP, openingCenterY)
                .relative(right, openingCenterZ);

        // Find the nearest dungeon portal block BlockEntity and configure it
        // The interior blocks don't have BlockEntities (they're air), so we need to
        // configure one of the surrounding frame blocks.
        // For simplicity, we'll configure the BlockEntity at the bottom-center of the frame.

        BlockPos bePos = cornerPos.relative(facing, (FRAME_WIDTH - PORTAL_WIDTH) / 2)
                .relative(right, (FRAME_DEPTH - 1) / 2);

        if (level.getBlockEntity(bePos) instanceof DungeonPortalBlockEntity portalBE) {
            portalBE.setDungeonData(instanceId, nodeId);
            portalBE.checkStructure(); // Re-check to mark as complete
            return centerPos;
        }

        // If the BE isn't at the expected position, search for it
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    boolean isInOpening = isInOpening(x, y, z);

                    if (!isInOpening) {
                        BlockPos checkPos = cornerPos.relative(facing, x)
                                .relative(Direction.UP, y)
                                .relative(right, z);

                        if (level.getBlockEntity(checkPos) instanceof DungeonPortalBlockEntity portalBE) {
                            portalBE.setDungeonData(instanceId, nodeId);
                            portalBE.checkStructure();
                            return centerPos;
                        }
                    }
                }
            }
        }

        PiranPort.LOGGER.warn("Failed to find DungeonPortalBlockEntity at portal structure {}", cornerPos);
        return centerPos;
    }
}
