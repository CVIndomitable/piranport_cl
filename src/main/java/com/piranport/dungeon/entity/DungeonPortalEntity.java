package com.piranport.dungeon.entity;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.NodeCaption;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Portal entity spawned when a dungeon node's flagship is defeated.
 * All dungeon players must enter the portal to advance.
 */
public class DungeonPortalEntity extends Entity {
    private static final EntityDataAccessor<Integer> SPIN_TICK =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.INT);
    /** 这扇门通往的节点；空串 = 旧存档/单门自动选路。 */
    private static final EntityDataAccessor<String> DATA_TARGET_NODE =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.STRING);
    /** 门所在的分歧来源节点；客户端用它查定格快照与条件文字。 */
    private static final EntityDataAccessor<String> DATA_SOURCE_NODE =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.STRING);
    /** 以下三项由服务端首次 tick 时从实例与关卡数据推出，客户端只读用于渲染标签。 */
    private static final EntityDataAccessor<String> DATA_STAGE_ID =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OBJECTIVE_KEY =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_INSTANCE_KEY =
            SynchedEntityData.defineId(DungeonPortalEntity.class, EntityDataSerializers.STRING);

    /** Auto-discard a portal that nobody touches in 10 minutes — prevents orphan portals
     *  if NBT was corrupted (instanceId == null) or instance was cleaned up early. */
    private static final int ORPHAN_DESPAWN_TICKS = 10 * 60 * 20;
    /** 走近门群的水平半径：相邻门间距为 14 格，半径 6 保证两扇门的触发区不重叠。 */
    private static final double APPROACH_RADIUS = 6.0;
    /** Defensive cap for persisted/observed portal entries. */
    private static final int MAX_ENTERED_PLAYERS = 64;
    private static final int MAX_NODE_ID_LENGTH = 128;

    private UUID instanceId;
    private String nodeId;
    private final Set<UUID> enteredPlayers = new HashSet<>();
    /** 当前仍在走近范围内的玩家；只在内存中，离开后清除，再次走近会重新触发。 */
    private final Set<UUID> approachedPlayers = new HashSet<>();
    private int orphanTicks = 0;

    public DungeonPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static DungeonPortalEntity create(ServerLevel level, UUID instanceId, String nodeId,
                                              double x, double y, double z) {
        return create(level, instanceId, nodeId, "", x, y, z);
    }

    /**
     * @param targetNodeId 这扇门通往的节点；空串表示旧存档的单门自动选路。
     */
    public static DungeonPortalEntity create(ServerLevel level, UUID instanceId, String nodeId,
                                              String targetNodeId, double x, double y, double z) {
        if (instanceId == null || nodeId == null || nodeId.isBlank()
                || nodeId.length() > MAX_NODE_ID_LENGTH
                || (targetNodeId != null && targetNodeId.length() > MAX_NODE_ID_LENGTH)) {
            PiranPort.LOGGER.warn("Rejected invalid dungeon portal creation (instance={}, nodeLength={}, targetLength={})",
                    instanceId, nodeId == null ? -1 : nodeId.length(),
                    targetNodeId == null ? -1 : targetNodeId.length());
            return null;
        }
        DungeonPortalEntity portal = new DungeonPortalEntity(
                ModEntityTypes.DUNGEON_PORTAL.get(), level);
        portal.instanceId = instanceId;
        portal.nodeId = nodeId;
        portal.setSourceNodeId(nodeId);
        portal.setTargetNodeId(targetNodeId == null ? "" : targetNodeId);
        portal.setPos(x, y, z);
        return portal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPIN_TICK, 0);
        builder.define(DATA_TARGET_NODE, "");
        builder.define(DATA_SOURCE_NODE, "");
        builder.define(DATA_STAGE_ID, "");
        builder.define(DATA_OBJECTIVE_KEY, "");
        builder.define(DATA_INSTANCE_KEY, "");
    }

    /** 这扇门通往的节点；空串 = 单门自动选路（旧存档/终局出口）。 */
    public String getTargetNodeId() {
        String target = entityData.get(DATA_TARGET_NODE);
        return target == null ? "" : target;
    }

    public void setTargetNodeId(String targetNodeId) {
        String safe = targetNodeId == null || targetNodeId.length() > MAX_NODE_ID_LENGTH
                ? "" : targetNodeId;
        entityData.set(DATA_TARGET_NODE, safe);
    }

    public String getSourceNodeId() { return entityData.get(DATA_SOURCE_NODE); }

    private void setSourceNodeId(String sourceNodeId) {
        entityData.set(DATA_SOURCE_NODE, sourceNodeId == null ? "" : sourceNodeId);
    }

    public String getStageId() { return entityData.get(DATA_STAGE_ID); }

    public String getObjectiveKey() { return entityData.get(DATA_OBJECTIVE_KEY); }

    public String getInstanceKey() { return entityData.get(DATA_INSTANCE_KEY); }

    /**
     * 把门的展示信息从实例推到同步数据：客户端没有实例，只能靠这里带过去。
     * 目标节点不存在时不写入，标签保持空白，避免显示成错误的节点名。
     */
    private void syncDoorInfo(DungeonInstance instance) {
        String target = getTargetNodeId();
        if (target.isEmpty()) return;
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData targetNode = stage == null ? null : stage.nodes().get(target);
        if (targetNode == null) return;
        entityData.set(DATA_STAGE_ID, instance.getStageId());
        entityData.set(DATA_OBJECTIVE_KEY, NodeCaption.objectiveKey(stage, targetNode));
        entityData.set(DATA_INSTANCE_KEY, instance.getInstanceId().toString());
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            // 视觉主体是方块门框；实体本身不可见（DungeonPortalRenderer 不再画门），
            // 只保留开口内的环境粒子。半径压在 0.9 内，避免粒子跑进实心框架块里白白被遮挡。
            if (tickCount % 4 == 0) {
                double angle = tickCount * 0.1;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        getX() + Math.cos(angle) * 0.9,
                        getY() - 0.5 + Math.sin(tickCount * 0.15) * 0.9,
                        getZ() + Math.sin(angle) * 0.9,
                        0, 0.02, 0);
            }
            return;
        }

        // Despawn orphan portals (broken NBT / cleaned-up instance) to avoid
        // leaving permanently-stuck entities in the dungeon dimension.
        if (instanceId == null || nodeId == null || nodeId.isBlank()) {
            PiranPort.LOGGER.warn("Discarding invalid dungeon portal {} (instance={}, nodeLength={})",
                    getId(), instanceId, nodeId == null ? -1 : nodeId.length());
            discard();
            return;
        }
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) level());
        if (mgr.getInstance(instanceId) == null) {
            discard();
            return;
        }

        // Server-side: check for nearby players every 10 ticks instead of every tick
        if (tickCount % 10 != 0) return;

        if (getStageId().isEmpty()) syncDoorInfo(mgr.getInstance(instanceId));

        AABB area = getBoundingBox().inflate(2.0, 1.0, 2.0);
        List<ServerPlayer> nearbyPlayers = ((ServerLevel) level())
                .getEntitiesOfClass(ServerPlayer.class, area);

        Set<UUID> members = mgr.getInstance(instanceId).getPlayerUuids();

        // 走近门群只触发一次（离开再进才重触发），用于分歧门的定格与下发；半径需小于相邻门的间距（14）。
        AABB approachArea = getBoundingBox().inflate(APPROACH_RADIUS, 3.0, APPROACH_RADIUS);
        Set<UUID> approaching = new HashSet<>();
        for (ServerPlayer player : ((ServerLevel) level()).getEntitiesOfClass(ServerPlayer.class, approachArea)) {
            UUID playerId = player.getUUID();
            if (!members.contains(playerId)) continue;
            approaching.add(playerId);
            if (approachedPlayers.add(playerId)) {
                com.piranport.dungeon.event.DungeonEventHandler.onPortalApproached(
                        player, mgr.getInstance(instanceId), nodeId);
            }
        }
        approachedPlayers.retainAll(approaching);

        for (ServerPlayer player : nearbyPlayers) {
            UUID playerId = player.getUUID();
            if (!members.contains(playerId)) continue;
            com.piranport.dungeon.event.DungeonEventHandler.onPortalEntered(
                    player, mgr.getInstance(instanceId), nodeId, getTargetNodeId());
            if (enteredPlayers.size() >= MAX_ENTERED_PLAYERS && !enteredPlayers.contains(playerId)) {
                continue;
            }
            enteredPlayers.add(playerId);
        }

        boolean memberNearby = false;
        for (UUID memberId : members) {
            if (enteredPlayers.contains(memberId)) {
                memberNearby = true;
                break;
            }
        }
        if (!memberNearby) {
            orphanTicks += 10;
            if (orphanTicks > ORPHAN_DESPAWN_TICKS) {
                discard();
                return;
            }
        } else {
            orphanTicks = 0;
        }

        // 传送门只处理当前触碰者的离开/推进，不等待实例中的其他历史成员。
        if (!enteredPlayers.isEmpty()) {
            DungeonInstance instance = mgr.getInstance(instanceId);
            if (instance != null) {
                // 出口保留给仍在战场中的玩家及之后的回访者，历史触碰记录无需保存。
                enteredPlayers.clear();
            }
        }
    }

    public UUID getInstanceId() { return instanceId; }
    public String getNodeId() { return nodeId; }
    public int getSpinTick() { return tickCount; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (instanceId != null) tag.putUUID("InstanceId", instanceId);
        if (nodeId != null) tag.putString("NodeId", nodeId);
        String target = getTargetNodeId();
        if (!target.isEmpty()) tag.putString("TargetNodeId", target);
        if (!enteredPlayers.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (UUID uuid : enteredPlayers) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                list.add(entry);
            }
            tag.put("EnteredPlayers", list);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("InstanceId")) instanceId = tag.getUUID("InstanceId");

        if (tag.contains("NodeId", net.minecraft.nbt.Tag.TAG_STRING)) {
            String loadedNodeId = tag.getString("NodeId");
            nodeId = (loadedNodeId.isBlank() || loadedNodeId.length() > MAX_NODE_ID_LENGTH)
                    ? null : loadedNodeId;
            setSourceNodeId(nodeId);
        }
        if (tag.contains("TargetNodeId", net.minecraft.nbt.Tag.TAG_STRING)) {
            String loadedTarget = tag.getString("TargetNodeId");
            setTargetNodeId(loadedTarget.isBlank() || loadedTarget.length() > MAX_NODE_ID_LENGTH
                    ? "" : loadedTarget);
        }

        if (tag.contains("EnteredPlayers")) {
            net.minecraft.nbt.ListTag list = tag.getList("EnteredPlayers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && enteredPlayers.size() < MAX_ENTERED_PLAYERS; i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("UUID")) {
                    enteredPlayers.add(entry.getUUID("UUID"));
                }
            }
        }
    }
}
