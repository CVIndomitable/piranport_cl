package com.piranport.dungeon.block;

import com.piranport.dungeon.block.entity.DungeonPortalBlockEntity;
import com.piranport.dungeon.data.BranchRule;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
        // 用开口的两端块反推包围盒：facing 可能是负轴，手写 min/max 容易把区间写反，
        // 且旧的 "min + PORTAL_WIDTH" 会多圈进一列框架（宽度实际变成 3）。
        BlockPos first = origin.relative(facing, openingMinX()).above(openingMinY());
        BlockPos last = origin.relative(facing, openingMinX() + PORTAL_WIDTH - 1)
                .above(openingMinY() + PORTAL_HEIGHT - 1);
        return new AABB(
                Math.min(first.getX(), last.getX()), first.getY(), Math.min(first.getZ(), last.getZ()),
                Math.max(first.getX(), last.getX()) + 1, first.getY() + PORTAL_HEIGHT,
                Math.max(first.getZ(), last.getZ()) + 1);
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

        // 开口只有前景一层（lz==0）。旧实现还沿 right 取深度中点，ec42b859 把
        // "2×3×4 隧道"改成"前景单层 2×3"后，那个中点已经落在实心框架块里。
        int openingCenterX = (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH / 2;
        int openingCenterY = (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT / 2;

        return origin.relative(facing, openingCenterX)
                .relative(Direction.UP, openingCenterY);
    }

    /**
     * Gets the AABB of the portal interior (for collision/entity detection).
     */
    public static AABB getPortalInteriorAABB(BlockPos cornerPos, Direction facing) {
        if (cornerPos == null) {
            return null;
        }
        // 开口只有前景单层，内部包围盒与进门扫描盒定义一致。
        return getOpeningAABB(cornerPos, facing);
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
                .above(openingMinY() + PORTAL_HEIGHT / 2);
    }

    /**
     * Builds the dungeon exit portal at the given anchor.
     *
     * <p><b>一片式出口（《副本/26》2026-10-22 补充）：</b>不再摆 3×3 平台、海晶灯光柱和
     * 4×5×4 的 {@code dungeon_portal} 门框，只在水面上放一具 {@code DungeonPortalEntity}，
     * 由 {@code DungeonPortalRenderer} 画一片门面。门没有碰撞体积、没有门框、没有地基，
     * 玩家直接穿过去触发传送。节点奖励仍由 {@code settleNode} 直接发进玩家背包。</p>
     *
     * <p>坐标约定：{@code cornerPos} 是历史锚点（多数传 {@code anchor.below() 或节点出生点}），
     * 本方法只把它当"出口大致方位"用——实际落点由 {@link #findWaterTopY} 在该列上
     * 扫出水面高度后决定，因此锚点的 y 不再重要。</p>
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

        // 门沿 Z 轴横排，间距 14 格：一片式门面只有 2 格宽，14 格保证门头标签互不重叠，
        // 也不会让相邻两扇门的走进判定区（半径 2）重叠。
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

    /**
     * 该节点应生成哪些门。分支节点：每个分歧目标（含 default）一扇门，已清目标保留为暗门；
     * 普通节点：取全部未清直连后继。门的亮灭由 {@code BranchDoors} 按玩家决定，这里只管门的数量与位置。
     */
    private static List<String> doorTargets(DungeonInstance instance, String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        if (node == null) return List.of();
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        if (node.hasBranches()) {
            for (BranchRule rule : node.branches()) {
                if (stage.nodes().containsKey(rule.to())) targets.add(rule.to());
            }
            if (node.branchDefault() != null && stage.nodes().containsKey(node.branchDefault())) {
                targets.add(node.branchDefault());
            }
            return List.copyOf(targets);
        }
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
        // 一片式出口没有 4×5×4 的占地约束，四个朝向只用来挑一个"门面没插进地形"的落点。
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos center = tryBuildExitSite(level, cornerPos, facing, instanceId, nodeId, targetNodeId);
            if (center != null) {
                return center;
            }
        }
        return tryBuildExitSite(level, cornerPos, Direction.NORTH, instanceId, nodeId, targetNodeId);
    }

    /**
     * 在 {@code facing} 朝向下把一片式出口门面放到水面上。
     *
     * <p><b>为什么不再摆门框/平台/光柱：</b>《副本/26》2026-10-22 补充把出口改成一片漂在
     * 水面上的门面——没有碰撞体积、没有门框、没有地基，玩家直接穿过去。方块门框
     * {@code dungeon_portal} 只为兼容旧存档保留注册，出口路径不再摆放；世界野生废传送门
     * 的 {@link #placeFrameGeometry} 不动。</p>
     *
     * <p>朝向仍然参与计算：多扇门沿 {@code facing} 的法线错开 2 格，门头标签不会互相叠；
     * 门面正下方若埋进实心地形（不是水也不是空气），说明这个朝向选错了，返回 null 换下一个。</p>
     *
     * @return 门面脚下的坐标；放不下则返回 null
     */
    private static BlockPos tryBuildExitSite(ServerLevel level, BlockPos cornerPos,
                                             Direction facing, UUID instanceId, String nodeId,
                                             String targetNodeId) {
        Direction front = facing.getCounterClockWise();
        BlockPos anchor = cornerPos.relative(facing, PORTAL_WIDTH / 2).relative(front, 2);

        int waterTopY = findWaterTopY(level, anchor);
        if (waterTopY < 0) {
            // 这一列没有水（出生点的 5×5 橡木板平台、或岸上）：退回到海平面，
            // 但要求落脚点不是实心方块，否则门会插进岸壁/山体，换下一个朝向。
            BlockPos footing = new BlockPos(anchor.getX(),
                    com.piranport.dungeon.DungeonConstants.SPAWN_Y - 1, anchor.getZ());
            BlockState footingState = level.getBlockState(footing);
            if (!footingState.isAir() && !footingState.canBeReplaced()) {
                return null;
            }
            waterTopY = com.piranport.dungeon.DungeonConstants.SPAWN_Y - 1;
        }
        // 水面高度 = 最上面那格水块的顶面（水在 y=63 时就是 64.0）。
        double surfaceY = waterTopY + 1.0;

        BlockPos center = new BlockPos(anchor.getX(), Mth.floor(surfaceY), anchor.getZ());
        // x/z 用方块中心，门面才真正居中；y 用水面高度（水柱顶面），下沿正好压在水上。
        var portal = com.piranport.dungeon.entity.DungeonPortalEntity.create(level, instanceId, nodeId,
                targetNodeId, center.getX() + 0.5, surfaceY, center.getZ() + 0.5);
        if (portal == null) return null;
        level.addFreshEntity(portal);
        return center;
    }

    /**
     * 找出 {@code anchor} 那一列最上面那格水块的 y；那一列没有水时返回 -1。
     *
     * <p>副本地形把水写在 {@link com.piranport.dungeon.DungeonConstants#SEA_LEVEL} 上下两层，
     * 但节点出生点那一列可能压着 5×5 橡木板平台，那一列扫不到水。扫描范围放到
     * SEA_LEVEL 上下各十几格，扫不到就返回 -1，由调用方回退到海平面。</p>
     */
    private static int findWaterTopY(ServerLevel level, BlockPos anchor) {
        int top = com.piranport.dungeon.DungeonConstants.SEA_LEVEL + 12;
        int bottom = com.piranport.dungeon.DungeonConstants.SEA_LEVEL - 16;
        for (int y = top; y >= bottom; y--) {
            BlockPos probe = new BlockPos(anchor.getX(), y, anchor.getZ());
            if (level.getBlockState(probe).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                return y;
            }
        }
        return -1;
    }

}
