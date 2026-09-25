package com.piranport.aviation;

import com.piranport.component.AircraftInfo;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves one immutable aircraft statistics snapshot; callers should cache it per aircraft. */
public final class AircraftStatsService {
    private static final Map<AircraftDefinition, ResolvedAircraftStats> CACHE = new ConcurrentHashMap<>();
    private AircraftStatsService() {}

    public static ResolvedAircraftStats resolve(AircraftInfo info) {
        Objects.requireNonNull(info, "info");
        return resolve(AircraftDefinitionService.resolve(info));
    }

    public static ResolvedAircraftStats resolve(AircraftDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return CACHE.computeIfAbsent(definition, AircraftStatsService::build);
    }

    public static void clear() { CACHE.clear(); }
    public static Map<AircraftDefinition, ResolvedAircraftStats> snapshot() { return Map.copyOf(CACHE); }

    private static ResolvedAircraftStats build(AircraftDefinition definition) {
        return new ResolvedAircraftStats(definition.id(), definition.panelDamage(), definition.panelSpeed(),
                definition.health(), definition.attackCooldown(), definition.fuelCapacity(),
                definition.ammoCapacity(), definition.weight());
    }

}
