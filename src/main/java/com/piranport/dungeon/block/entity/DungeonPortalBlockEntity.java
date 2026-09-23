package com.piranport.dungeon.block.entity;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.DungeonLecternBlock;
import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.block.PortalStructureHelper;
import com.piranport.dungeon.event.DungeonEntryService;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity for the dungeon portal block.
 * Tracks portal structure state and handles player teleportation when structure is complete.
 */
public class DungeonPortalBlockEntity extends BlockEntity {
    private static final VoxelShape PORTAL_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    // 框架尺寸常量已上移到 PortalStructureHelper（几何判定的唯一来源），此处不再重复定义。

    // Portal state
    private boolean isComplete = false;
    private boolean isActive = false;
    /** 框架方位缓存：由 checkStructure 解析，供开口 AABB 与讲台查找复用。 */
    private PortalStructureHelper.OrientedFrame frame = null;
    /** 是否已挂载持钥匙的讲台（门控条件之一）。 */
    private boolean hasKeyedLectern = false;
    /** 开口空间盒缓存，用于 ticker 扫描“玩家是否站进门里”。 */
    private AABB openingAABB = null;
    private UUID instanceId = null;
    private String nodeId = null;
    private final Set<UUID> enteredPlayers = new HashSet<>();

    public DungeonPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DUNGEON_PORTAL.get(), pos, state);
    }

    /**
     * Checks if the portal frame structure is complete.
     * Should be called when neighboring blocks change.
     *
     * <p>几何判定统一委托给 {@link PortalStructureHelper#findFrame}——本类不再自己遍历 4×5×4。
     * 旧实现把自身当角点，于是只有恰好落在角点的 BE 才能判定成立；findFrame 会从任意框架块
     * 反推角点，任何一块上的 BE 都能得到正确结果。</p>
     */
    public void checkStructure() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean previouslyComplete = isComplete;
        frame = PortalStructureHelper.findFrame(level, getBlockPos());
        isComplete = frame != null;
        // 回起点式门控：框架成立 + 有挂载的持钥匙讲台才算可用。
        hasKeyedLectern = isComplete && findKeyedLectern() != null;
        isActive = isComplete && hasKeyedLectern && instanceId != null && nodeId != null;
        openingAABB = isComplete ? PortalStructureHelper.getOpeningAABB(frame.origin(), frame.facing()) : null;

        // If structure just became complete, notify nearby players
        if (isComplete && !previouslyComplete) {
            PiranPort.LOGGER.debug("Dungeon portal structure completed at {}", getBlockPos());
        }
    }

    /**
     * 找出挂在框架底边、且插着钥匙的讲台。
     *
     * <p>“贴底边”的定义：讲台与框架底边（局部 ly==0 那一整层，因开口从 ly=1 起，该层恒为框架块）
     * 的某一格正交相邻，且讲台本身不在框架体积内。等价于讲台紧贴门框底部外圈——门前、门后、
     * 左右两侧或正下方均可，但必须单块且插着钥匙。</p>
     *
     * <p>持钥匙是硬条件：讲台是钥匙的权威来源，门本身不存钥匙。《副本/17》§四
     * “钥匙插在讲台/传送门结构上、玩家不携带进副本”。</p>
     *
     * @return 讲台坐标，若没有符合条件的讲台则返回 null
     */
    @Nullable
    public BlockPos findKeyedLectern() {
        if (level == null || frame == null) return null;
        Direction facing = frame.facing();
        Direction right = facing.getClockWise();
        BlockPos origin = frame.origin();

        for (int lx = 0; lx < PortalStructureHelper.FRAME_WIDTH; lx++) {
            for (int lz = 0; lz < PortalStructureHelper.FRAME_DEPTH; lz++) {
                // 底边层：ly == 0
                BlockPos sill = origin.relative(facing, lx).relative(right, lz);
                for (Direction side : Direction.values()) {
                    BlockPos candidate = sill.relative(side);
                    // 讲台必须落在框架体积之外（不能插在门框内部）
                    if (isInsideFrame(candidate, origin, facing, right)) continue;
                    if (!(level.getBlockState(candidate).getBlock() instanceof DungeonLecternBlock)) continue;
                    if (level.getBlockEntity(candidate) instanceof DungeonLecternBlockEntity lectern
                            && lectern.hasKey()) {
                        return candidate.immutable();
                    }
                }
            }
        }
        return null;
    }

    /** 判断某坐标是否落在框架 4×5×4 体积内。 */
    private static boolean isInsideFrame(BlockPos pos, BlockPos origin, Direction facing, Direction right) {
        for (int lx = 0; lx < PortalStructureHelper.FRAME_WIDTH; lx++) {
            for (int ly = 0; ly < PortalStructureHelper.FRAME_HEIGHT; ly++) {
                for (int lz = 0; lz < PortalStructureHelper.FRAME_DEPTH; lz++) {
                    if (origin.relative(facing, lx).above(ly).relative(right, lz).equals(pos)) return true;
                }
            }
        }
        return false;
    }

    /**
     * 玩家踏入开口时走这里进副本。
     *
     * <p>两个要点：
     * <ul>
     *   <li>必须是玩家，且只认服务端。开口是空气，本方法由 ticker 的 AABB 扫描驱动
     *       （空气没有方块承载 entityInside），方块侧的 entityInside 只是冗余触发。</li>
     *   <li>进门走 {@link DungeonEntryService#enter}——那是全项目唯一的权威入口，
     *       负责首次进入（instance == null → createInstance）与节点推进。绝不能另起一套
     *       近似逻辑，否则首次进入永远建不出实例。</li>
     * </ul>
     *
     * <p>刻意不走 {@link DungeonEventHandler#onPortalEntered}：那是副本内“已清节点出口”
     * 的离开逻辑，只会把玩家送回讲台，无法开局。</p>
     */
    public void entityInside(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        // 站在门里每 tick 都会触发，用 enteredPlayers 做去重，避免反复传送。
        if (!enteredPlayers.add(player.getUUID())) {
            return;
        }
        tryEnter(player);
    }

    /**
     * 解析挂载讲台并交给权威入口。失败时给出可见反馈，不静默。
     */
    private void tryEnter(ServerPlayer player) {
        if (frame == null) {
            player.displayClientMessage(
                    Component.translatable("block.piranport.dungeon_portal.incomplete_structure"), true);
            return;
        }
        BlockPos lecternPos = findKeyedLectern();
        if (lecternPos == null) {
            // 门框立起来了但没挂持钥匙的讲台——这是最容易被当成 bug 的状态，必须提示。
            player.displayClientMessage(
                    Component.translatable("block.piranport.dungeon_portal.waiting_for_activation"), true);
            return;
        }
        // 权威入口：首次进入 / 续关 / 节点推进都在这里分流。
        DungeonEntryService.enter(player, lecternPos, false, null, DungeonEntryService.Mode.ADVANCE);
    }

    /** 玩家离开开口后清掉去重标记，使其可以再次进门。 */
    public void forgetPlayer(UUID playerId) {
        enteredPlayers.remove(playerId);
    }

    /**
     * Sets the dungeon instance and node ID for this portal.
     */
    public void setDungeonData(UUID instanceId, String nodeId) {
        this.instanceId = instanceId;
        this.nodeId = nodeId;
        this.isActive = isComplete && hasKeyedLectern && instanceId != null && nodeId != null;
    }

    /**
     * Gets the dungeon instance ID.
     */
    public UUID getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the node ID.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Checks if the portal structure is complete.
     */
    public boolean isStructureComplete() {
        return isComplete;
    }

    /**
     * Checks if the portal is active (complete and has data).
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Server tick - updates portal state and checks for player teleportation.
     *
     * <p>开口是空气，没有方块能承载 {@code entityInside}，所以“走进门”必须靠这里主动扫描
     * 开口 AABB。每 tick 扫描；每 100 tick 复检一次结构（讲台插拔钥匙会触发 neighborChanged，
     * 但区块加载等路径不会，需要兜底）。</p>
     */
    public static void tick(Level level, BlockPos pos, BlockState state, DungeonPortalBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        // Periodic structure check
        if (be.tickCounter % 100 == 0) {
            be.checkStructure();
        }
        if (be.openingAABB != null) {
            be.scanOpening(serverLevel);
        }
        be.tickCounter++;
    }

    /**
     * 扫描开口内的玩家并触发进门；离开开口的玩家清掉去重标记，使其可再次进入。
     */
    private void scanOpening(ServerLevel serverLevel) {
        Set<UUID> inside = new HashSet<>();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, openingAABB)) {
            inside.add(player.getUUID());
            if (enteredPlayers.add(player.getUUID())) {
                tryEnter(player);
            }
        }
        // 离开开口后允许再次进门（例如放弃后又回来）。
        enteredPlayers.removeIf(id -> !inside.contains(id));
    }

    private int tickCounter = 0;

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (instanceId != null) {
            tag.putUUID("InstanceId", instanceId);
        }
        if (nodeId != null) {
            tag.putString("NodeId", nodeId);
        }
        tag.putBoolean("IsComplete", isComplete);
        tag.putBoolean("IsActive", isActive);

        // Save entered players (limit to prevent excessive save size)
        if (!enteredPlayers.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            int count = 0;
            for (UUID uuid : enteredPlayers) {
                if (count >= 100) break; // Limit saved players
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                list.add(entry);
                count++;
            }
            tag.put("EnteredPlayers", list);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("InstanceId")) {
            instanceId = tag.getUUID("InstanceId");
        }
        if (tag.contains("NodeId", net.minecraft.nbt.Tag.TAG_STRING)) {
            nodeId = tag.getString("NodeId");
        }
        isComplete = tag.getBoolean("IsComplete");
        isActive = tag.getBoolean("IsActive");

        if (tag.contains("EnteredPlayers")) {
            net.minecraft.nbt.ListTag list = tag.getList("EnteredPlayers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("UUID")) {
                    enteredPlayers.add(entry.getUUID("UUID"));
                }
            }
        }
    }
}
