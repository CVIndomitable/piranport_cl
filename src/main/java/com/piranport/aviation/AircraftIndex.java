package com.piranport.aviation;

import com.piranport.entity.AircraftEntity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owner-indexed registry of live AircraftEntity instances.
 *
 * <p>Lets logout/death/dimension-change handlers find a player's airborne aircraft
 * in O(k) instead of scanning every loaded entity (which is O(N) and noticeable on
 * large servers, see audit P2-2).</p>
 *
 * <p>Updated by {@link AircraftEntity}'s join/leave hooks. Server-thread access is
 * the norm, but ConcurrentHashMap keeps shutdown clearAll() and async listeners safe.</p>
 */
public final class AircraftIndex {
    private AircraftIndex() {}

    private static final ConcurrentHashMap<UUID, Set<AircraftEntity>> BY_OWNER = new ConcurrentHashMap<>();

    public static void add(UUID ownerUuid, AircraftEntity aircraft) {
        if (ownerUuid == null) return;
        BY_OWNER.computeIfAbsent(ownerUuid, k -> ConcurrentHashMap.newKeySet()).add(aircraft);
    }

    public static void remove(UUID ownerUuid, AircraftEntity aircraft) {
        if (ownerUuid == null) return;
        Set<AircraftEntity> set = BY_OWNER.get(ownerUuid);
        if (set == null) return;
        set.remove(aircraft);
        if (set.isEmpty()) BY_OWNER.remove(ownerUuid);
    }

    /** Returns a snapshot of the player's currently-loaded aircraft (safe to iterate while removing). */
    public static Set<AircraftEntity> snapshot(UUID ownerUuid) {
        Set<AircraftEntity> set = BY_OWNER.get(ownerUuid);
        return set == null ? Collections.emptySet() : Set.copyOf(set);
    }

    public static void clearAll() {
        BY_OWNER.clear();
    }
}
