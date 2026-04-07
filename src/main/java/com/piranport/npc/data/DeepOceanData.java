package com.piranport.npc.data;

import java.util.List;

/**
 * Data structure for deep ocean entity configuration, loaded from JSON.
 */
public record DeepOceanData(
        String entityId,
        double health,
        double armor,
        double speed,
        double detectionRange,
        double orbitDistance,
        List<String> weapons,
        String lootTable,
        int trackingIntervalMin,
        int trackingIntervalMax,
        int fireInterval,
        float shellDamage,
        float explosionPower
) {
    /**
     * Create a default data entry for fallback.
     */
    public static DeepOceanData defaults(String entityId) {
        return new DeepOceanData(entityId, 50, 8, 0.25, 32, 16,
                List.of("cannon"), "", 3, 6, 80, 5.0f, 1.5f);
    }
}
