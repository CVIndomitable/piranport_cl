package com.piranport.aviation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
/** Server-side fire control state. Maps player UUID → locked entity UUIDs.
 *  Primarily accessed on server thread; ConcurrentHashMap guards clearAll() during shutdown. */
public class FireControlManager {

    private static final Map<UUID, List<UUID>> LOCKED_TARGETS = new ConcurrentHashMap<>();
    private static final int MAX_TARGETS = 4;

    /** Replace the target list with a single target. */
    public static void lock(UUID playerUUID, UUID targetUUID) {
        List<UUID> list = new ArrayList<>();
        list.add(targetUUID);
        LOCKED_TARGETS.put(playerUUID, list);
        com.piranport.debug.PiranPortDebug.event(
                "FireControl LOCK | player={} target={}", playerUUID, targetUUID);
    }

    /** Append a target (up to MAX_TARGETS). Does nothing if already in list. */
    public static void addTarget(UUID playerUUID, UUID targetUUID) {
        List<UUID> list = LOCKED_TARGETS.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        if (!list.contains(targetUUID) && list.size() < MAX_TARGETS) {
            list.add(targetUUID);
            com.piranport.debug.PiranPortDebug.event(
                    "FireControl ADD | player={} target={} total={}", playerUUID, targetUUID, list.size());
        }
    }

    /** Remove all locked targets for this player. */
    public static void clearTargets(UUID playerUUID) {
        LOCKED_TARGETS.remove(playerUUID);
        com.piranport.debug.PiranPortDebug.event("FireControl CANCEL | player={}", playerUUID);
    }

    /** Returns an unmodifiable snapshot of the player's locked targets. */
    public static List<UUID> getTargets(UUID playerUUID) {
        List<UUID> list = LOCKED_TARGETS.get(playerUUID);
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Remove all state (call on server stop / world unload). */
    public static void clearAll() {
        LOCKED_TARGETS.clear();
    }
}
