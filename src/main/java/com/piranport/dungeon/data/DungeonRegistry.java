package com.piranport.dungeon.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton registry holding all loaded dungeon configuration data.
 * Populated by {@link DungeonDataLoader} on datapack reload.
 */
public final class DungeonRegistry {
    public static final DungeonRegistry INSTANCE = new DungeonRegistry();

    private Map<String, ChapterData> chapters = Map.of();
    private Map<String, StageData> stages = Map.of();
    private Map<String, EnemySetData> enemySets = Map.of();
    private List<ChapterData> sortedChapters = List.of();

    private DungeonRegistry() {}

    public void load(Map<String, ChapterData> chapters,
                     Map<String, StageData> stages,
                     Map<String, EnemySetData> enemySets) {
        this.chapters = Map.copyOf(chapters);
        this.stages = Map.copyOf(stages);
        this.enemySets = Map.copyOf(enemySets);

        List<ChapterData> sorted = new ArrayList<>(chapters.values());
        sorted.sort(Comparator.comparingInt(ChapterData::sortOrder));
        this.sortedChapters = List.copyOf(sorted);
    }

    public ChapterData getChapter(String chapterId) {
        return chapters.get(chapterId);
    }

    public StageData getStage(String stageId) {
        return stages.get(stageId);
    }

    public EnemySetData getEnemySet(String enemySetId) {
        return enemySets.get(enemySetId);
    }

    public List<ChapterData> getSortedChapters() {
        return sortedChapters;
    }

    public Map<String, StageData> getAllStages() {
        return stages;
    }

    public boolean hasStage(String stageId) {
        return stages.containsKey(stageId);
    }
}
