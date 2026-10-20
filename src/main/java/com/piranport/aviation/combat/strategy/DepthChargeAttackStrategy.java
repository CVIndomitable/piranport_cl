package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.combat.CombatTargeting;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.debug.PiranPortDebug;
import com.piranport.entity.AircraftAswRecon;
import com.piranport.entity.DepthChargeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 反潜深弹攻击策略（profile=depth_charge）。 */
public final class DepthChargeAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.DEPTH_CHARGE.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.DEPTH_CHARGE; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        LivingEntity target = resolveAswTarget(context, owner);
        if (target == null) {
            if (context.stateTicks() >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get()) {
                context.resumeCruising();
            }
            return;
        }
        tickAswAttack(context, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        LivingEntity target = context.autonomousTarget();
        if (target == null) {
            context.startReturning("target_lost");
            return;
        }
        tickAswAttack(context, null, target);
    }

    private static void tickAswAttack(AircraftAttackContext context, @Nullable Player owner,
                                      LivingEntity target) {
        if (context.remainingAmmo() <= 0) {
            context.startReturning("asw_ammo_depleted");
            return;
        }

        Vec3 position = context.position();
        double seaLevel = context.level().getSeaLevel();
        double bombAltitude = target.isUnderWater()
                ? seaLevel + 15.0
                : target.getY() + ModEquipmentConfig.ASW_ATTACK_HEIGHT_OFFSET.get();

        if (position.y < bombAltitude - 1.5) {
            Vec3 toPoint = new Vec3(target.getX() - position.x, bombAltitude - position.y,
                    target.getZ() - position.z);
            double distance = toPoint.length();
            context.setDeltaMovement(toPoint.normalize().scale(Math.min(context.panelSpeed() * 0.4
                    * context.phaseSpeedCoefficient(), distance)));
            return;
        }

        double dx = target.getX() - position.x;
        double dz = target.getZ() - position.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double targetDepth = Math.max(0, seaLevel - target.getY());
        double leadDistance = targetDepth * 0.15;

        if (horizontalDistance < (4.0 + leadDistance) && context.attackCooldown() <= 0) {
            float depthChargeDamage = context.panelDamage() * 1.5f;
            int toFire = context.salvoSize(target, depthChargeDamage);
            PiranPortDebug.event(
                    "Aircraft ASW_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    context.entityId(), context.ammoCapacity(), context.remainingAmmo(),
                    toFire, target.getHealth());
            Vec3 velocity = context.deltaMovement();
            double horizontalLength = velocity.horizontalDistance();
            Vec3 forward = horizontalLength > 0.01
                    ? new Vec3(velocity.x / horizontalLength, 0, velocity.z / horizontalLength)
                    : new Vec3(1, 0, 0);
            for (int i = 0; i < toFire; i++) {
                double offset = (i - (toFire - 1) / 2.0) * 0.8;
                DepthChargeEntity depthCharge = new DepthChargeEntity(context.level(), depthChargeDamage,
                        (float) (double) ModProjectilesConfig.DEPTH_CHARGE_EXPLOSION_POWER.get());
                depthCharge.moveTo(position.x + forward.x * offset, position.y,
                        position.z + forward.z * offset, depthCharge.getYRot(), depthCharge.getXRot());
                depthCharge.setDeltaMovement(velocity.x * 0.1, -0.1, velocity.z * 0.1);
                depthCharge.setOwner(owner);
                context.level().addFreshEntity(depthCharge);
            }
            context.setRemainingAmmo(context.remainingAmmo() - toFire);
            context.setAttackCooldown(context.attackCooldownDuration());
        }

        Vec3 horizontal = new Vec3(dx, 0, dz).normalize().scale(Math.min(context.panelSpeed() * 0.4
                * context.phaseSpeedCoefficient(), horizontalDistance));
        double yCorrect = (bombAltitude - position.y) * 0.15;
        context.setDeltaMovement(new Vec3(horizontal.x, yCorrect, horizontal.z));
    }

    /**
     * 反潜目标解析：手动火控锁定优先；自动索敌必须同时满足“反潜对象”和“敌对”，
     * 不能复用声呐的 {@link AircraftAswRecon#isAswTarget}（它会认鱼类/海豚）。
     */
    @Nullable
    private static LivingEntity resolveAswTarget(AircraftAttackContext context,
                                                 @Nullable Player owner) {
        ServerLevel level = context.serverLevel();
        if (level == null || owner == null) return null;

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (context.attackMode() == com.piranport.component.AircraftAttackMode.SPREAD) {
                return locks.stream()
                        .map(level::getEntity)
                        .filter(entity -> entity instanceof LivingEntity living
                                && living.isAlive() && AircraftAswRecon.isAswTarget(living))
                        .map(entity -> (LivingEntity) entity)
                        .min(Comparator.comparingDouble(context::distanceToEntity))
                        .orElse(null);
            }
            for (UUID uuid : locks) {
                Entity entity = level.getEntity(uuid);
                if (entity instanceof LivingEntity living && living.isAlive()
                        && AircraftAswRecon.isAswTarget(living)) {
                    return living;
                }
            }
            return null;
        }

        if (context.hasEverHadFireControl() || context.autoSeekDone()) return null;
        if (context.autoSeekCooldown() > 0) {
            context.setAutoSeekCooldown(context.autoSeekCooldown() - 1);
            return null;
        }
        context.setAutoSeekDone(true);
        AABB box = context.boundingBox().inflate(ModEquipmentConfig.ASW_AUTO_SEEK_RANGE.get());
        return level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> entity.isAlive() && entity != owner
                                && CombatTargeting.isHostileTarget(owner, entity)
                                && AircraftAswRecon.isAswTarget(entity))
                .stream()
                .min(Comparator.comparingDouble(context::distanceToEntity))
                .orElse(null);
    }
}
