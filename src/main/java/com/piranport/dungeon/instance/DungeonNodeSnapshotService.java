package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.BossAntiStuckScheduler;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.escort.EscortRuntime;
import com.piranport.dungeon.event.DungeonBossBroadcast;
import com.piranport.dungeon.saved.DungeonNodeSnapshotData;
import com.piranport.dungeon.saved.DungeonObjectiveData;
import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 副本死亡快照：记录死亡节点仍存活的敌人，在玩家重返该节点时补刷。
 *
 * <p>《副本/12》失败与重试：玩家死亡不重置副本里的敌人、波次、宝箱和已有进度。
 * 现行实现死亡回书台后实例转为 SUSPENDED、区块卸载；重返时
 * {@code hasEnteredNode} 为 true 只传送不生成。如果原实体因任何原因不在了，
 * 节点就会卡在「还有敌人却打不到」的不可通关状态。这里以完整实体 NBT 做兜底：
 * 恢复沿用原 UUID，因此 {@link DungeonObjectiveData} 的存活登记、波次判定与
 * 旗舰判定继续成立。</p>
 *
 * <p><b>v1 只覆盖无脚本的 BATTLE / BOSS 节点。</b>脚本节点（boss_intro /
 * artillery_intro / ulan_activity / goldencatcat）自带阶段状态机，并在自己的
 * NBT 里按 UUID 重新解析实体；在状态机之外补刷会与脚本阶段冲突（重复生成或
 * 阶段错位）。护航运输船也排除：它由 {@link EscortRuntime} 按标签重新寻找，
 * 失败时整节点重置，不能当成普通敌人复活。</p>
 */
public final class DungeonNodeSnapshotService {
    private DungeonNodeSnapshotService() {}

    private static final String INSTANCE_TAG_PREFIX = "dungeon_instance_";
    private static final String NODE_TAG_PREFIX = "dungeon_node_";
    /** 等待节点区块实体就绪的最长 tick 数；超时放弃本次恢复，快照保留待下次入节点再试。 */
    private static final int MAX_READY_WAIT_TICKS = 100;
    /** 到达节点后先等几 tick 再查实体，给传送与区块实体加载留出时间。 */
    private static final int MIN_WAIT_TICKS = 2;

    private static final class Pending {
        final UUID instanceId;
        final String nodeId;
        int age;
        boolean chunksRequested;

        Pending(UUID instanceId, String nodeId) {
            this.instanceId = instanceId;
            this.nodeId = nodeId;
        }
    }

    /** 待恢复的节点；进程内状态，服务器重启后由下一次入节点重新调度。 */
    private static final Map<String, Pending> PENDING = new HashMap<>();

    private static String key(UUID instanceId, String nodeId) {
        return instanceId + "|" + nodeId;
    }

    // ===== 死亡抓拍 =====

