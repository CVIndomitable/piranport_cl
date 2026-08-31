package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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
        setDirty();
        PiranPort.LOGGER.info("Dungeon script started for instance {}: {}",
                instanceId, script.getClass().getSimpleName());
    }

    /** Remove the script for the given instance. */
    public void remove(UUID instanceId) {
        DungeonScript removed = activeScripts.remove(instanceId);
        if (removed != null) {
            setDirty();
            PiranPort.LOGGER.info("Dungeon script removed for instance {}", instanceId);
        }
    }

    /** Get the active script for an instance, or null. */
    public DungeonScript getScript(UUID instanceId) {
        return activeScripts.get(instanceId);
    }

    /** Tick all active scripts. Called from ServerTickEvent. */
    public void tickAll(ServerLevel dungeonLevel) {
        boolean changed = false;
        Iterator<Map.Entry<UUID, DungeonScript>> iter = activeScripts.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, DungeonScript> entry = iter.next();
            DungeonScript script = entry.getValue();
            try {
                if (script.tick(dungeonLevel)) changed = true;
                if (script.isFinished()) {
                    iter.remove();
                    changed = true;
                    PiranPort.LOGGER.info("Dungeon script finished for instance {}", entry.getKey());
                }
            } catch (Exception e) {
                PiranPort.LOGGER.error("Error ticking dungeon script for instance {}",
                        entry.getKey(), e);
                iter.remove();
                changed = true;
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
