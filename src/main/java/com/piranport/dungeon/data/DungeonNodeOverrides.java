package com.piranport.dungeon.data;

import com.piranport.terminal.TerminalParameters;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 副本节点怪物编成与 Boss 数值的调试终端覆盖（《调试/02》）。
 *
 * <p>终端只支持 int/double/bool，不能直接保存“敌人组 ID”。因此“换怪”做成
 * 节点级、按实体类型的数量覆盖：每个战斗/Boss 节点暴露 8 种常规深海舰的
 * 数量，设为 0 即移除、设为 N 即改为 N 只。旗舰不参与数量覆盖，所以 Boss
 * 是谁保持不变。</p>
 *
 * <p>参数键由本类统一生成，终端目录和刷怪读取必须走同一套方法，避免出现
 * “终端能改、游戏不生效”的死参数。键形如：
 * {@code dungeon_node.<关卡>/<节点>.spawn_deep_ocean_destroyer}。</p>
 */
public final class DungeonNodeOverrides {
    /** 任何节点都可以额外加入的常规深海舰。 */
    public static final List<String> STANDARD_TYPES = List.of(
            "piranport:deep_ocean_destroyer",
            "piranport:deep_ocean_light_cruiser",
            "piranport:deep_ocean_heavy_cruiser",
            "piranport:deep_ocean_battle_cruiser",
            "piranport:deep_ocean_battleship",
            "piranport:deep_ocean_light_carrier",
            "piranport:deep_ocean_carrier",
            "piranport:deep_ocean_submarine");

    /** 单舰种数量上限。8 种全拉满也只有 64 条，避免调试时一个节点刷爆服务端。 */
    public static final int SPAWN_MAX = 8;

    private DungeonNodeOverrides() {}

    public static String target(String stageId, String nodeId) {
        return stageId + "/" + nodeId;
    }

    /** 目录与读取侧共用的唯一键生成入口。 */
    public static String key(String stageId, String nodeId, String property) {
        return "dungeon_node." + target(stageId, nodeId) + "." + property;
    }

    public static String spawnProperty(String entityId) {
        int colon = entityId.indexOf(':');
        String path = colon >= 0 ? entityId.substring(colon + 1) : entityId;
        return "spawn_" + path;
    }

    /** 聚合基础编成：同种船可能出现多条 SpawnEntry，终端按“总数”展示。 */
    public static Map<String, Integer> aggregateSpawnCounts(EnemySetData enemySet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (enemySet == null) return counts;
        for (EnemySetData.SpawnEntry entry : enemySet.spawnList()) {
            counts.merge(entry.entity(), entry.count(), Integer::sum);
        }
        return counts;
    }

    /** 终端为该节点展示的实体类型：基础编成里出现过的 + 8 种常规深海舰。 */
    public static List<String> exposedTypes(EnemySetData enemySet) {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        if (enemySet != null) {
            for (EnemySetData.SpawnEntry entry : enemySet.spawnList()) {
                types.add(entry.entity());
            }
        }
        types.addAll(STANDARD_TYPES);
        return List.copyOf(types);
    }

    /**
     * 应用终端数量覆盖后的敌人组。旗舰与队形原样保留；没有任何覆盖时直接返回
     * 原对象，避免不必要的对象分配和编队顺序变化。
     */
    public static EnemySetData effective(String stageId, String nodeId, EnemySetData base) {
        if (base == null) return null;
        Map<String, Integer> baseCounts = aggregateSpawnCounts(base);
        // 先保持基础编成的舰种顺序，再追加“本来没有、但可以在终端加进来”的舰种。
        LinkedHashSet<String> ordered = new LinkedHashSet<>(baseCounts.keySet());
        ordered.addAll(STANDARD_TYPES);

        List<EnemySetData.SpawnEntry> spawnList = new ArrayList<>();
        boolean changed = false;
        for (String entity : ordered) {
            int baseCount = baseCounts.getOrDefault(entity, 0);
            int count = TerminalParameters.getInt(key(stageId, nodeId, spawnProperty(entity)), baseCount);
            // 只钳制被策划改过的值；没有覆盖时保留数据包原值（未来基础编成可能超过 SPAWN_MAX）。
            if (count != baseCount) {
                count = Math.max(0, Math.min(SPAWN_MAX, count));
                changed = true;
            }
            if (count > 0) spawnList.add(new EnemySetData.SpawnEntry(entity, count));
        }
        if (!changed) return base;
        return new EnemySetData(base.enemySetId(), List.copyOf(spawnList),
                base.flagship(), base.formation());
    }

    public static double bossMultiplier(String stageId, String nodeId, String property,
                                        double fallback) {
        return TerminalParameters.getDouble(key(stageId, nodeId, property), fallback);
    }

    /**
     * Boss 数值只在旗舰生成时应用一次，和 difficulty_scale 的时机一致；
     * 只改血量、伤害、护甲、移速，不碰旗舰的实体类型。
     *
     * <p>注意：终端「深海舰」分类里的同舰种全局属性每 tick 生效。如果给旗舰所在
     * 的舰种也设置了全局属性，全局值会覆盖这里的 Boss 倍率。调试 Boss 专属数值时
     * 不要同时改该舰种的全局属性。</p>
     */
    public static void applyBossStats(Entity entity, String stageId, String nodeId) {
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide()) return;
        double healthMultiplier = bossMultiplier(stageId, nodeId, "boss_health_multiplier", 1.0);
        double damageMultiplier = bossMultiplier(stageId, nodeId, "boss_damage_multiplier", 1.0);
        double armorBonus = bossMultiplier(stageId, nodeId, "boss_armor_bonus", 0.0);
        double speedMultiplier = bossMultiplier(stageId, nodeId, "boss_speed_multiplier", 1.0);

        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && Math.abs(healthMultiplier - 1.0) > 1.0e-6) {
            health.setBaseValue(Math.max(1.0, health.getBaseValue() * healthMultiplier));
            living.setHealth(living.getMaxHealth());
        }
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        if (armor != null && Math.abs(armorBonus) > 1.0e-6) {
            armor.setBaseValue(Math.max(0.0, armor.getBaseValue() + armorBonus));
        }
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && Math.abs(speedMultiplier - 1.0) > 1.0e-6) {
            speed.setBaseValue(Math.max(0.0, speed.getBaseValue() * speedMultiplier));
        }
        if (Math.abs(damageMultiplier - 1.0) > 1.0e-6) {
            String scaleKey = com.piranport.dungeon.event.DungeonDamageScaleHandler.DAMAGE_SCALE_KEY;
            double current = entity.getPersistentData().contains(scaleKey)
                    ? entity.getPersistentData().getDouble(scaleKey) : 1.0;
            com.piranport.dungeon.event.DungeonDamageScaleHandler.setDamageScale(
                    entity, current * damageMultiplier);
        }
    }
}
