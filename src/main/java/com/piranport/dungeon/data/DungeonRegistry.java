package com.piranport.dungeon.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Singleton registry holding all loaded dungeon configuration data.
 * Populated by {@link DungeonDataLoader} on datapack reload.
 */
public final class DungeonRegistry {
    public static final DungeonRegistry INSTANCE = new DungeonRegistry();

    private record Snapshot(Map<String, ChapterData> chapters,
                            Map<String, StageData> stages,
                            Map<String, EnemySetData> enemySets,
                            List<ChapterData> sortedChapters) {}

    // 一次发布完整快照，读者不会看到新章节搭配旧关卡的中间状态。
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of(), Map.of(), List.of());

    private DungeonRegistry() {}

    public void load(Map<String, ChapterData> chapters,
                     Map<String, StageData> stages,
                     Map<String, EnemySetData> enemySets) {
        List<ChapterData> sorted = new ArrayList<>(chapters.values());
        sorted.sort(Comparator.comparingInt(ChapterData::sortOrder));
        Snapshot next = new Snapshot(Map.copyOf(chapters), Map.copyOf(stages),
                Map.copyOf(enemySets), List.copyOf(sorted));
        snapshot = next;
    }

    public ChapterData getChapter(String chapterId) {
        return snapshot.chapters().get(chapterId);
    }

    public StageData getStage(String stageId) {
        return snapshot.stages().get(stageId);
    }

    public EnemySetData getEnemySet(String enemySetId) {
        return snapshot.enemySets().get(enemySetId);
    }

    public List<ChapterData> getSortedChapters() {
        return snapshot.sortedChapters();
    }

    public Map<String, StageData> getAllStages() {
        return snapshot.stages();
    }

    public boolean hasStage(String stageId) {
        return snapshot.stages().containsKey(stageId);
    }
}
