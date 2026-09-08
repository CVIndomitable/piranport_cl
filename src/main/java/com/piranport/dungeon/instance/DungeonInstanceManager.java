package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages all active dungeon instances. Persisted as world SavedData.
 *
 * <p>整合版 §3.1 联机大厅与队长机制已作废（副本/10）：
 * <ul>
 *   <li>删除基于旗舰的反向索引，改为遍历 playerUuids 集合判断玩家是否在副本中</li>
 *   <li>整合版 §3.4：副本永不自动删除，删除 SUSPENDED_CLEANUP_MILLIS 与 sweepLeaks 时间段</li>
 * </ul>
 * </p>
 */
public class DungeonInstanceManager extends SavedData {
    private static final String DATA_NAME = "piranport_instances";

    private final Map<UUID, DungeonInstance> instances = new HashMap<>();
    private int nextIndex = 0;
    private final java.util.Queue<Integer> freedIndices = new java.util.ArrayDeque<>();
    /** Indices freed this tick — moved to freedIndices on next sweepLeaks() call so
     *  any in-flight crate/portal references in the just-discarded region cannot
     *  resolve into a freshly-created instance reusing the same offset. */
    private final java.util.List<Integer> pendingFreedIndices = new java.util.ArrayList<>();

    public DungeonInstanceManager() {}

    // ===== Instance Lifecycle =====

    /**
     * Creates a new dungeon instance for the given stage.
     */
    public DungeonInstance createInstance(String stageId, ServerPlayer creator,
                                          BlockPos lecternPos, String lecternDimension) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(stageId);
        if (stage == null) {
            PiranPort.LOGGER.warn("Cannot create instance: unknown stage {}", stageId);
            return null;
        }

        UUID instanceId = UUID.randomUUID();
        int index = freedIndices.isEmpty() ? nextIndex++ : freedIndices.poll();
        DungeonInstance instance = new DungeonInstance(instanceId, stageId, index);
        instance.setState(DungeonInstance.State.ACTIVE);
        instance.setLecternPos(lecternPos);
        instance.setLecternDimension(lecternDimension);
        instance.addPlayer(creator.getUUID());

        instances.put(instanceId, instance);
        setDirty();

