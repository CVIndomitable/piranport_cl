package com.piranport.aviation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side store for ASW sonar detections.
 * Each ASW aircraft sends its own scan results; this aggregates them.
 * Yellow highlight for detected underwater entities — independent of Y-key battlefield highlight.
 */
@OnlyIn(Dist.CLIENT)
public class ClientAswSonarData {

    /** aircraft entity ID → set of detected entity IDs from that aircraft's last scan */
    private static final Map<Integer, Set<Integer>> detectionsByAircraft = new ConcurrentHashMap<>();

    /** Tick counter for each aircraft's last update, used to prune stale entries. */
    private static final Map<Integer, Long> lastUpdateTick = new ConcurrentHashMap<>();

    private static long clientTick = 0;

    public static void update(int aircraftEntityId, List<Integer> detectedIds) {
        detectionsByAircraft.put(aircraftEntityId, new HashSet<>(detectedIds));
        lastUpdateTick.put(aircraftEntityId, clientTick);
    }

    /** Check if an entity is detected by any ASW sonar. */
    public static boolean isDetected(int entityId) {
        for (Set<Integer> ids : detectionsByAircraft.values()) {
            if (ids.contains(entityId)) return true;
        }
        return false;
    }

    /** Returns true if there are any active ASW sonar detections. */
    public static boolean hasDetections() {
        return !detectionsByAircraft.isEmpty();
    }

    /** Called each client tick to prune stale aircraft entries. */
    public static void tick() {
        clientTick++;
        // Remove aircraft entries that haven't been updated in 40 ticks (2 scan cycles)
        lastUpdateTick.entrySet().removeIf(entry -> {
            if (clientTick - entry.getValue() > 40) {
                detectionsByAircraft.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /** Get all currently detected entity IDs (union across all ASW aircraft). */
    public static Set<Integer> getAllDetected() {
        Set<Integer> all = new HashSet<>();
        for (Set<Integer> ids : detectionsByAircraft.values()) {
            all.addAll(ids);
        }
        return all;
    }

    public static void clear() {
        detectionsByAircraft.clear();
        lastUpdateTick.clear();
    }

    public static void resetClientState() {
        clear();
        clientTick = 0;
    }
}
