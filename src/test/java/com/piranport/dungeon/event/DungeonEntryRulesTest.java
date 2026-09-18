package com.piranport.dungeon.event;

import com.piranport.dungeon.data.*;
import com.piranport.dungeon.instance.DungeonInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DungeonEntryRulesTest {
    private final CheckpointData checkpoint = new CheckpointData("cp", "A", 2, 64, -3, "south", "any");
    private final NodeData node = new NodeData("A", NodeData.NodeType.BATTLE, null,
            List.of(), List.of(), "", 0, 0, null, TerrainType.T1_OCEAN, Set.of(), SceneData.DAY);
    private final StageData stage = new StageData("test", "test", "test", Map.of("A", node, "B", node),
            List.of(new StageData.EdgeData("A", "B")), "A", List.of("B"), List.of(),
            List.of(checkpoint), Set.of(), SceneData.DAY, Set.of(), StageData.VictoryObjectives.EMPTY);

    @Test
    void unfinishedBattleCanBeReenteredButCannotUnlockTheNextNode() {
        DungeonInstance instance = instance(0);
        instance.beginNode("A");
        instance.setState(DungeonInstance.State.SUSPENDED);
        assertTrue(DungeonEntryRules.canEnter(instance, stage, "A"));
        assertFalse(DungeonEntryRules.canEnter(instance, stage, "B"));
        assertEquals(DungeonInstance.State.SUSPENDED, instance.getState());
        assertTrue(instance.getPlayerUuids().isEmpty());

        instance.clearNode("A");
        assertTrue(DungeonEntryRules.canEnter(instance, stage, "B"));
        assertTrue(DungeonEntryRules.canEnter(instance, stage, "A"));
    }

    @Test
    void completedInstanceOnlyAllowsPreviouslyGeneratedNodes() {
        DungeonInstance instance = instance(0);
        instance.beginNode("A");
        instance.clearNode("A");
        instance.setState(DungeonInstance.State.COMPLETED);
        assertTrue(DungeonEntryRules.canEnter(instance, stage, "A"));
        assertFalse(DungeonEntryRules.canEnter(instance, stage, "B"));
        assertFalse(DungeonEntryRules.canEnter(null, stage, "B"));
    }

    @Test
    void checkpointsArePersonalAndResolvedInsideTheirOwnInstanceRegion() {
        DungeonInstance first = instance(0);
        DungeonInstance second = instance(9);
        UUID player = UUID.randomUUID();
        first.beginNode("A");
        first.markCheckpointReached(player, "cp");
        assertEquals(checkpoint, DungeonEntryRules.latestCheckpoint(first, stage, player));
        assertNull(DungeonEntryRules.latestCheckpoint(first, stage, UUID.randomUUID()));
        var pos = DungeonEntryRules.checkpointPosition(first, checkpoint);
        var other = DungeonEntryRules.checkpointPosition(second, checkpoint);
        assertTrue(first.isInsideUsableArea(pos));
        assertTrue(second.isInsideUsableArea(other));
        assertEquals(64, pos.getY());
        assertNotEquals(pos, other);
    }

    private DungeonInstance instance(int index) {
        DungeonInstance instance = new DungeonInstance(UUID.randomUUID(), "test", index);
        instance.setState(DungeonInstance.State.ACTIVE);
        return instance;
    }
}
