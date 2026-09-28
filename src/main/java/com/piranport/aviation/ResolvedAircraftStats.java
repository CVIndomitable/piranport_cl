package com.piranport.aviation;

import java.util.Objects;

/** Immutable aircraft statistics resolved once when an aircraft is created. */
public record ResolvedAircraftStats(String definitionId, float damage, float speed, int health,
                                    int cooldown, int fuelCapacity, int ammoCapacity, int weight,
                                    int reloadTime) {
    /** Default manual aircraft loading duration (3 seconds = 60 game ticks). */
    public static final int DEFAULT_RELOAD_TIME = 60;

    /** Compatibility constructor for callers that only resolve attack statistics. */
    public ResolvedAircraftStats(String definitionId, float damage, float speed, int health,
                                 int cooldown, int fuelCapacity, int ammoCapacity, int weight) {
        this(definitionId, damage, speed, health, cooldown, fuelCapacity, ammoCapacity, weight,
                DEFAULT_RELOAD_TIME);
    }

    public ResolvedAircraftStats {
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        if (definitionId.isBlank()) throw new IllegalArgumentException("definitionId must not be blank");
        if (!Float.isFinite(damage) || damage < 0.0F) throw new IllegalArgumentException("invalid damage");
        if (!Float.isFinite(speed) || speed <= 0.0F) throw new IllegalArgumentException("invalid speed");
        if (health < 1 || cooldown < 1 || fuelCapacity < 1 || ammoCapacity < 0 || weight < 0
                || reloadTime < 1) {
            throw new IllegalArgumentException("aircraft stats are outside valid ranges");
        }
    }
}
