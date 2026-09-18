package com.piranport.dungeon.instance;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DungeonNodeProgressTest {
    @Test
    void enteringBattleDoesNotClearItOrRegenerateItAfterReload() {
        DungeonInstance instance = activeInstance();
        assertTrue(instance.beginNode("A"));
        assertFalse(instance.getClearedNodes().contains("A"));

        DungeonInstance restored = DungeonInstance.load(instance.save());
        assertTrue(restored.hasEnteredNode("A"));
        assertFalse(restored.beginNode("A"));
        assertFalse(restored.getClearedNodes().contains("A"));
        assertTrue(restored.clearNode("A"));
        assertFalse(restored.clearNode("A"));
    }

    @Test
    void unknownNodesCannotBeClearedAndCompletedInstancesCannotStartNewNodes() {
        DungeonInstance instance = activeInstance();
        assertFalse(instance.clearNode("unknown"));
        instance.setState(DungeonInstance.State.COMPLETED);
        assertFalse(instance.beginNode("A"));
    }

    private static DungeonInstance activeInstance() {
        DungeonInstance instance = new DungeonInstance(UUID.randomUUID(), "test", 0);
        instance.setState(DungeonInstance.State.ACTIVE);
        return instance;
    }
}
