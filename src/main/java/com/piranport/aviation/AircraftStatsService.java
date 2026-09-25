package com.piranport.aviation;

import com.piranport.component.AircraftInfo;
import com.piranport.config.ModAircraftConfig;

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
        AircraftInfo.AircraftType type = definition.aircraftClass();
        int health = safeInt(() -> health(type), legacyHealth(type));
        int cooldown = safeInt(() -> cooldown(type), legacyCooldown(type));
        return new ResolvedAircraftStats(definition.id(), definition.panelDamage(), definition.panelSpeed(),
                health, cooldown, definition.fuelCapacity(), definition.ammoCapacity(), definition.weight());
    }

    private static int health(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER -> ModAircraftConfig.FIGHTER_HEALTH.get();
            case ROCKET_FIGHTER -> ModAircraftConfig.ROCKET_FIGHTER_HEALTH.get();
            case DIVE_BOMBER -> ModAircraftConfig.DIVE_BOMBER_HEALTH.get();
            case LEVEL_BOMBER -> ModAircraftConfig.LEVEL_BOMBER_HEALTH.get();
            case TORPEDO_BOMBER -> ModAircraftConfig.TORPEDO_BOMBER_HEALTH.get();
            case ASW -> ModAircraftConfig.ASW_AIRCRAFT_HEALTH.get();
            case RECON -> ModAircraftConfig.RECON_AIRCRAFT_HEALTH.get();
        };
    }

    private static int cooldown(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER -> ModAircraftConfig.FIGHTER_COOLDOWN.get();
            case ROCKET_FIGHTER -> ModAircraftConfig.ROCKET_FIGHTER_COOLDOWN.get();
            case DIVE_BOMBER -> ModAircraftConfig.DIVE_BOMBER_COOLDOWN.get();
            case LEVEL_BOMBER -> ModAircraftConfig.LEVEL_BOMBER_COOLDOWN.get();
            case TORPEDO_BOMBER -> ModAircraftConfig.TORPEDO_BOMBER_COOLDOWN.get();
            case ASW -> ModAircraftConfig.ASW_AIRCRAFT_COOLDOWN.get();
            case RECON -> 1;
        };
    }

    private static int legacyHealth(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER, ROCKET_FIGHTER, ASW -> 20;
            case DIVE_BOMBER, TORPEDO_BOMBER -> 25;
            case LEVEL_BOMBER -> 30;
            case RECON -> 15;
        };
    }

    private static int legacyCooldown(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER -> 40;
            case ROCKET_FIGHTER -> 60;
            case DIVE_BOMBER, TORPEDO_BOMBER -> 100;
            case LEVEL_BOMBER -> 120;
            case ASW -> 80;
            case RECON -> 1;
        };
    }

    private interface IntSupplier { int get(); }
    private static int safeInt(IntSupplier supplier, int fallback) {
        try { int value = supplier.get(); return value > 0 ? value : fallback; }
        catch (RuntimeException ex) { return fallback; }
    }
}