    /**
     * 玩家在副本内死亡、传送回书台之前调用。
     *
     * <p>只扫当前已加载的实体：玩家刚在节点里战斗，节点附近的剩余敌人一定已加载；
     * 远处节点的敌人仍留在世界存档里，不需要快照。重复死亡/多人死亡时按
     * {@link DungeonObjectiveData} 的存活登记裁剪旧条目后再抓最新状态，
     * 因此不会把已经打死的敌人重新复活，也不会重复叠加同一 UUID。</p>
     */
    public static void captureOnPlayerDeath(ServerLevel level, DungeonInstance instance) {
        if (level == null || instance == null) return;
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;

        DungeonNodeSnapshotData snapshots = DungeonNodeSnapshotData.get(level);
        DungeonObjectiveData objectives = DungeonObjectiveData.get(level);
        UUID instanceId = instance.getInstanceId();
        String instanceTag = INSTANCE_TAG_PREFIX + instanceId;

        List<Entity> candidates = level.getEntitiesOfClass(Entity.class, usableBox(level, instance),
                e -> e.isAlive()
                        && !(e instanceof Player)
                        && e.getTags().contains(instanceTag)
                        && !e.getTags().contains(EscortRuntime.TRANSPORT_TAG));

        int captured = 0;
        java.util.Set<String> touchedNodes = new java.util.HashSet<>();
        for (Entity entity : candidates) {
            String nodeId = nodeTagOf(entity);
            if (nodeId == null) continue;
            NodeData node = stage.nodes().get(nodeId);
            if (!isSnapshotNode(node, instance, objectives)) continue;
            CompoundTag nbt = new CompoundTag();
            if (!entity.save(nbt)) continue;
            snapshots.put(instanceId, nodeId, entity.getUUID(), nbt);
            touchedNodes.add(nodeId);
            captured++;
        }

        // 旧快照里已经阵亡的条目要剔除：死亡到死亡之间被击杀的敌人不能再补刷。
        // 这里按 objectives 的存活登记判断，不依赖区块是否加载。
        for (String nodeId : snapshots.nodesOf(instanceId)) {
            snapshots.retain(instanceId, nodeId,
                    uuid -> objectives.isTrackedAlive(instanceId, nodeId, uuid));
        }

        if (captured > 0) {
            PiranPort.LOGGER.info("[副本] 实例 {} 死亡快照：节点 {} 共记录 {} 个存活敌人",
                    instanceId, touchedNodes, captured);
        }
    }

    // ===== 重返恢复 =====

    /**
     * 玩家被传送到某个节点后调用。节点未通关且存在快照时登记一次恢复，
     * 实际补刷在 {@link #tick(ServerLevel)} 里等区块实体加载完成后执行。
     */
    public static void scheduleRestore(ServerLevel level, DungeonInstance instance, String nodeId) {
        if (level == null || instance == null || nodeId == null || nodeId.isBlank()) return;
        if (!instance.hasEnteredNode(nodeId) || instance.getClearedNodes().contains(nodeId)) return;
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        if (node == null || !isSnapshotNode(node, instance, null)) return;
        if (!DungeonNodeSnapshotData.get(level).hasNode(instance.getInstanceId(), nodeId)) return;
        PENDING.putIfAbsent(key(instance.getInstanceId(), nodeId),
                new Pending(instance.getInstanceId(), nodeId));
    }

