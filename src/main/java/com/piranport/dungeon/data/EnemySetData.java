package com.piranport.dungeon.data;

import java.util.List;

/**
 * Parsed enemy set configuration from JSON.
 */
public record EnemySetData(
        String enemySetId,
        List<SpawnEntry> spawnList,
        SpawnEntry flagship,
        String formation
) {
    public record SpawnEntry(String entity, int count) {}

    public boolean hasFormation() {
        return formation != null && !formation.isEmpty();
    }
}
