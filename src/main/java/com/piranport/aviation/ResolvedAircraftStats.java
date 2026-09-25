package com.piranport.aviation;

import java.util.Objects;

/** Immutable aircraft statistics resolved once when an aircraft is created. */
public record ResolvedAircraftStats(String definitionId, float damage, float speed, int health,
                                    int cooldown, int fuelCapacity, int ammoCapacity, int weight) {
    public ResolvedAircraftStats {
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        if (definitionId.isBlank()) throw new IllegalArgumentException("definitionId must not be blank");
        if (!Float.isFinite(damage) || damage < 0.0F) throw new IllegalArgumentException("invalid damage");
        if (!Float.isFinite(speed) || speed <= 0.0F) throw new IllegalArgumentException("invalid speed");
        if (health < 1 || cooldown < 1 || fuelCapacity < 1 || ammoCapacity < 0 || weight < 0) {
            throw new IllegalArgumentException("aircraft stats are outside valid ranges");
        }
    }
}