    /** 服务端每 tick 调度：区块实体就绪后才检查/补刷，避免把「未加载」误判成「不存在」而重复生成。 */
    public static void tick(ServerLevel level) {
        if (PENDING.isEmpty()) return;
        DungeonInstanceManager mgr = DungeonInstanceManager.get(level);
        DungeonNodeSnapshotData snapshots = DungeonNodeSnapshotData.get(level);
        DungeonObjectiveData objectives = DungeonObjectiveData.get(level);

        Iterator<Map.Entry<String, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Pending pending = it.next().getValue();
            DungeonInstance instance = mgr.getInstance(pending.instanceId);
            if (instance == null || instance.getClearedNodes().contains(pending.nodeId)
                    || !instance.hasEnteredNode(pending.nodeId)) {
                it.remove();
                continue;
            }

            Map<UUID, CompoundTag> entries = snapshots.nodeEntries(pending.instanceId, pending.nodeId);
            if (entries.isEmpty()) {
                it.remove();
                continue;
            }

            pending.age++;
            if (pending.age < MIN_WAIT_TICKS) continue;

            if (!pending.chunksRequested) {
                requestSnapshotChunks(level, entries);
                pending.chunksRequested = true;
                continue;
            }
            if (!entitiesReady(level, entries)) {
                if (pending.age < MAX_READY_WAIT_TICKS) continue;
                // 超时仍取不到实体数据：放弃这次恢复，保留快照等下次入节点。
                // 此时强行补刷有和原实体重复 UUID 的风险，宁可留到下次。
                PiranPort.LOGGER.warn("[副本] 实例 {} 节点 {} 死亡快照恢复超时（区块实体未就绪），保留快照等待下次进入",
                        pending.instanceId, pending.nodeId);
                it.remove();
                continue;
            }

            int restored = restoreEntries(level, instance, pending.nodeId, entries, snapshots, objectives);
            snapshots.clearNode(pending.instanceId, pending.nodeId);
            it.remove();
            if (restored > 0) {
                PiranPort.LOGGER.info("[副本] 实例 {} 节点 {} 按死亡快照补刷 {} 个敌人",
                        pending.instanceId, pending.nodeId, restored);
            }
        }
    }

    // ===== 清理入口 =====

    /** 节点通关后清掉该节点快照，避免重复补刷。 */
    public static void clearNode(ServerLevel level, UUID instanceId, String nodeId) {
        if (level == null || instanceId == null || nodeId == null) return;
        DungeonNodeSnapshotData.get(level).clearNode(instanceId, nodeId);
        PENDING.remove(key(instanceId, nodeId));
    }

    /** 实例通关/清理/从头开始时丢弃全部快照。 */
    public static void clearInstance(ServerLevel level, UUID instanceId) {
        if (level == null || instanceId == null) return;
        DungeonNodeSnapshotData.get(level).clearInstance(instanceId);
        PENDING.keySet().removeIf(k -> k.startsWith(instanceId + "|"));
    }

    private static int restoreEntries(ServerLevel level, DungeonInstance instance, String nodeId,
                                      Map<UUID, CompoundTag> entries,
                                      DungeonNodeSnapshotData snapshots,
                                      DungeonObjectiveData objectives) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        int restored = 0;

        for (Map.Entry<UUID, CompoundTag> entry : new ArrayList<>(entries.entrySet())) {
            UUID entityId = entry.getKey();
            // 实体仍在世界（可能只是刚加载回来）→ 不需要补刷。
            Entity existing = level.getEntity(entityId);
            if (existing != null) {
                snapshots.remove(instance.getInstanceId(), nodeId, entityId);
                continue;
            }
            // 玩家离开期间已被其他人击杀 → 不能复活。
            if (!objectives.isTrackedAlive(instance.getInstanceId(), nodeId, entityId)) {
                snapshots.remove(instance.getInstanceId(), nodeId, entityId);
                continue;
            }

            Entity created;
            try {
                created = EntityType.create(entry.getValue(), level).orElse(null);
            } catch (Exception ex) {
                // 损坏的实体 NBT 不能让服务端 tick 崩掉；丢掉这一条即可。
                PiranPort.LOGGER.warn("[副本] 死亡快照实体 NBT 解析失败，丢弃条目：{}",
                        entry.getValue().getString("id"), ex);
                snapshots.remove(instance.getInstanceId(), nodeId, entityId);
                continue;
            }
            if (created == null) {
                PiranPort.LOGGER.warn("[副本] 死亡快照实体类型无法重建，丢弃条目：{}",
                        entry.getValue().getString("id"));
                snapshots.remove(instance.getInstanceId(), nodeId, entityId);
                continue;
            }
            // 补回编队归属：实例冻结期间 FleetGroupManager.cleanup 可能已经把
            // 「未加载」的成员当成阵亡清空，这里按实体 NBT 里的 FleetGroupId 重建。
            ensureFleetGroup(level, created);
            if (!level.addFreshEntity(created)) {
                PiranPort.LOGGER.warn("[副本] 死亡快照实体无法加入世界，丢弃条目：{}", entityId);
                snapshots.remove(instance.getInstanceId(), nodeId, entityId);
                continue;
            }
            onRestored(level, instance, node, created);
            snapshots.remove(instance.getInstanceId(), nodeId, entityId);
            restored++;
        }
        return restored;
    }

    /**
     * 只对普通战斗节点做快照。脚本节点与护航运输船的生命周期由各自运行时管理。
     *
     * @param objectives 可为 null；死亡抓拍时要求节点已有敌人登记，
     *                   以防把未生成过敌人的空节点也写进快照。
     */
    private static boolean isSnapshotNode(NodeData node, DungeonInstance instance,
                                          DungeonObjectiveData objectives) {
        if (node == null) return false;
        if (node.type() != NodeData.NodeType.BATTLE && node.type() != NodeData.NodeType.BOSS) return false;
        if (node.script() != null && !node.script().isBlank()) return false;
        if (instance.getClearedNodes().contains(node.nodeId())) return false;
        if (objectives != null
                && !objectives.hasRegistration(instance.getInstanceId(), node.nodeId())) return false;
        return true;
    }

    private static String nodeTagOf(Entity entity) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith(NODE_TAG_PREFIX) && tag.length() > NODE_TAG_PREFIX.length()) {
                return tag.substring(NODE_TAG_PREFIX.length());
            }
        }
        return null;
    }

    /** 先把快照坐标所在区块拉起来，否则 getEntity 会把「区块未加载」误判成实体不存在。 */
    private static void requestSnapshotChunks(ServerLevel level, Map<UUID, CompoundTag> entries) {
        java.util.Set<Long> requested = new java.util.HashSet<>();
        for (CompoundTag nbt : entries.values()) {
            BlockPos pos = readPos(nbt);
            if (pos == null) continue;
            long chunk = ChunkPos.asLong(pos);
            if (requested.add(chunk)) {
                level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            }
        }
    }

    private static boolean entitiesReady(ServerLevel level, Map<UUID, CompoundTag> entries) {
        for (CompoundTag nbt : entries.values()) {
            BlockPos pos = readPos(nbt);
            if (pos == null) continue;
            if (!level.areEntitiesLoaded(ChunkPos.asLong(pos))) return false;
        }
        return true;
    }

    private static BlockPos readPos(CompoundTag nbt) {
        ListTag pos = nbt.getList("Pos", Tag.TAG_DOUBLE);
        if (pos.size() < 3) return null;
        return new BlockPos((int) Math.floor(pos.getDouble(0)),
                (int) Math.floor(pos.getDouble(1)),
                (int) Math.floor(pos.getDouble(2)));
    }

    private static void ensureFleetGroup(ServerLevel level, Entity entity) {
        if (!(entity instanceof AbstractDeepOceanEntity abyssal)) return;
        UUID groupId = abyssal.getFleetGroupId();
        if (groupId == null) return;
        FleetGroupManager mgr = FleetGroupManager.get(level);
        FleetGroup group = mgr.getGroup(groupId);
        if (group == null) {
            group = mgr.createGroup(groupId);
            group.setFormation(FleetGroup.FormationType.SINGLE_LINE);
        }
        if (entity instanceof DeepOceanBossEntity) {
            // Boss 是独立领队，不进入护卫成员表（与 NodeBattleField.spawnFleet 一致）。
            group.setLeaderUuid(entity.getUUID());
            mgr.setDirty();
        } else if (!group.getMembers().contains(entity.getUUID())
                && !entity.getUUID().equals(group.getLeaderUuid())) {
            mgr.addMember(groupId, entity.getUUID());
        }
    }

    /** 补回 Boss 铭牌与防卡调度：两者都是纯内存状态，实例冻结后需要重新登记。 */
    private static void onRestored(ServerLevel level, DungeonInstance instance, NodeData node, Entity entity) {
        if (node == null || node.type() != NodeData.NodeType.BOSS) return;
        if (!entity.getTags().contains("dungeon_flagship")) return;
        BossAntiStuckScheduler.register(instance, node, List.of(entity));
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        DungeonBossBroadcast.trackBoss(instance, node, List.of(entity), entityId);
    }

    private static AABB usableBox(ServerLevel level, DungeonInstance instance) {
        return new AABB(instance.getUsableMinX(), level.getMinBuildHeight(), instance.getUsableMinZ(),
                instance.getUsableMaxX() + 1, level.getMaxBuildHeight(), instance.getUsableMaxZ() + 1);
    }
}
