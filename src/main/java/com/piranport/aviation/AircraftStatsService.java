package com.piranport.aviation;

import com.piranport.component.AircraftInfo;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import com.piranport.terminal.TerminalParameters;

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
        // 参数镜像可在存档切换或人工修改时更新；只在有效数值变化时替换快照。
        ResolvedAircraftStats current = build(definition);
        return CACHE.compute(definition, (ignored, previous) ->
                previous != null && previous.equals(current) ? previous : current);
    }

    public static void clear() { CACHE.clear(); }
    public static Map<AircraftDefinition, ResolvedAircraftStats> snapshot() { return Map.copyOf(CACHE); }

    private static ResolvedAircraftStats build(AircraftDefinition definition) {
        String key = "aircraft." + definition.id() + ".";
        return new ResolvedAircraftStats(definition.id(),
                (float) TerminalParameters.getDouble(key + "panel_damage", definition.panelDamage()),
                (float) TerminalParameters.getDouble(key + "panel_speed", definition.panelSpeed()),
                TerminalParameters.getInt(key + "health", definition.health()),
                TerminalParameters.getInt(key + "attack_cooldown", definition.attackCooldown()),
                TerminalParameters.getInt(key + "fuel_capacity", definition.fuelCapacity()),
                TerminalParameters.getInt(key + "ammo_capacity", definition.ammoCapacity()),
                TerminalParameters.getInt(key + "weight", definition.weight()));
    }

}
