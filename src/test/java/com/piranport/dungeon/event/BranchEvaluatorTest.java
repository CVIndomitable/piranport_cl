package com.piranport.dungeon.event;

import com.piranport.dungeon.data.*;
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

    @Test
    void firstMatchingRuleWinsInOrder() {
        List<BranchRule> rules = List.of(
                new BranchRule("submarine", null, null, null, null, "S"),
                new BranchRule("small", null, null, null, null, "B"),
                new BranchRule(null, null, null, null, null, "C"));
        var d = BranchEvaluator.evaluate(rules, "D", ctx("small", null, 0.5));
        assertEquals("B", d.target());
        assertFalse(d.diverted());
        assertEquals(2, d.results().size()); // 命中后不再判定
        assertFalse(d.results().get(0).matched());
    }

    @Test
    void defaultIsFallbackAndMarksDiverted() {
        List<BranchRule> rules = List.of(new BranchRule("large", null, null, null, null, "B"));
        var d = BranchEvaluator.evaluate(rules, "D", ctx("small", null, 0.5));
        assertEquals("D", d.target());
        assertTrue(d.diverted());
        assertFalse(d.results().get(0).conditions().get(0).met());
        // 未变身也走 default
        assertEquals("D", BranchEvaluator.evaluate(rules, "D", ctx(null, null, 0.5)).target());
    }

    @Test
    void allConditionsInARuleAreAnded() {
        BranchRule r = new BranchRule("medium", null, null, "piranport:hentai_trophy", null, "B");
        assertEquals("B", BranchEvaluator.evaluate(List.of(r), "D",
                ctx("MEDIUM", "piranport:hentai_trophy", 0)).target());
        assertEquals("D", BranchEvaluator.evaluate(List.of(r), "D",
                ctx("medium", "minecraft:stone", 0)).target());
    }

    @Test
    void escortsAndRoleUseUndevelopedDefaults() {
        // 随从未开发：escorts=0，role=无
        assertEquals("B", BranchEvaluator.evaluate(
                List.of(new BranchRule(null, "<=0", null, null, null, "B")), "D", ctx(null, null, 0)).target());
        assertEquals("D", BranchEvaluator.evaluate(
                List.of(new BranchRule(null, ">=1", null, null, null, "B")), "D", ctx(null, null, 0)).target());
        assertEquals("D", BranchEvaluator.evaluate(
                List.of(new BranchRule(null, null, "escort_carrier", null, null, "B")), "D", ctx(null, null, 0)).target());
    }

    @Test
    void chanceUsesInjectedRandom() {
        BranchRule r = new BranchRule(null, null, null, null, 0.3, "B");
        assertEquals("B", BranchEvaluator.evaluate(List.of(r), "D", ctx(null, null, 0.29)).target());
        assertEquals("D", BranchEvaluator.evaluate(List.of(r), "D", ctx(null, null, 0.3)).target());
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
        NodeData a = new NodeData("A", NodeData.NodeType.BATTLE, null, List.of(), List.of(), "", 0, 0,
                null, TerrainType.T1_OCEAN, Set.of(), SceneData.DAY, 0.0, 1,
                List.of(new BranchRule("small", null, null, null, null, "B")), "C");
        NodeData plain = new NodeData("B", NodeData.NodeType.BATTLE, null, List.of(), List.of(), "", 0, 0,
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
        // 快照持久化
        DungeonInstance loaded = DungeonInstance.load(inst.save());
        assertEquals("B", loaded.getBranchChoice(p1, "A"));
        assertEquals("C", loaded.getBranchChoice(p2, "A"));
    }
}
