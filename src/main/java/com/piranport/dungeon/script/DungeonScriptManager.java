package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Persists active dungeon scripts as world SavedData attached to the dungeon dimension.
 * Ticked once per server tick from the game event bus. All access is on the server main
 * thread, so a plain HashMap is safe.
 */
public final class DungeonScriptManager extends SavedData {
    private static final String DATA_NAME = "piranport_dungeon_scripts";
    /** Defensive upper bound for scripts restored from one SavedData blob. */
    private static final int MAX_SCRIPTS = 256;

    private final Map<UUID, DungeonScript> activeScripts = new HashMap<>();
    /** 异常脚本停止调度但保留存档；重启或显式替换脚本后才重试，避免每 tick 刷屏。 */
    private final Set<UUID> failedScripts = new HashSet<>();

    public DungeonScriptManager() {}

    public static DungeonScriptManager get(MinecraftServer server) {
        // Always store on overworld dataStorage (consistent with DungeonInstanceManager).
        // Storing on the dungeon dimension caused state to split between two SavedData files
        // depending on whether that dimension was loaded at access time.
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonScriptManager::new,
                        DungeonScriptManager::load, null),
                DATA_NAME);
    }

    /** Register a new script for a dungeon instance. */
    public void start(UUID instanceId, DungeonScript script) {
        activeScripts.put(instanceId, script);
        failedScripts.remove(instanceId);
        setDirty();
        PiranPort.LOGGER.info("Dungeon script started for instance {}: {}",
                instanceId, script.getClass().getSimpleName());
    }

    /** Remove the script for the given instance. */
    public void remove(UUID instanceId) {
        DungeonScript removed = activeScripts.remove(instanceId);
        failedScripts.remove(instanceId);
        if (removed != null) {
            setDirty();
            PiranPort.LOGGER.info("Dungeon script removed for instance {}", instanceId);
        }
    }

    /** Get the active script for an instance, or null. */
    public DungeonScript getScript(UUID instanceId) {
        return activeScripts.get(instanceId);
    }

    /** 唯一的脚本调度入口：先按实际玩家位置校准实例，再推进运行中的剧情。 */
    public void tickAll(ServerLevel dungeonLevel) {
        DungeonInstanceManager instances = DungeonInstanceManager.get(dungeonLevel);
        instances.refreshPlayerPresence(dungeonLevel.getServer());
        tickAll(dungeonLevel, instances::getInstance, instance -> {
            // 重连时实体 NBT 可能尚未加载，等待节点入口区块实体就绪，不将缺失实体当作死亡。
            String node = instance.getCurrentNode();
            return node != null && dungeonLevel.areEntitiesLoaded(
                    new ChunkPos(instance.getNodeSpawnPos(node)).toLong());
        });
    }

    /** 保留真实调度逻辑，通过实例解析和区块就绪查询隔离服务端依赖。 */
    void tickAll(ServerLevel dungeonLevel, Function<UUID, DungeonInstance> findInstance,
                 Predicate<DungeonInstance> isReady) {
        boolean changed = false;
        Iterator<Map.Entry<UUID, DungeonScript>> iter = activeScripts.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, DungeonScript> entry = iter.next();
            UUID instanceId = entry.getKey();
            DungeonInstance instance = findInstance.apply(instanceId);
            if (instance == null || instance.getState() == DungeonInstance.State.COMPLETED
                    || instance.getState() == DungeonInstance.State.CLEANUP) {
                iter.remove();
                failedScripts.remove(instanceId);
                changed = true;
                continue;
            }
            // SUSPENDED 保留原脚本及计时器，恢复后从同一阶段继续；CREATING 也不能提前运行。
            if (instance.getState() != DungeonInstance.State.ACTIVE
                    || failedScripts.contains(instanceId) || !isReady.test(instance)) continue;

            DungeonScript script = entry.getValue();
            try {
                // 现有脚本的计时器每 tick 都会变化，即使 tick() 未报告阶段转换也需要保存。
                changed = true;
                if (!script.isFinished()) script.tick(dungeonLevel);
                if (script.isFinished()) {
                    iter.remove();
                    PiranPort.LOGGER.info("Dungeon script finished for instance {}", instanceId);
                }
            } catch (Exception e) {
                failedScripts.add(instanceId);
                PiranPort.LOGGER.error("Dungeon script paused after an error; saved state retained for instance {}",
                        instanceId, e);
            }
        }
        if (changed) setDirty();
    }

    /**
     * Notify scripts that a dungeon entity (e.g. destroyer) died.
     */
    public void onEntityDeath(UUID instanceId, Entity entity) {
        DungeonScript script = activeScripts.get(instanceId);
        if (script != null && script.onEntityDeath(entity)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, DungeonScript> entry : activeScripts.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.put("InstanceId", NbtUtils.createUUID(entry.getKey()));
            e.putString("Type", entry.getValue().typeId());
            CompoundTag data = new CompoundTag();
            try {
                entry.getValue().writeNbt(data);
            } catch (Exception ex) {
                PiranPort.LOGGER.error("Failed to serialize dungeon script {}",
                        entry.getValue().getClass().getSimpleName(), ex);
                continue;
            }
            e.put("Data", data);
            list.add(e);
        }
        tag.put("Scripts", list);
        return tag;
    }

    public static DungeonScriptManager load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonScriptManager mgr = new DungeonScriptManager();
        ListTag list = tag.getList("Scripts", Tag.TAG_COMPOUND);

        // Bound restored scripts so corrupt or oversized saved data cannot exhaust memory.
        int count = Math.min(list.size(), MAX_SCRIPTS);
        if (list.size() > count) {
            PiranPort.LOGGER.warn(
                    "Dungeon script save contains {} entries; loading only first {}",
                    list.size(), count);
        }

        for (int i = 0; i < count; i++) {
            CompoundTag e = list.getCompound(i);
            UUID id = null;
            try {
                id = NbtUtils.loadUUID(e.get("InstanceId"));
                String type = e.getString("Type");
                CompoundTag data = e.getCompound("Data");

                if (id == null || type == null || type.isBlank()) {
                    PiranPort.LOGGER.warn("Invalid dungeon script entry {}: missing instance/type, skipping", i);
                    continue;
                }

                DungeonScript script = DungeonScriptRegistry.create(type, data);
                if (script == null) {
                    PiranPort.LOGGER.warn(
                            "Unknown dungeon script type '{}' for instance {}, skipping", type, id);
                    continue;
                }
                mgr.activeScripts.put(id, script);
            } catch (Exception ex) {
                PiranPort.LOGGER.warn("Invalid dungeon script entry {}, skipping", i, ex);
            }
        }
        return mgr;
    }
}
