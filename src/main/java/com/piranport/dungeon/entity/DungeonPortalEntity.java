package com.piranport.dungeon.entity;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.NodeCaption;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.network.PortalTransitPayload;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    /**
     * 走进门面到真正传送之间的演出时长：20 tick = 1 秒（《副本/26》2026-10-22 补充）。
     * 与客户端 {@code ClientPortalTransit.TRANSIT_TICKS} 必须一致——两边不同步时
     * 会出现"演出放完但还没传"或"传送了但镜头还在晃"。
     */
    private static final int TRANSIT_TICKS = 20;

    private UUID instanceId;
    private String nodeId;
    private final Set<UUID> enteredPlayers = new HashSet<>();
    /** 当前仍在走近范围内的玩家；只在内存中，离开后清除，再次走近会重新触发。 */
    private final Set<UUID> approachedPlayers = new HashSet<>();
    private int orphanTicks = 0;
    /** 玩家 → 剩余倒数 tick。只在服务端内存里：重登/重启即取消，不会把半截演出带进存档。 */
    private final Map<UUID, Integer> pendingTransit = new HashMap<>();
    /** 倒数走完却没被传走的玩家 → 剩余重试冷却，避免"留在门内等待"时每秒重播演出。 */
    private final Map<UUID, Integer> transitCooldown = new HashMap<>();

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
            // 视觉主体是 DungeonPortalRenderer 画的一片门面，实体本身不可见，只补环境粒子。
            // 粒子从水面一路飘到门顶（3 格高）、半径压在 1.0 内，贴着 2 格宽的门面走。
            if (tickCount % 4 == 0) {
                double angle = tickCount * 0.1;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        getX() + Math.cos(angle) * 1.0,
                        getY() + random.nextDouble() * 3.0,
                        getZ() + Math.sin(angle) * 1.0,
                        0, 0.02, 0);
            }
            return;
        }

        // 无效 NBT / 实例已清理的门直接销毁，避免副本维度里留下永久卡死的实体。
        if (instanceId == null || nodeId == null || nodeId.isBlank()) {
            PiranPort.LOGGER.warn("Discarding invalid dungeon portal {} (instance={}, nodeLength={})",
                    getId(), instanceId, nodeId == null ? -1 : nodeId.length());
            discard();
            return;
        }
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) level());
        DungeonInstance instance = mgr.getInstance(instanceId);
        if (instance == null) {
            discard();
            return;
        }

        // 走近判定只需 10 tick 一次；但倒数必须每 tick 走，否则 1 秒的演出会被采样成 0.5~1.5 秒。
        if (tickCount % 10 == 0) {
            if (getStageId().isEmpty()) syncDoorInfo(instance);
            scanApproach(instance);
        }

        Set<UUID> touching = tickTransit(instance);
        enteredPlayers.clear();
        enteredPlayers.addAll(touching);
        tickOrphanDespawn(instance, touching);
    }

    /**
     * 玩家走近门群只触发一次（离开再进才重触发），用于分歧门的定格与下发。
     * 半径需小于相邻门的间距（14）。
     */
    private void scanApproach(DungeonInstance instance) {
        ServerLevel serverLevel = (ServerLevel) level();
        Set<UUID> members = instance.getPlayerUuids();
        AABB approachArea = getBoundingBox().inflate(APPROACH_RADIUS, 3.0, APPROACH_RADIUS);
        Set<UUID> approaching = new HashSet<>();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, approachArea)) {
            UUID playerId = player.getUUID();
            if (!members.contains(playerId)) continue;
            approaching.add(playerId);
            if (approachedPlayers.add(playerId)) {
                com.piranport.dungeon.event.DungeonEventHandler.onPortalApproached(
                        player, instance, nodeId);
            }
        }
        approachedPlayers.retainAll(approaching);
    }

    /**
     * 触碰判定 + 传送倒数，返回本 tick 站在门内的成员集合。
     *
     * <p><b>为什么是"先演出、后传送"：</b>《副本/26》2026-10-22 补充要求走进门面后
     * 先播约一秒的下界门式演出再传送。所以服务端自己倒数 {@link #TRANSIT_TICKS}，
     * 倒数结束才交给 {@code DungeonProgressionRouter} 选路/传送；倒数期间走出门范围
     * 就取消并下发关闭包。</p>
     */
    private Set<UUID> tickTransit(DungeonInstance instance) {
        ServerLevel serverLevel = (ServerLevel) level();
        Set<UUID> members = instance.getPlayerUuids();
        List<ServerPlayer> nearby = serverLevel.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(2.0, 1.0, 2.0));

        Set<UUID> touching = new HashSet<>();
        for (ServerPlayer player : nearby) {
            if (members.contains(player.getUUID())) touching.add(player.getUUID());
        }

        // 走出门面：取消演出，不传送。关闭包由服务端主动发，客户端不自己超时。
        pendingTransit.entrySet().removeIf(entry -> {
            if (touching.contains(entry.getKey())) return false;
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) PortalTransitPayload.sendStop(player);
            return true;
        });

        transitCooldown.replaceAll((id, left) -> left - 1);
        transitCooldown.values().removeIf(left -> left <= 0);

        for (ServerPlayer player : nearby) {
            UUID playerId = player.getUUID();
            if (!members.contains(playerId)) continue;

            Integer left = pendingTransit.get(playerId);
            if (left == null) {
                // 倒数刚走完还没被传走（地形未就绪/条件不满足）时，等冷却再重来，
                // 否则"留在门内等待"会每秒重播一次演出。
                if (transitCooldown.containsKey(playerId)) continue;
                pendingTransit.put(playerId, TRANSIT_TICKS);
                PortalTransitPayload.sendStart(player);
                continue;
            }

            int next = left - 1;
            if (next > 0) {
                pendingTransit.put(playerId, next);
                continue;
            }

            pendingTransit.remove(playerId);
            PortalTransitPayload.sendStop(player);
            com.piranport.dungeon.event.DungeonEventHandler.onPortalEntered(
                    player, instance, nodeId, getTargetNodeId());
            transitCooldown.put(playerId, TRANSIT_TICKS);
        }
        return touching;
    }

    /** 没人碰门超过 {@link #ORPHAN_DESPAWN_TICKS} 就销毁，避免实例被清理后留下孤儿门。 */
    private void tickOrphanDespawn(DungeonInstance instance, Set<UUID> touching) {
        if (tickCount % 10 != 0) return;
        // 用"本 tick 有人碰"而不是历史记录判断：出口要一直留给仍在战场里的玩家和回访者。
        if (!touching.isEmpty()) {
            orphanTicks = 0;
            return;
        }
        orphanTicks += 10;
        if (orphanTicks > ORPHAN_DESPAWN_TICKS) {
            discard();
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
