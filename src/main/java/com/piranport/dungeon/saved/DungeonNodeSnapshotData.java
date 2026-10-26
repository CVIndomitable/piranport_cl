package com.piranport.dungeon.saved;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家在副本内死亡时的「战斗节点存活敌人快照」。
 *
 * <p>《副本/12》失败与重试：玩家死亡不重置副本里的敌人、波次、宝箱和进度。
 * 但玩家被送回书台后，节点区块会随无人在场而卸载；下次重返时
 * {@code DungeonNodeRouter.enterNode} 只传送、不再生成敌人（{@code enteredNodes} 已登记）。
 * 如果此时原实体已经不在（区块数据丢失、实体被外部原因移除等），节点就会永远
 * 无法通过「全灭」判定。本存档记录死亡瞬间仍存活敌人的完整实体 NBT，
 * 供下次到达该节点时按 UUID 补刷。</p>
 *
 * <p>只保存实体 NBT（含 UUID、类型、坐标、血量、标签、编队归属），
 * 恢复时必须沿用原 UUID，{@link DungeonObjectiveData} 里的存活登记才能继续生效。
 * 按 instanceId + nodeId 分桶，实例共享：任何参与者死亡都可写入，恢复只做一次。</p>
 */
public final class DungeonNodeSnapshotData extends SavedData {
    private static final String DATA_NAME = "piranport_dungeon_node_snapshots";

    /** 防御性上限：单个节点最多保存的实体条目。 */
    private static final int MAX_ENTRIES_PER_NODE = 128;
    /** 防御性上限：单个实例最多快照的节点数（关卡节点上限为 26）。 */
    private static final int MAX_NODES_PER_INSTANCE = 64;
    /** 防御性上限：一次读档最多恢复的实例数，防止损坏存档撑爆内存。 */
    private static final int MAX_INSTANCES = 64;

    /** instanceId → (nodeId → (entityUuid → 实体 NBT))。LinkedHashMap 保持刷怪顺序。 */
    private final Map<UUID, Map<String, LinkedHashMap<UUID, CompoundTag>>> instances = new HashMap<>();

    public DungeonNodeSnapshotData() {}

    private Map<String, LinkedHashMap<UUID, CompoundTag>> nodes(UUID instanceId, boolean create) {
        if (instanceId == null) return null;
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = instances.get(instanceId);
        if (nodes == null && create) {
            if (instances.size() >= MAX_INSTANCES) return null;
            nodes = new HashMap<>();
            instances.put(instanceId, nodes);
        }
        return nodes;
    }

    /** 某个节点当前的快照条目（只读；UUID → 实体 NBT）。 */
    public Map<UUID, CompoundTag> nodeEntries(UUID instanceId, String nodeId) {
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = nodes(instanceId, false);
        LinkedHashMap<UUID, CompoundTag> entries = nodes == null ? null : nodes.get(nodeId);
        return entries == null ? Map.of() : Collections.unmodifiableMap(entries);
    }

    public boolean hasNode(UUID instanceId, String nodeId) {
        return !nodeEntries(instanceId, nodeId).isEmpty();
    }

    /** 本实例所有有快照的节点 id（拷贝，供死亡时清理已阵亡条目）。 */
    public java.util.Set<String> nodesOf(UUID instanceId) {
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = nodes(instanceId, false);
        return nodes == null ? java.util.Set.of() : java.util.Set.copyOf(nodes.keySet());
    }

    /** 写入/覆盖单个实体条目；重复死亡时同一 UUID 用最新状态覆盖，不会叠加。 */
    public void put(UUID instanceId, String nodeId, UUID entityId, CompoundTag entityNbt) {
        if (instanceId == null || nodeId == null || nodeId.isBlank()
                || entityId == null || entityNbt == null) return;
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = nodes(instanceId, true);
        if (nodes == null) return;
        if (nodes.size() >= MAX_NODES_PER_INSTANCE && !nodes.containsKey(nodeId)) return;
        LinkedHashMap<UUID, CompoundTag> entries = nodes.computeIfAbsent(nodeId, ignored -> new LinkedHashMap<>());
        if (entries.size() >= MAX_ENTRIES_PER_NODE && !entries.containsKey(entityId)) {
            // 条目已满：丢弃新增项，保留原有快照（原快照至少是曾经确认存活的实体）。
            return;
        }
        entries.put(entityId, entityNbt.copy());
        setDirty();
    }

    public void remove(UUID instanceId, String nodeId, UUID entityId) {
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = nodes(instanceId, false);
        LinkedHashMap<UUID, CompoundTag> entries = nodes == null ? null : nodes.get(nodeId);
        if (entries != null && entries.remove(entityId) != null) setDirty();
    }

    /** 按谓词裁剪条目：谓词返回 false 的实体视为已经阵亡，不再补刷。 */
    public void retain(UUID instanceId, String nodeId, java.util.function.Predicate<UUID> keep) {
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = nodes(instanceId, false);
        LinkedHashMap<UUID, CompoundTag> entries = nodes == null ? null : nodes.get(nodeId);
        if (entries == null) return;
        if (entries.keySet().removeIf(uuid -> !keep.test(uuid))) setDirty();
        if (entries.isEmpty()) nodes.remove(nodeId);
    }

    public void clearNode(UUID instanceId, String nodeId) {
        Map<String, LinkedHashMap<UUID, CompoundTag>> nodes = nodes(instanceId, false);
        if (nodes != null && nodes.remove(nodeId) != null) setDirty();
    }

    public void clearInstance(UUID instanceId) {
        if (instanceId != null && instances.remove(instanceId) != null) setDirty();
    }

    // ===== SavedData =====

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag records = new ListTag();
        instances.forEach((instanceId, nodes) -> nodes.forEach((nodeId, entries) -> {
            if (entries.isEmpty()) return;
            CompoundTag record = new CompoundTag();
            record.putUUID("Instance", instanceId);
            record.putString("Node", nodeId);
            ListTag list = new ListTag();
            entries.forEach((entityId, nbt) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Entity", entityId);
                entry.put("Data", nbt);
                list.add(entry);
            });
            record.put("Entities", list);
            records.add(record);
        }));
        tag.put("Snapshots", records);
        return tag;
    }

    public static DungeonNodeSnapshotData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonNodeSnapshotData data = new DungeonNodeSnapshotData();
        ListTag records = tag.getList("Snapshots", Tag.TAG_COMPOUND);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            if (!record.hasUUID("Instance") || record.getString("Node").isBlank()) continue;
            UUID instanceId = record.getUUID("Instance");
            String nodeId = record.getString("Node");
            ListTag list = record.getList("Entities", Tag.TAG_COMPOUND);
            for (int j = 0; j < list.size(); j++) {
                CompoundTag entry = list.getCompound(j);
                if (!entry.hasUUID("Entity")) continue;
                if (!entry.contains("Data", Tag.TAG_COMPOUND)) continue;
                CompoundTag nbt = entry.getCompound("Data");
                // 恢复只能依赖带类型 id 与 UUID 的完整实体 NBT；缺任何一个都补不出来。
                if (!nbt.contains("id", Tag.TAG_STRING) || !nbt.hasUUID("UUID")) continue;
                data.put(instanceId, nodeId, entry.getUUID("Entity"), nbt);
            }
        }
        return data;
    }

    public static DungeonNodeSnapshotData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonNodeSnapshotData::new,
                        DungeonNodeSnapshotData::load, null),
                DATA_NAME);
    }
}
