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

    /**
     * 回归：战斗中（current 未清）点"从头开始"必须放行。
     *
     * <p>旧实现复用 canEnter，而 canEnter 在 {@code current != null && !cleared.contains(current)}
     * 时拒绝一切请求（阻止战斗中直接跳到下一个节点）。玩家阵亡/掉线时 current 恰好未清，
     * 于是"回起点"也被一并拒掉，按钮静默失效——这就是本 bug。</p>
     *
     * <p>注意：canEnter 对<b>已经进入过</b>的节点会在 hasEnteredNode 处短路返回 true，
     * 所以不能拿 startNode 去断言 canEnter==false（那永远不成立）。真实差异出现在
     * COMPLETED 状态：canEnter 对新节点拒绝，而仅传送的回起点必须放行。</p>
     */
    @Test
    void restartFromBeginningIsAllowedWhileCurrentNodeIsUncleared() {
        DungeonInstance instance = instance(0);
        instance.beginNode("A");
        instance.clearNode("A");
        instance.beginNode("B");
        instance.setState(DungeonInstance.State.SUSPENDED);
        // 复现 bug 触发条件：current="B" 已进入、未清。
        assertEquals("B", instance.getCurrentNode());
        assertFalse(instance.getClearedNodes().contains("B"));
        // 推进规则确实拒绝从 B 跳到别处（B 未清，不能解锁任何后继节点）。
        // 注意不能查 "B" 本身——canEnter 对已进入节点会短路放行，那测不到 current 守卫。
        assertFalse(DungeonEntryRules.canEnter(instance, stage, "C_UNKNOWN"));
        // 但"仅传送回起点"必须放行——这正是旧实现漏掉的分支。
        assertTrue(DungeonEntryRules.canRestartFromBeginning(instance, stage));
    }

    /**
     * 回起点与推进规则的真实分界：COMPLETED 实例。
     * canEnter 拒绝进入未生成过的新节点，但玩家应能回起点翻找遗漏的宝箱。
     */
    @Test
    void restartFromBeginningBypassesCompletedStateWhileCanEnterDoesNot() {
        DungeonInstance instance = instance(0);
        instance.beginNode("A");
        instance.clearNode("A");
        instance.setState(DungeonInstance.State.COMPLETED);
        // 推进规则：通关后不能再进新节点（B 从未进入过）。
        assertFalse(DungeonEntryRules.canEnter(instance, stage, "B"));
        // 仅传送：通关后仍可回起点找漏掉的宝箱。
        assertTrue(DungeonEntryRules.canRestartFromBeginning(instance, stage));
    }

    /**
     * 不变量：回起点只读状态，绝不产生任何推进收益。
     * 本测试锁定"仅传送"语义——判定通过后 enteredNodes / clearedNodes / currentNode 均不变。
     */
    @Test
    void restartFromBeginningDecisionMutatesNoProgressionState() {
        DungeonInstance instance = instance(0);
        instance.beginNode("A");
        instance.clearNode("A");
        instance.beginNode("B");
        var enteredBefore = new java.util.HashSet<>(instance.getClearedNodes());
        String currentBefore = instance.getCurrentNode();
        assertTrue(DungeonEntryRules.canRestartFromBeginning(instance, stage));
        // 判定本身不得改动任何进度字段。
        assertEquals(currentBefore, instance.getCurrentNode());
        assertEquals(enteredBefore, instance.getClearedNodes());
        assertFalse(instance.getClearedNodes().contains("B"));
    }

    /** 回起点在任何可玩状态下都放行，包括 COMPLETED（用于回去找遗漏宝箱）。 */
    @Test
    void restartFromBeginningIsAllowedForAllPlayableStates() {
        DungeonInstance instance = instance(0);
        for (DungeonInstance.State state : new DungeonInstance.State[]{
                DungeonInstance.State.ACTIVE, DungeonInstance.State.SUSPENDED,
                DungeonInstance.State.COMPLETED}) {
            instance.setState(state);
            assertTrue(DungeonEntryRules.canRestartFromBeginning(instance, stage), "state=" + state);
        }
    }

    /** 实例正在创建或清理时不能传送：数据未就绪/即将销毁。 */
    @Test
    void restartFromBeginningIsRefusedWhileCreatingOrCleaningUp() {
        DungeonInstance instance = instance(0);
        instance.setState(DungeonInstance.State.CREATING);
        assertFalse(DungeonEntryRules.canRestartFromBeginning(instance, stage));
        instance.setState(DungeonInstance.State.CLEANUP);
        assertFalse(DungeonEntryRules.canRestartFromBeginning(instance, stage));
    }

    /** 起点节点必须真实存在，且没有实例可回（未绑定钥匙）时拒绝。 */
    @Test
    void restartFromBeginningRequiresARealInstanceAndStartNode() {
        assertFalse(DungeonEntryRules.canRestartFromBeginning(null, stage));
        assertFalse(DungeonEntryRules.canRestartFromBeginning(instance(0), null));
        StageData broken = new StageData("test", "test", "test", Map.of("B", node),
                List.of(), "A", List.of(), List.of(), List.of(checkpoint), Set.of(),
                SceneData.DAY, Set.of(), StageData.VictoryObjectives.EMPTY);
        assertFalse(DungeonEntryRules.canRestartFromBeginning(instance(0), broken));
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
