package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.EnemySetData;
import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.npc.ai.FleetHull;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 编队生成器（副本/15）：
 * - 读取 EnemySetData.formation 字符串，映射为 FleetGroup.FormationType
 * - 按队形计算每艘船的出生偏移
 * - 创建 FleetGroup 并将实体登记为成员
 * - 无 formation 字段时回退到原有随机散布逻辑
 */
public final class FormationGenerator {

    /** 领舰离出生点中心的距离，与随机散布半径一致 */
    private static final double LEADER_DISTANCE = 30.0;

    public enum FormationStyle {
        SINGLE_LINE("single_line", FleetGroup.FormationType.SINGLE_LINE),
        DOUBLE_LINE("double_line", FleetGroup.FormationType.DOUBLE_LINE),
        WHEEL("wheel", FleetGroup.FormationType.WHEEL),
        SINGLE_HORIZONTAL("single_horizontal", FleetGroup.FormationType.SINGLE_HORIZONTAL),
        RING("ring", FleetGroup.FormationType.WHEEL),          // alias
        LINE("line", FleetGroup.FormationType.SINGLE_LINE),     // alias
        COLUMN("column", FleetGroup.FormationType.SINGLE_LINE); // alias

        final String jsonKey;
        final FleetGroup.FormationType formationType;

        FormationStyle(String jsonKey, FleetGroup.FormationType formationType) {
            this.jsonKey = jsonKey;
            this.formationType = formationType;
        }

        static FormationStyle fromString(String s) {
            for (FormationStyle style : values()) {
                if (style.jsonKey.equalsIgnoreCase(s)) return style;
            }
            return null;
        }
    }

    public record SpawnPlan(
            EntityType<?> entityType,
            Vec3 position,
            boolean isLeader
    ) {}

    private FormationGenerator() {}

    /**
     * 根据 enemySet 的 formation 字段生成出生计划。
     * 若 enemySet 无 formation，返回空列表（调用方回退随机散布）。
     */
    public static List<SpawnPlan> generateSpawnPlan(EnemySetData enemySet,
                                                     BlockPos center,
                                                     ServerLevel level) {
        if (!enemySet.hasFormation()) return List.of();

        FormationStyle style = FormationStyle.fromString(enemySet.formation());
        if (style == null) {
            PiranPort.LOGGER.warn("Unknown formation '{}' in enemy_set {} — falling back to random spawn",
                    enemySet.formation(), enemySet.enemySetId());
            return List.of();
        }

        var rng = level.getRandom();

        // 收集所有要生成的实体（spawnList + flagship），保持配置顺序
        List<EnemySetData.SpawnEntry> allEntries = new ArrayList<>(enemySet.spawnList());
        if (enemySet.flagship() != null) {
            allEntries.add(enemySet.flagship());
        }
        List<EntityType<?>> ordered = new ArrayList<>();
        for (EnemySetData.SpawnEntry entry : allEntries) {
            EntityType<?> type = resolveEntityType(entry.entity());
            if (type == null) {
                PiranPort.LOGGER.warn("Unknown entity type '{}' in formation generation for enemy_set {}",
                        entry.entity(), enemySet.enemySetId());
                continue;
            }
            for (int i = 0; i < entry.count(); i++) {
                ordered.add(type);
            }
        }
        if (ordered.isEmpty()) return List.of();

        // 领舰槽位固定给旗舰：BossIntroScript 在 INTRO 直接生成 enemySet.flagship()，
        // 它必须占据这个槽位并由调用方跳过，否则同一艘会再以护卫身份生成一次。
        // 旗舰类型不是 deep_ocean_boss 时（如 boss_1_4 为战列舰）也不能退回首船。
        EntityType<?> leaderType = enemySet.flagship() == null ? null
                : resolveEntityType(enemySet.flagship().entity());
        if (leaderType == null || !ordered.contains(leaderType)) {
            leaderType = ordered.get(0);
        }
        List<EntityType<?>> slotTypes = new ArrayList<>(ordered.size());
        slotTypes.add(leaderType);
        boolean leaderRemoved = false;
        for (EntityType<?> type : ordered) {
            if (!leaderRemoved && type == leaderType) {
                leaderRemoved = true;
                continue;
            }
            slotTypes.add(type);
        }
        List<FleetHull> hulls = slotTypes.stream().map(FleetHull::of).toList();

        // 默认朝向：南（+Z），即从出生点平台向战场深处展开
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 centerPos = new Vec3(center.getX() + 0.5, DungeonConstants.SPAWN_Y, center.getZ() + 0.5);
        Vec3 leaderAnchor = centerPos.add(forward.scale(LEADER_DISTANCE));

        FleetGroup tempGroup = new FleetGroup(java.util.UUID.randomUUID());
        tempGroup.setFormation(style.formationType);

        List<SpawnPlan> plans = new ArrayList<>(slotTypes.size());
        for (int slot = 0; slot < slotTypes.size(); slot++) {
            EntityType<?> type = slotTypes.get(slot);
            Vec3 offset = tempGroup.getFormationOffset(slot, hulls, Vec3.ZERO, forward);
            Vec3 spawnPos = leaderAnchor.add(offset);
            // 加少量随机抖动避免完全重叠
            spawnPos = new Vec3(
                    spawnPos.x + (rng.nextDouble() - 0.5) * 1.0,
                    DungeonConstants.SPAWN_Y,
                    spawnPos.z + (rng.nextDouble() - 0.5) * 1.0
            );
            plans.add(new SpawnPlan(type, spawnPos, slot == 0));
        }

        return plans;
    }

    /**
     * 生成出生计划并同时创建 FleetGroup。
     * 返回 null 表示 enemySet 无 formation，调用方应回退。
     */
    public static FormationResult generateWithFleetGroup(EnemySetData enemySet,
                                                         BlockPos center,
                                                         ServerLevel level) {
        List<SpawnPlan> plans = generateSpawnPlan(enemySet, center, level);
        if (plans.isEmpty()) return null;

        FleetGroup group = FleetGroupManager.get(level).createGroup();
        // 队形与出生计划必须用同一式，否则跟随阶段的站位会和出生位置对不上
        group.setFormation(FormationStyle.fromString(enemySet.formation()).formationType);

        return new FormationResult(plans, group);
    }

    // ===== Helpers =====

    @SuppressWarnings("unchecked")
    private static EntityType<?> resolveEntityType(String entityId) {
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl == null) return null;
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        return type.orElse(null);
    }

    public record FormationResult(
            List<SpawnPlan> plans,
            FleetGroup group
    ) {}
}
