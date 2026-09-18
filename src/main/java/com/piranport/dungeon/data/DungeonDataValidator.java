package com.piranport.dungeon.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 提交副本配置前统一检查跨文件引用和数值，不触碰运行中的实例。 */
final class DungeonDataValidator {
    private DungeonDataValidator() {}

    static List<String> validate(Map<String, ChapterData> chapters,
                                 Map<String, StageData> stages,
                                 Map<String, EnemySetData> enemySets) {
        List<String> errors = new ArrayList<>();
        for (ChapterData chapter : chapters.values()) {
            Set<String> seen = new HashSet<>();
            for (String id : chapter.stages()) {
                StageData stage = stages.get(id);
                if (stage == null) errors.add("章节 " + chapter.chapterId() + " 引用不存在的关卡 " + id);
                else if (!chapter.chapterId().equals(stage.chapter())) {
                    errors.add("关卡 " + id + " 的所属章节与章节列表不一致");
                }
                if (!seen.add(id)) errors.add("章节 " + chapter.chapterId() + " 重复引用关卡 " + id);
            }
        }
        for (StageData stage : stages.values()) {
            String context = "关卡 " + stage.stageId();
            if (!chapters.containsKey(stage.chapter())) errors.add(context + " 引用不存在的章节 " + stage.chapter());
            checkNode(stage, stage.startNode(), context + " 起点", errors);
            for (StageData.EdgeData edge : stage.edges()) {
                checkNode(stage, edge.from(), context + " 连线起点", errors);
                checkNode(stage, edge.to(), context + " 连线终点", errors);
            }
            for (String id : stage.bossNodes()) checkNode(stage, id, context + " Boss", errors);
            Set<String> checkpoints = new HashSet<>();
            for (CheckpointData checkpoint : stage.checkpoints()) {
                checkNode(stage, checkpoint.nodeId(), context + " 记录点 " + checkpoint.id(), errors);
                if (checkpoint.id().isBlank() || !checkpoints.add(checkpoint.id())) {
                    errors.add(context + " 记录点 ID 为空或重复: " + checkpoint.id());
                }
            }
            checkRewards(stage.firstClearRewards(), context, errors);
            for (NodeData node : stage.nodes().values()) {
                String nodeContext = context + " 节点 " + node.nodeId();
                if (node.enemies() != null && !enemySets.containsKey(node.enemies())) {
                    errors.add(nodeContext + " 引用不存在的敌人组 " + node.enemies());
                }
                checkRewards(node.rewards(), nodeContext, errors);
                for (NodeData.CostEntry cost : node.cost()) {
                    if (ResourceLocation.tryParse(cost.item()) == null || cost.count() <= 0) {
                        errors.add(nodeContext + " 过路费物品 ID 或数量无效");
                    }
                }
            }
        }
        for (EnemySetData set : enemySets.values()) {
            List<EnemySetData.SpawnEntry> spawns = new ArrayList<>(set.spawnList());
            if (set.flagship() != null) spawns.add(set.flagship());
            for (EnemySetData.SpawnEntry spawn : spawns) {
                if (ResourceLocation.tryParse(spawn.entity()) == null || spawn.count() <= 0) {
                    errors.add("敌人组 " + set.enemySetId() + " 实体 ID 或数量无效");
                }
            }
        }
        return List.copyOf(errors);
    }

    private static void checkNode(StageData stage, String id, String context, List<String> errors) {
        if (!stage.nodes().containsKey(id)) errors.add(context + " 引用不存在的节点 " + id);
    }

    private static void checkRewards(List<NodeData.RewardEntry> rewards, String context, List<String> errors) {
        for (NodeData.RewardEntry reward : rewards) {
            if (ResourceLocation.tryParse(reward.item()) == null || reward.count() <= 0
                    || !Float.isFinite(reward.chance()) || reward.chance() < 0 || reward.chance() > 1) {
                errors.add(context + " 奖励物品 ID、数量或概率无效");
            }
        }
    }
}
