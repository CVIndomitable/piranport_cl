package com.piranport.dungeon.event;

import com.piranport.dungeon.data.*;
import com.piranport.dungeon.event.BranchDoors.Door;
import com.piranport.dungeon.event.BranchDoors.State;
import com.piranport.dungeon.instance.DungeonInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BranchEvaluatorTest {

    private static BranchEvaluator.Context ctx(String hull, String carried, double roll) {
        return new BranchEvaluator.Context(hull, 0, Set.of(),
                id -> id.equals(carried), () -> roll);
    }

    private static NodeData branchNode(List<BranchRule> rules, String branchDefault) {
        return new NodeData("A", NodeData.NodeType.BATTLE, null, List.of(), List.of(), "", 0, 0,
                null, TerrainType.T1_OCEAN, Set.of(), SceneData.DAY, 0.0, 1,
                rules, branchDefault, null, null, null);
    }

    private static Door doorOf(List<Door> doors, String target) {
        return doors.stream().filter(d -> d.target().equals(target)).findFirst().orElseThrow();
    }

    @Test
    void hullMatchIgnoresCase() {
        BranchRule r = new BranchRule("medium", null, null, null, null, "B");
        assertEquals(0, BranchEvaluator.unmetMask(r, ctx("MEDIUM", null, 0)));
        assertEquals(BranchEvaluator.HULL, BranchEvaluator.unmetMask(r, ctx("small", null, 0)));
        assertEquals(BranchEvaluator.HULL, BranchEvaluator.unmetMask(r, ctx(null, null, 0)));
    }

    @Test
    void allConditionsInARuleAreAnded() {
        BranchRule r = new BranchRule("medium", null, null, "piranport:hentai_trophy", null, "B");
        assertEquals(0, BranchEvaluator.unmetMask(r, ctx("medium", "piranport:hentai_trophy", 0)));
        assertEquals(BranchEvaluator.CARRY,
                BranchEvaluator.unmetMask(r, ctx("medium", "minecraft:stone", 0)));
    }

    @Test
    void escortsAndRoleUseUndevelopedDefaults() {
        assertEquals(0, BranchEvaluator.unmetMask(
                new BranchRule(null, "<=0", null, null, null, "B"), ctx(null, null, 0)));
        assertEquals(BranchEvaluator.ESCORTS, BranchEvaluator.unmetMask(
                new BranchRule(null, ">=1", null, null, null, "B"), ctx(null, null, 0)));
        assertEquals(BranchEvaluator.ROLE, BranchEvaluator.unmetMask(
                new BranchRule(null, null, "防空", null, null, "B"), ctx(null, null, 0)));
    }

    @Test
    void chancePassesOnlyBelowThreshold() {
        BranchRule r = new BranchRule(null, null, null, null, 0.3, "B");
        assertTrue(BranchEvaluator.chancePasses(r, ctx(null, null, 0.29)));
        assertFalse(BranchEvaluator.chancePasses(r, ctx(null, null, 0.3)));
        assertTrue(BranchEvaluator.chancePasses(new BranchRule(null, null, null, null, null, "B"),
                ctx(null, null, 0.99)));
    }

    @Test
    void litDoorForMatchedRuleAndDarkForUnmetOthersDefault() {
        NodeData node = branchNode(List.of(
                new BranchRule("small", null, null, null, null, "B"),
                new BranchRule("large", null, null, null, null, "C")), "D");
        List<Door> doors = BranchDoors.compute(node, ctx("small", null, 0.5), t -> false, null);
        assertEquals(State.LIT, doorOf(doors, "B").state());
        assertEquals(State.DARK, doorOf(doors, "C").state());
        assertEquals(BranchEvaluator.HULL, doorOf(doors, "C").unmetMask());
        assertEquals(State.LIT, doorOf(doors, "D").state());
    }

    @Test
    void defaultDoorStaysLitWhenNoRuleMatches() {
        NodeData node = branchNode(List.of(new BranchRule("large", null, null, null, null, "B")), "D");
        List<Door> doors = BranchDoors.compute(node, ctx(null, null, 0.5), t -> false, null);
        assertEquals(State.DARK, doorOf(doors, "B").state());
        assertEquals(State.LIT, doorOf(doors, "D").state());
    }

    @Test
    void chanceFailureHidesDoorInsteadOfShowingPercent() {
        NodeData node = branchNode(List.of(new BranchRule(null, null, null, null, 0.3, "B")), null);
        assertEquals(State.HIDDEN, doorOf(
                BranchDoors.compute(node, ctx(null, null, 0.3), t -> false, null), "B").state());
        assertEquals(State.LIT, doorOf(
                BranchDoors.compute(node, ctx(null, null, 0.2), t -> false, null), "B").state());
    }

    @Test
    void anyPassingRuleForSameTargetLightsDoor() {
        NodeData node = branchNode(List.of(
                new BranchRule("large", null, null, null, null, "B"),
                new BranchRule("small", null, null, null, null, "B")), null);
        assertEquals(State.LIT, doorOf(
                BranchDoors.compute(node, ctx("small", null, 0.5), t -> false, null), "B").state());
    }

    @Test
    void clearedTargetAlwaysShowsDark() {
        NodeData node = branchNode(List.of(new BranchRule("small", null, null, null, null, "B")), "D");
        List<Door> doors = BranchDoors.compute(node, ctx("small", null, 0.5), "B"::equals, null);
        assertEquals(State.DARK, doorOf(doors, "B").state());
        assertEquals(0, doorOf(doors, "B").unmetMask());
        assertEquals(State.LIT, doorOf(doors, "D").state());
    }

    @Test
    void chosenRouteLeavesPassedDoorAndHidesTheRest() {
        NodeData node = branchNode(List.of(
                new BranchRule("small", null, null, null, null, "B"),
                new BranchRule(null, null, null, null, null, "C")), "D");
        List<Door> doors = BranchDoors.compute(node, ctx("small", null, 0.5), t -> false, "B");
        assertEquals(State.PASSED, doorOf(doors, "B").state());
        assertEquals(State.HIDDEN, doorOf(doors, "C").state());
        assertEquals(State.HIDDEN, doorOf(doors, "D").state());
    }

    @Test
    void comparisonParsing() {
        assertTrue(BranchEvaluator.compare(2, ">=2"));
        assertFalse(BranchEvaluator.compare(1, ">=2"));
        assertTrue(BranchEvaluator.compare(1, "<=1"));
        assertTrue(BranchEvaluator.compare(3, ">2"));
        assertTrue(BranchEvaluator.compare(1, "<2"));
        assertTrue(BranchEvaluator.compare(2, "==2"));
        assertTrue(BranchEvaluator.compare(2, "2"));
        assertFalse(BranchEvaluator.compare(2, "abc"));
        assertTrue(BranchEvaluator.isValidComparison(" >= 3"));
        assertFalse(BranchEvaluator.isValidComparison(">=x"));
        assertTrue(BranchEvaluator.isValidHull("Submarine"));
        assertFalse(BranchEvaluator.isValidHull("carrier"));
    }

    @Test
    void canEnterRespectsPerPlayerBranchSnapshot() {
        NodeData a = branchNode(List.of(new BranchRule("small", null, null, null, null, "B")), "C");
        NodeData plain = TestNodes.plain("B", NodeData.NodeType.BATTLE, null, List.of(), List.of(), "", 0, 0,
                null, TerrainType.T1_OCEAN, Set.of(), SceneData.DAY);
        StageData stage = new StageData("t", "chapter_2", "t", Map.of("A", a, "B", plain, "C", plain),
                List.of(new StageData.EdgeData("A", "B"), new StageData.EdgeData("A", "C")),
                "A", List.of("C"), List.of(), List.of(), Set.of(), SceneData.DAY, Set.of(),
                StageData.VictoryObjectives.EMPTY);
        DungeonInstance inst = new DungeonInstance(UUID.randomUUID(), "t", 0);
        inst.setState(DungeonInstance.State.ACTIVE);
        inst.beginNode("A");
        inst.clearNode("A");
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        inst.setBranchChoice(p1, "A", "B");
        inst.setBranchChoice(p2, "A", "C");
        assertTrue(DungeonEntryRules.canEnter(inst, stage, "B", f -> inst.getBranchChoice(p1, f)));
        assertFalse(DungeonEntryRules.canEnter(inst, stage, "C", f -> inst.getBranchChoice(p1, f)));
        assertTrue(DungeonEntryRules.canEnter(inst, stage, "C", f -> inst.getBranchChoice(p2, f)));
        // 未走过分歧门的玩家不能靠讲台直接进后继
        UUID p3 = UUID.randomUUID();
        assertFalse(DungeonEntryRules.canEnter(inst, stage, "B", f -> inst.getBranchChoice(p3, f)));
        // 快照持久化
        DungeonInstance loaded = DungeonInstance.load(inst.save());
        assertEquals("B", loaded.getBranchChoice(p1, "A"));
        assertEquals("C", loaded.getBranchChoice(p2, "A"));
    }
}
