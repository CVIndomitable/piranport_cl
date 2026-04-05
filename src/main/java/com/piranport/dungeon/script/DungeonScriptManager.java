package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages active dungeon scripts. Ticked once per server tick from the game event bus.
 * Each script is keyed by its dungeon instance UUID.
 */
public final class DungeonScriptManager {
    private DungeonScriptManager() {}

    // All access is on the server main thread (via enqueueWork / ServerTickEvent), plain HashMap suffices
    private static final Map<UUID, DungeonScript> ACTIVE_SCRIPTS = new HashMap<>();

    /** Register a new script for a dungeon instance. */
    public static void start(UUID instanceId, DungeonScript script) {
        ACTIVE_SCRIPTS.put(instanceId, script);
        PiranPort.LOGGER.info("Dungeon script started for instance {}: {}",
                instanceId, script.getClass().getSimpleName());
    }

    /** Remove the script for the given instance. */
    public static void remove(UUID instanceId) {
        DungeonScript removed = ACTIVE_SCRIPTS.remove(instanceId);
        if (removed != null) {
            PiranPort.LOGGER.info("Dungeon script removed for instance {}", instanceId);
        }
    }

    /** Get the active script for an instance, or null. */
    public static DungeonScript get(UUID instanceId) {
        return ACTIVE_SCRIPTS.get(instanceId);
    }

    /** Tick all active scripts. Called from ServerTickEvent. */
    public static void tickAll(ServerLevel dungeonLevel) {
        var iter = ACTIVE_SCRIPTS.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            DungeonScript script = entry.getValue();
            try {
                script.tick(dungeonLevel);
                if (script.isFinished()) {
                    iter.remove();
                    PiranPort.LOGGER.info("Dungeon script finished for instance {}",
                            entry.getKey());
                }
            } catch (Exception e) {
                PiranPort.LOGGER.error("Error ticking dungeon script for instance {}",
                        entry.getKey(), e);
                iter.remove();
            }
        }
    }

    /**
     * Notify scripts that a dungeon entity (e.g. destroyer) died.
     * Called from death event handlers.
     */
    public static void onEntityDeath(UUID instanceId, net.minecraft.world.entity.Entity entity) {
        DungeonScript script = ACTIVE_SCRIPTS.get(instanceId);
        if (script != null) {
            script.onEntityDeath(entity);
        }
    }

    /** Clear all scripts (e.g. on server stop). */
    public static void clearAll() {
        ACTIVE_SCRIPTS.clear();
    }
}
