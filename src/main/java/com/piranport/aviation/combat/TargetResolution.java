package com.piranport.aviation.combat;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.CombatTargeting;
import com.piranport.config.ModEquipmentConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 攻击策略共用的目标解析骨架。
 *
 * <p>手动火控锁定优先，其次自动索敌；SPREAD 模式取最近目标。敌对判据统一走
 * {@link CombatTargeting#isHostileTarget}，不在各策略里另写一套。
 */
public final class TargetResolution {
    private TargetResolution() {}

    public static boolean isAirborneTarget(Entity entity) {
        return entity instanceof com.piranport.entity.AircraftEntity;
    }

    @Nullable
    public static <T extends Entity> T resolveLocked(AircraftAttackContext ctx, Player owner,
                                                      Predicate<Entity> predicate, Class<T> castTo) {
        ServerLevel level = ctx.serverLevel();
        if (level == null) return null;
        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (locks.isEmpty()) return null;
        if (ctx.attackMode() == com.piranport.component.AircraftAttackMode.SPREAD) {
            return locks.stream()
                    .map(level::getEntity)
                    .filter(entity -> entity != null && entity.isAlive() && predicate.test(entity))
                    .map(castTo::cast)
                    .min(Comparator.comparingDouble(ctx::distanceToEntity))
                    .orElse(null);
        }
        for (UUID uuid : locks) {
            Entity entity = level.getEntity(uuid);
            if (entity != null && entity.isAlive() && predicate.test(entity)) return castTo.cast(entity);
        }
        return null;
    }

    @Nullable
    public static <T extends Entity> T autoSeek(AircraftAttackContext ctx, Class<T> rangeClass,
                                                Predicate<T> predicate, double range) {
        ServerLevel level = ctx.serverLevel();
        if (level == null) return null;
        AABB box = ctx.boundingBox().inflate(range);
        return level.getEntitiesOfClass(rangeClass, box, predicate)
                .stream()
                .min(Comparator.comparingDouble(ctx::distanceToEntitySqr))
                .orElse(null);
    }

    /** 对海/对地攻击目标：手动锁定优先，自动索敌只找敌对地面目标。 */
    @Nullable
    public static LivingEntity resolveGroundTarget(AircraftAttackContext ctx, Player owner) {
        return resolveGroundTarget(ctx, owner, entity -> true);
    }

    /** 对海/对地攻击目标，附加调用方过滤（例如火箭机排除水下单位）。 */
    @Nullable
    public static LivingEntity resolveGroundTarget(AircraftAttackContext ctx, Player owner,
                                                   Predicate<LivingEntity> filter) {
        LivingEntity locked = resolveLocked(ctx, owner,
                entity -> entity instanceof LivingEntity living
                        && !isAirborneTarget(living) && filter.test(living),
                LivingEntity.class);
        if (locked != null) return locked;
        if (!FireControlManager.getTargets(owner.getUUID()).isEmpty()) return null;
        if (ctx.hasEverHadFireControl()) return null;
        return autoSeek(ctx, LivingEntity.class,
                entity -> entity.isAlive() && entity != owner
                        && CombatTargeting.isHostileTarget(owner, entity)
                        && !(entity instanceof Mob mob && mob.isNoAi())
                        && !isAirborneTarget(entity)
                        && filter.test(entity),
                ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
    }

    /** 战斗机目标：只接受空中目标；手动锁定优先，自动索敌只找敌对飞机。 */
    @Nullable
    public static Entity resolveFighterTarget(AircraftAttackContext ctx, Player owner) {
        Entity locked = resolveLocked(ctx, owner,
                TargetResolution::isAirborneTarget,
                Entity.class);
        if (locked != null) return locked;
        if (!FireControlManager.getTargets(owner.getUUID()).isEmpty()) return null;
        return autoSeek(ctx, com.piranport.entity.AircraftEntity.class,
                entity -> entity.isAlive() && entity != ctx.self()
                        && CombatTargeting.isHostileTarget(owner, entity),
                ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
    }

    /** 自主机枪战斗机：只找玩家飞机或中立生物，不主动打深海单位。 */
    @Nullable
    public static Entity resolveAutonomousFighterTarget(AircraftAttackContext ctx) {
        ServerLevel level = ctx.serverLevel();
        if (level == null) return null;
        AABB box = ctx.boundingBox().inflate(ModEquipmentConfig.AIRCRAFT_AUTONOMOUS_SEEK_RANGE.get());
        return level.getEntitiesOfClass(Entity.class, box,
                        entity -> entity.isAlive() && entity != ctx.self()
                                && isAutonomousFighterPrey(entity))
                .stream()
                .min(Comparator.comparingDouble(ctx::distanceToEntitySqr))
                .orElse(null);
    }

    private static boolean isAutonomousFighterPrey(Entity entity) {
        if (entity instanceof com.piranport.entity.AircraftEntity other) {
            return other.getOwnerUUID() != null;
        }
        return entity instanceof Mob mob && !(mob instanceof Enemy) && !mob.isNoAi();
    }
}