        PiranPort.LOGGER.info("Created dungeon instance {} for stage {} (index {})",
                instanceId, stageId, index);
        return instance;
    }

    /**
     * Loads an existing instance by UUID (for key reconnection).
     */
    public DungeonInstance getInstance(UUID instanceId) {
        return instances.get(instanceId);
    }

    /**
     * Suspends an instance (all players left).
     */
    public void suspendInstance(UUID instanceId) {
        DungeonInstance inst = instances.get(instanceId);
        if (inst != null && inst.getState() == DungeonInstance.State.ACTIVE) {
            inst.setState(DungeonInstance.State.SUSPENDED);
            setDirty();
        }
    }

    /**
     * Resumes a suspended instance.
     */
    public void resumeInstance(UUID instanceId) {
        DungeonInstance inst = instances.get(instanceId);
        if (inst != null && inst.getState() == DungeonInstance.State.SUSPENDED) {
            inst.setState(DungeonInstance.State.ACTIVE);
            setDirty();
        }
    }

    /**
     * Marks an instance as completed.
     */
    public void completeInstance(UUID instanceId) {
        DungeonInstance inst = instances.get(instanceId);
        if (inst != null) {
            inst.setState(DungeonInstance.State.COMPLETED);
            inst.setEndTimeMillis(System.currentTimeMillis());
            setDirty();
        }
    }

    /**
     * Cleans up an instance, removing it from the active list.
     * The actual block/entity cleanup in the dungeon dimension should happen before calling this.
     * The freed region index is delayed until the next sweepLeaks() pass so a brand-new
     * instance cannot land on top of any not-yet-discarded entities from the old region.
     */
    public void cleanupInstance(UUID instanceId) {
        DungeonInstance inst = instances.remove(instanceId);
        if (inst != null) {
            pendingFreedIndices.add(inst.getInstanceIndex());
            inst.setState(DungeonInstance.State.CLEANUP);
            setDirty();
            PiranPort.LOGGER.info("Cleaned up dungeon instance {}", instanceId);
        }
    }

    /**
     * 整合版 §3.4：副本永远不会重置进度，也不会自动删除——唯一重开方式 = 钥匙取下+合成重置。
     * 本方法仅将 pendingFreedIndices 提升为 freedIndices 供后续新实例复用区域索引；
     * SUSPENDED 状态的实例永不被自动清理（区块卸载由原版 ChunkUnloadEvent 接管）。
     */
    public void sweepLeaks() {
        if (!pendingFreedIndices.isEmpty()) {
            freedIndices.addAll(pendingFreedIndices);
            pendingFreedIndices.clear();
            setDirty();
        }
    }

    /**
     * Updates the progress on both the instance and the key.
     * Idempotent: re-calling for an already-cleared node is a no-op.
     * Returns true if state actually changed.
     */
    public boolean advanceNode(UUID instanceId, String nodeId, ItemStack keyStack) {
        DungeonInstance inst = instances.get(instanceId);
        if (inst == null) return false;
        // Idempotent guard against duplicate SelectNodePayload / re-entry races.
        if (inst.getClearedNodes().contains(nodeId)) return false;

        // Verify key's instanceId matches current instance
        UUID keyInstanceId = keyStack.get(com.piranport.registry.ModDataComponents.DUNGEON_INSTANCE_ID.get());
        if (keyInstanceId == null || !keyInstanceId.equals(instanceId)) return false;

        // Verify nodeId belongs to current stage
        StageData stage = DungeonRegistry.INSTANCE.getStage(inst.getStageId());
        if (stage == null || !stage.nodes().containsKey(nodeId)) return false;

        inst.setCurrentNode(nodeId);
        inst.addClearedNode(nodeId);

        // Also update the key's DataComponent
        DungeonProgress progress = DungeonKeyItem.getProgress(keyStack);
        progress = progress.withCurrentNode(nodeId).withNodeCleared(nodeId);
        if (!progress.timerStarted()) {
            progress = progress.withTimerStarted(System.currentTimeMillis());
            inst.setStartTimeMillis(System.currentTimeMillis());
        }
        DungeonKeyItem.setProgress(keyStack, progress);
        setDirty();
        return true;
    }

    // ===== SavedData =====

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextIndex", nextIndex);
        if (!freedIndices.isEmpty()) {
            tag.putIntArray("FreedIndices", freedIndices.stream().mapToInt(Integer::intValue).toArray());
        }
        // P0 #2: 持久化 pendingFreedIndices 防止服务器崩溃时索引永久丢失
        if (!pendingFreedIndices.isEmpty()) {
            tag.putIntArray("PendingFreedIndices", pendingFreedIndices.stream().mapToInt(Integer::intValue).toArray());
        }
        ListTag list = new ListTag();
        for (DungeonInstance inst : instances.values()) {
            list.add(inst.save());
        }
        tag.put("Instances", list);
        return tag;
    }

    public static DungeonInstanceManager load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonInstanceManager mgr = new DungeonInstanceManager();
        mgr.nextIndex = tag.getInt("NextIndex");
        if (tag.contains("FreedIndices")) {
            for (int idx : tag.getIntArray("FreedIndices")) {
                mgr.freedIndices.add(idx);
            }
        }
        // P0 #2: 恢复 pendingFreedIndices
        if (tag.contains("PendingFreedIndices")) {
            for (int idx : tag.getIntArray("PendingFreedIndices")) {
                mgr.pendingFreedIndices.add(idx);
            }
        }
        ListTag list = tag.getList("Instances", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            DungeonInstance inst = DungeonInstance.load(list.getCompound(i));
            mgr.instances.put(inst.getInstanceId(), inst);
            // 整合版 §3.1：不再基于旗舰重建反向索引
        }
        return mgr;
    }

    public static DungeonInstanceManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonInstanceManager::new,
                        DungeonInstanceManager::load, null),
                DATA_NAME);
    }

    /**
     * 玩家断连时遍历全部 ACTIVE 实例，暂停该玩家参与的所有副本。
     * 整合版 §3.1：玩家不再绑定旗舰身份，任何参与副本的玩家离开都触发 SUSPENDED。
     */
    public void handlePlayerDisconnect(UUID playerUuid) {
        for (DungeonInstance inst : instances.values()) {
            if (inst.getState() != DungeonInstance.State.ACTIVE) continue;
            if (!inst.getPlayerUuids().contains(playerUuid)) continue;
            suspendInstance(inst.getInstanceId());
            PiranPort.LOGGER.debug("Suspended instance {} due to player disconnect: {}",
                    inst.getInstanceId(), playerUuid);
        }
    }
}