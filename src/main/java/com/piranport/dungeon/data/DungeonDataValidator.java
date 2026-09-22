package com.piranport.dungeon.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 提交副本配置前统一检查跨文件引用和数值，不触碰运行中的实例。 */
final class DungeonDataValidator {
    private DungeonDataValidator() {}

    /**
     * 注册表校验只在已 bootstrap 的 JVM 里做。
     *
     * 为什么必须判定：{@code BuiltInRegistries} 的类初始化会调
     * {@code internalRegister → Bootstrap.checkBootstrapCalled}，未 bootstrap 时直接抛
     * {@code ExceptionInInitializerError}（且类此后永久 ERRONEOUS，try/catch 无法挽救）——
     * 单元测试 JVM 从未 bootstrap，一碰就炸。
     *
     * 为什么可以跳过：服务端 {@code Bootstrap.bootStrap()} 在 {@code Main.main} 里
     * 严格早于服务器构造，而副本数据包的 reload 在服务器构造之后才发生，
     * 所以正式游戏跑了这段校验、单测不会。测试环境无法自建 bootstrap——
     * {@code Bootstrap.bootStrap()} 需要 FML 的 LoadingModList，裸 JUnit JVM 里同样会炸。
     *
     * 判定手段：MC 没暴露 isBootstrapped 的 getter，但 {@code checkBootstrapCalled}
     * 恰好就是「未 bootstrap 即抛」，拿它当探针即可——这里 catch 的是探针自身的
     * 异常，不是 BuiltInRegistries 的 ExceptionInInitializerError（那个救不回来）。
     */
    private static boolean registriesAvailable() {
        try {
            Bootstrap.checkBootstrapCalled(() -> "dungeon validator registry probe");
            return true;
        } catch (RuntimeException notBootstrapped) {
            return false;
        }
    }

    private static boolean isItemRegistered(ResourceLocation id) {
        return !registriesAvailable() || BuiltInRegistries.ITEM.containsKey(id);
    }

    private static boolean isEntityTypeRegistered(ResourceLocation id) {
        return !registriesAvailable() || BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }

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
                    ResourceLocation costId = ResourceLocation.tryParse(cost.item());
                    if (costId == null || cost.count() <= 0) {
                        errors.add(nodeContext + " 过路费物品 ID 或数量无效");
                    } else if (!isItemRegistered(costId)) {
                        // 同 checkRewards：过路费是玩家前进的必经扣费，未注册物品会导致节点永远无法通过。
                        errors.add(nodeContext + " 过路费物品未注册: " + cost.item());
                    }
                }
            }
        }
        for (EnemySetData set : enemySets.values()) {
            List<EnemySetData.SpawnEntry> spawns = new ArrayList<>(set.spawnList());
            if (set.flagship() != null) spawns.add(set.flagship());
            for (EnemySetData.SpawnEntry spawn : spawns) {
                ResourceLocation entityId = ResourceLocation.tryParse(spawn.entity());
                if (entityId == null || spawn.count() <= 0) {
                    errors.add("敌人组 " + set.enemySetId() + " 实体 ID 或数量无效");
                } else if (!isEntityTypeRegistered(entityId)) {
                    // 同 checkRewards：未注册实体虽不会静默失败（NodeBattleField 会打 WARN 并生成恢复传送门），
                    // 但节点实际没有战斗内容，等于关卡被跳过——玩家不会报错，只会觉得这关莫名其妙就过了。
                    errors.add("敌人组 " + set.enemySetId() + " 实体未注册: " + spawn.entity());
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
            ResourceLocation id = ResourceLocation.tryParse(reward.item());
            if (id == null || reward.count() <= 0
                    || !Float.isFinite(reward.chance()) || reward.chance() < 0 || reward.chance() > 1) {
                errors.add(context + " 奖励物品 ID、数量或概率无效");
            } else if (!isItemRegistered(id)) {
                // 决策/副本/21 §4.4：只校验 ID 语法会让"语法合法但从未注册"的奖励静默丢失——
                // 加载期拦下，比等玩家通关后拿不到东西、且只有一条容易被忽略的 WARN 要好。
                errors.add(context + " 奖励物品未注册: " + reward.item());
            }
        }
    }
}
