package com.piranport.dungeon.event;

import com.piranport.dungeon.instance.DungeonInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FirstClearParticipationTest {

    @Test
    void onlySnapshotParticipantsAreEligible() {
        DungeonInstance inst = new DungeonInstance(UUID.randomUUID(), "t", 0);
        UUID inside = UUID.randomUUID();
        UUID outside = UUID.randomUUID();
        inst.addPlayer(inside);
        inst.addPlayer(outside);
        inst.snapshotBossParticipants(List.of(inside));
        assertTrue(FirstClearRules.eligible(inst, inside));
        assertFalse(FirstClearRules.eligible(inst, outside));
    }

    @Test
    void legacySaveWithoutSnapshotFallsBackToEveryone() {
        DungeonInstance inst = new DungeonInstance(UUID.randomUUID(), "t", 0);
        assertTrue(FirstClearRules.eligible(inst, UUID.randomUUID()));
    }

    @Test
    void lastBossKillSnapshotWinsAndPersists() {
        DungeonInstance inst = new DungeonInstance(UUID.randomUUID(), "t", 0);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        inst.snapshotBossParticipants(List.of(a));
        inst.snapshotBossParticipants(List.of(b));
        DungeonInstance loaded = DungeonInstance.load(inst.save());
        assertFalse(FirstClearRules.eligible(loaded, a));
        assertTrue(FirstClearRules.eligible(loaded, b));
        // 空快照（击杀瞬间无人在 Boss 节点）也要持久化为“无人有资格”
        inst.snapshotBossParticipants(List.of());
        assertFalse(FirstClearRules.eligible(DungeonInstance.load(inst.save()), b));
    }
}
