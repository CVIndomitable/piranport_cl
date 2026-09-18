package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.EnemySetData;
import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.ai.FleetGroupManager;
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

        List<SpawnPlan> plans = new ArrayList<>();
        var rng = level.getRandom();

        // 收集所有要生成的实体（spawnList + flagship）
        List<EnemySetData.SpawnEntry> allEntries = new ArrayList<>(enemySet.spawnList());
        if (enemySet.flagship() != null) {
            allEntries.add(enemySet.flagship());
        }

        // 扁平化为单个实体列表，第一艘默认领舰
        List<EntityTypeEntry> flat = new ArrayList<>();
        for (EnemySetData.SpawnEntry entry : allEntries) {
            EntityType<?> type = resolveEntityType(entry.entity());
            if (type == null) {
                PiranPort.LOGGER.warn("Unknown entity type '{}' in formation generation for enemy_set {}",
                        entry.entity(), enemySet.enemySetId());
                continue;
            }
            for (int i = 0; i < entry.count(); i++) {
                flat.add(new EntityTypeEntry(type, i == 0 && flat.isEmpty()));
            }
        }

        if (flat.isEmpty()) return List.of();

        // 默认朝向：南（+Z），即从出生点平台向战场深处展开
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 centerPos = new Vec3(center.getX() + 0.5, DungeonConstants.SPAWN_Y, center.getZ() + 0.5);

        // 根据队形计算每艘船的偏移
        FleetGroup tempGroup = new FleetGroup(java.util.UUID.randomUUID());
        tempGroup.setFormation(style.formationType);
        double leaderDistance = 30.0; // 领舰离出生点中心的距离，与随机散布半径一致
        for (int i = 0; i < flat.size(); i++) {
            EntityTypeEntry ete = flat.get(i);
            Vec3 offset;
            if (i == 0) {
                // 领舰放在中心前方 leaderDistance 处
                offset = forward.scale(leaderDistance);
            } else {
                // 编队偏移相对领舰，再叠加上领舰的世界偏移
                Vec3 formationOffset = tempGroup.getFormationOffset(i, flat.size(), Vec3.ZERO, forward);
                offset = forward.scale(leaderDistance).add(formationOffset);
            }

            Vec3 spawnPos = centerPos.add(offset);
            // 加少量随机抖动避免完全重叠
            spawnPos = new Vec3(
                    spawnPos.x + (rng.nextDouble() - 0.5) * 1.0,
                    DungeonConstants.SPAWN_Y,
                    spawnPos.z + (rng.nextDouble() - 0.5) * 1.0
            );

            plans.add(new SpawnPlan(ete.entityType(), spawnPos, ete.isLeader()));
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
        // 默认队形：单纵阵（可由策划在 JSON formation 字段中指定，此处先统一为默认）
        group.setFormation(FleetGroup.FormationType.SINGLE_LINE);

        return new FormationResult(plans, group);
    }

    // ===== Helpers =====

    private record EntityTypeEntry(EntityType<?> entityType, boolean isLeader) {}

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
