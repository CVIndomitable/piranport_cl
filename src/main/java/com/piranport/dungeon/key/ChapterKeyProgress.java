package com.piranport.dungeon.key;

import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 章节钥匙的「本章已通关关卡」记录（副本/17「换关」）。
 *
 * <p>WHY：通关一关后实例会被清理，完成度若只存在实例里就会随实例一起消失，
 * 钥匙随即被判为失效。这里把完成度记在钥匙的
 * {@link ModDataComponents#DUNGEON_CLEARED_STAGES} 上：
 * <ul>
 *   <li>本关通关 → 加入集合、解绑实例 → 钥匙回到「等待选关」；</li>
 *   <li>本章全部关卡都在集合里 → 钥匙才算用完（INVALID / 「钥匙已失效」）；</li>
 *   <li>集合里的关卡不能再被同一把钥匙选中，避免重复刷奖励。</li>
 * </ul>
 *
 * <p>前三个判定刻意做成不依赖 Minecraft 注册表的纯函数，便于单测；
 * 带 {@link ItemStack} 的重载只是读写组件。</p>
 */
public final class ChapterKeyProgress {

    private ChapterKeyProgress() {}

    /**
     * 本章是否所有关卡都已通关。
     *
     * <p>章节数据缺失（空列表）时返回 false：宁可让钥匙继续保持可用，
     * 也不要因为一次数据包加载失败把玩家钥匙永久判死。</p>
     */
    public static boolean allStagesCleared(List<String> chapterStages, Set<String> clearedStages) {
        if (chapterStages == null || chapterStages.isEmpty()) return false;
        if (clearedStages == null || clearedStages.isEmpty()) return false;
        return clearedStages.containsAll(chapterStages);
    }

    /** 目标关是否可被章节钥匙切换：必须属于本章，且尚未通关。 */
    public static boolean isStageSelectable(List<String> chapterStages, Set<String> clearedStages,
                                            String stageId) {
        if (chapterStages == null || stageId == null) return false;
        if (!chapterStages.contains(stageId)) return false;
        return clearedStages == null || !clearedStages.contains(stageId);
    }

    /**
     * 章节钥匙是否处于「本关已通关、等待选下一关」。
     *
     * <p>必须是章节钥匙、当前没有绑定实例、当前关已通关，且本章尚未全通。
     * 本章全通时钥匙应保持 INVALID（已失效），不再提供选关。</p>
     */
    public static boolean awaitsStageSelection(boolean chapterKey, boolean hasInstance,
                                               boolean currentStageCleared, boolean chapterFullyCleared) {
        return chapterKey && !hasInstance && currentStageCleared && !chapterFullyCleared;
    }

    /**
     * 旧存档修复：钥匙的进度已满足通关条件、但对应实例已不存在时，认定本关已通关。
     *
     * <p>旧版本打完一关会清掉实例、却不在钥匙上留下记录，钥匙因此永久 INVALID。
     * 判定口径与 {@code DungeonEventHandler.onNodeCompleted} 一致：
     * 有 Boss 的关卡看 Boss 是否全清；无 Boss 的活动关看终点节点（无出边）是否全清。</p>
     */
    public static boolean isStageClearedByProgress(StageData stage, Set<String> clearedNodes) {
        if (stage == null || clearedNodes == null || clearedNodes.isEmpty()) return false;
        if (stage.bossNodes().isEmpty()) {
            List<String> terminals = stage.nodes().keySet().stream()
                    .filter(id -> stage.edges().stream().noneMatch(e -> e.from().equals(id)))
                    .toList();
            return !terminals.isEmpty() && clearedNodes.containsAll(terminals);
        }
        return clearedNodes.containsAll(stage.bossNodes());
    }

    // ===== ItemStack 读写 =====

    public static Set<String> clearedStages(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DUNGEON_CLEARED_STAGES.get(), Set.of());
    }

    public static boolean isStageCleared(ItemStack stack, String stageId) {
        return stageId != null && !stageId.isEmpty() && clearedStages(stack).contains(stageId);
    }

    /** 把一关记为已通关；重复写入不改变集合。 */
    public static void markStageCleared(ItemStack stack, String stageId) {
        if (stageId == null || stageId.isEmpty()) return;
        if (isStageCleared(stack, stageId)) return;
        Set<String> cleared = new LinkedHashSet<>(clearedStages(stack));
        cleared.add(stageId);
        stack.set(ModDataComponents.DUNGEON_CLEARED_STAGES.get(), Set.copyOf(cleared));
    }

    /** 按钥匙所属章节查注册表，判断本章是否已全通。旧版通用钥匙恒为 false。 */
    public static boolean isChapterFullyCleared(ItemStack stack) {
        int chapterNumber = DungeonKeyItem.getChapterNumber(stack);
        if (chapterNumber < 1) return false;
        ChapterData chapter = DungeonRegistry.INSTANCE.getChapter("chapter_" + chapterNumber);
        if (chapter == null) return false;
        return allStagesCleared(chapter.stages(), clearedStages(stack));
    }
}
