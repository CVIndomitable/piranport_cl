package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Manages all active dungeon instances. Persisted as world SavedData.
 *
 * <p>整合版 §3.1 联机大厅与队长机制已作废（副本/10）：
 * <ul>
 *   <li>删除基于旗舰的反向索引，改为遍历 playerUuids 集合判断玩家是否在副本中</li>
     *   <li>整合版 §3.4：副本永不自动删除，完成实例也保留区域、存档和钥匙关联</li>
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
     * 返回当前管理器中所有实例（拷贝）。
     * 整合版 §3.2：用于 onCheckpointReached 按玩家反查所在 instance。
     */
    public java.util.Collection<DungeonInstance> getAllInstances() {
        return java.util.Collections.unmodifiableCollection(instances.values());
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
    public boolean completeInstance(UUID instanceId) {
        DungeonInstance inst = instances.get(instanceId);
        if (inst != null && inst.getState() != DungeonInstance.State.COMPLETED
                && inst.getState() != DungeonInstance.State.CLEANUP) {
            inst.setState(DungeonInstance.State.COMPLETED);
            inst.setEndTimeMillis(System.currentTimeMillis());
            setDirty();
            return true;
        }
        return false;
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
     * 整合版 §3.4：副本永远不会重置进度，也不会自动删除；复制钥匙创建独立的新实例。
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

    /** 战斗只登记已进入，通关由传送门结算登记。 */
    public boolean beginBattleNode(UUID instanceId, String nodeId, ItemStack keyStack) {
        DungeonInstance instance = instances.get(instanceId);
        if (instance == null || !instanceId.equals(DungeonKeyItem.getInstanceId(keyStack))) return false;
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null || !stage.nodes().containsKey(nodeId) || !instance.beginNode(nodeId)) return false;
        if (instance.getStartTimeMillis() == 0) instance.setStartTimeMillis(System.currentTimeMillis());
        syncKey(instance, keyStack);
        setDirty();
        return true;
    }

    /** 资源与费用节点当场结算；重复调用不会再次扣费或发奖。 */
    public boolean advanceNode(UUID instanceId, String nodeId, ItemStack keyStack) {
        if (!beginBattleNode(instanceId, nodeId, keyStack)) return false;
        return markNodeCleared(instanceId, nodeId, keyStack);
    }

    public boolean markNodeCleared(UUID instanceId, String nodeId, ItemStack keyStack) {
        DungeonInstance instance = instances.get(instanceId);
        if (instance == null || !instance.clearNode(nodeId)) return false;
        syncKey(instance, keyStack);
        setDirty();
        return true;
    }

    /** 实例存档是运行状态源；钥匙只同步该实例的元进度，取出再插入也能恢复。 */
    public void syncKey(DungeonInstance instance, ItemStack keyStack) {
        if (keyStack.isEmpty() || !instance.getInstanceId().equals(DungeonKeyItem.getInstanceId(keyStack))) return;
        DungeonKeyItem.setProgress(keyStack, new DungeonProgress(
                instance.getCurrentNode() == null ? "" : instance.getCurrentNode(),
                java.util.Set.copyOf(instance.getClearedNodes()), instance.getStartTimeMillis(),
                instance.getStartTimeMillis() > 0));
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
     * 统一校准暂停/恢复：只有实例区域内仍有在线参与者才运行。
     * 脚本调度前也会调用，覆盖外部传送、同维度跨实例移动及服务器重启后的恢复。
     */
    public void refreshPlayerPresence(MinecraftServer server) {
        refreshPlayerPresence(null, (instance, uuid) -> isPlayerPresent(server, instance, uuid));
    }

    /** 登出事件触发时玩家可能仍在 PlayerList，必须明确排除正在离开的玩家。 */
    public void handlePlayerDisconnect(MinecraftServer server, UUID playerUuid) {
        refreshPlayerPresence(playerUuid, (instance, uuid) -> isPlayerPresent(server, instance, uuid));
    }

    /** 按实际位置找当前实例，历史参与记录不能把玩家归入另一个区域的副本。 */
    public DungeonInstance getInstanceForPlayer(ServerPlayer player) {
        for (DungeonInstance inst : instances.values()) {
            if (inst.getState() == DungeonInstance.State.CLEANUP) continue;
            if (inst.getPlayerUuids().contains(player.getUUID())
                    && isInsideInstance(inst, player.level().dimension(), player.blockPosition())) {
                return inst;
            }
        }
        return null;
    }

    private static boolean isPlayerPresent(MinecraftServer server, DungeonInstance instance, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        return player != null
                && isInsideInstance(instance, player.level().dimension(), player.blockPosition());
    }

    /** 使用分配区域而非可玩区，边缘缓冲带中的玩家仍属于原实例。 */
    static boolean isInsideInstance(DungeonInstance instance, ResourceKey<Level> dimension, BlockPos pos) {
        return DungeonEventHandler.DUNGEON_DIMENSION.equals(dimension)
                && Math.floorDiv(pos.getX(), DungeonConstants.REGION_SIZE)
                == Math.floorDiv(instance.getRegionOriginX(), DungeonConstants.REGION_SIZE)
                && Math.floorDiv(pos.getZ(), DungeonConstants.REGION_SIZE)
                == Math.floorDiv(instance.getRegionOriginZ(), DungeonConstants.REGION_SIZE);
    }

    /** 以在线位置查询为依赖边界，便于验证多人退出而无需构造完整 MinecraftServer。 */
    void refreshPlayerPresence(UUID departingPlayer, BiPredicate<DungeonInstance, UUID> isPresent) {
        for (DungeonInstance instance : instances.values()) {
            if (instance.getState() != DungeonInstance.State.ACTIVE
                    && instance.getState() != DungeonInstance.State.SUSPENDED) continue;

            boolean anyPresent = instance.getPlayerUuids().stream()
                    .anyMatch(uuid -> !uuid.equals(departingPlayer) && isPresent.test(instance, uuid));
            if (anyPresent) {
                resumeInstance(instance.getInstanceId());
            } else {
                suspendInstance(instance.getInstanceId());
            }
        }
    }
}
