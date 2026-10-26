package com.piranport.dungeon.key;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.SceneData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.TerrainType;
import com.piranport.dungeon.data.TestNodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 章节钥匙「打到本章全通才失效」的纯逻辑判定（副本/17「换关」）。
 *
 * <p>只测不依赖 Minecraft 注册表的静态方法；ItemStack 读写由游戏内验证覆盖。</p>
 */
class ChapterKeyProgressTest {

    private static final List<String> CHAPTER_STAGES = List.of("1-1", "1-2", "1-3");

    @Test
    void chapterIsNotCompleteUntilEveryStageIsCleared() {
        assertFalse(ChapterKeyProgress.allStagesCleared(CHAPTER_STAGES, Set.of()));
        assertFalse(ChapterKeyProgress.allStagesCleared(CHAPTER_STAGES, Set.of("1-1", "1-2")));
        assertTrue(ChapterKeyProgress.allStagesCleared(CHAPTER_STAGES, Set.of("1-1", "1-2", "1-3")));
    }

    @Test
    void extraClearedStagesFromOtherChaptersDoNotAffectThisChapter() {
        assertTrue(ChapterKeyProgress.allStagesCleared(CHAPTER_STAGES,
                Set.of("1-1", "1-2", "1-3", "2-1")));
        assertFalse(ChapterKeyProgress.allStagesCleared(CHAPTER_STAGES,
                Set.of("2-1", "2-2", "2-3")));
    }

    @Test
    void missingChapterDataNeverCountsAsComplete() {
        // 数据包加载失败时宁可继续可用，也不要把钥匙永久判死。
        assertFalse(ChapterKeyProgress.allStagesCleared(List.of(), Set.of()));
        assertFalse(ChapterKeyProgress.allStagesCleared(null, Set.of("1-1")));
    }

    @Test
    void onlyUnclearedStagesOfThisChapterAreSelectable() {
        Set<String> cleared = Set.of("1-1");
        assertTrue(ChapterKeyProgress.isStageSelectable(CHAPTER_STAGES, cleared, "1-2"));
        assertFalse(ChapterKeyProgress.isStageSelectable(CHAPTER_STAGES, cleared, "1-1"));
        assertFalse(ChapterKeyProgress.isStageSelectable(CHAPTER_STAGES, cleared, "2-1"));
        assertFalse(ChapterKeyProgress.isStageSelectable(CHAPTER_STAGES, cleared, null));
    }

    private static NodeData node(String id) {
        return TestNodes.plain(id, NodeData.NodeType.BATTLE, null, List.of(), List.of(), "",
                0, 0, null, TerrainType.T1_OCEAN, Set.of(), SceneData.DAY);
    }

    private static StageData stage(Map<String, NodeData> nodes, List<StageData.EdgeData> edges,
                                   List<String> bossNodes) {
        return new StageData("s", "chapter_1", "s", nodes, edges, "A", bossNodes,
                List.of(), List.of(), Set.of(), SceneData.DAY, Set.of(),
                StageData.VictoryObjectives.EMPTY);
    }

    @Test
    void legacyProgressRecoveryUsesBossNodesForBossStages() {
        StageData bossStage = stage(Map.of("A", node("A"), "B", node("B")),
                List.of(new StageData.EdgeData("A", "B")), List.of("B"));
        assertFalse(ChapterKeyProgress.isStageClearedByProgress(bossStage, Set.of("A")));
        assertTrue(ChapterKeyProgress.isStageClearedByProgress(bossStage, Set.of("A", "B")));
    }

    @Test
    void legacyProgressRecoveryUsesTerminalNodesForBosslessStages() {
        StageData activity = stage(Map.of("A", node("A"), "B", node("B")),
                List.of(new StageData.EdgeData("A", "B")), List.of());
        assertFalse(ChapterKeyProgress.isStageClearedByProgress(activity, Set.of("A")));
        assertTrue(ChapterKeyProgress.isStageClearedByProgress(activity, Set.of("A", "B")));
    }

    @Test
    void legacyProgressRecoveryNeedsAStageAndSomeProgress() {
        assertFalse(ChapterKeyProgress.isStageClearedByProgress(null, Set.of("A")));
        assertFalse(ChapterKeyProgress.isStageClearedByProgress(
                stage(Map.of("A", node("A")), List.of(), List.of("A")), Set.of()));
    }

    @Test
    void awaitingSelectionOnlyWhenClearedStageHasNoInstanceAndChapterIsNotComplete() {
        // 打完 1-1、实例已解绑、本章还有关：等待选关。
        assertTrue(ChapterKeyProgress.awaitsStageSelection(true, false, true, false));
        // 仍绑着实例（例如战斗/建造中）不进入选关态。
        assertFalse(ChapterKeyProgress.awaitsStageSelection(true, true, true, false));
        // 当前关还没通关：不是选关态。
        assertFalse(ChapterKeyProgress.awaitsStageSelection(true, false, false, false));
        // 本章全通：钥匙失效，不再提供选关。
        assertFalse(ChapterKeyProgress.awaitsStageSelection(true, false, true, true));
        // 旧版通用钥匙不参与章节选关。
        assertFalse(ChapterKeyProgress.awaitsStageSelection(false, false, true, false));
    }
}
