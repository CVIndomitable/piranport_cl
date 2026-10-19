package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.debug.PiranPortDebug;
import com.piranport.entity.MissileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 火箭机策略（profile=rocket）。
 *
 * <p>优先对空：空中有敌对目标时用机枪空战；没有空中目标时才进行对海火箭齐射。
 * 火箭齐射一次性消耗弹药，打完后退化为机枪对空。对海火箭不攻击水下单位。
 */
public final class RocketAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.ROCKET.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.ROCKET; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        // 优先对空：有敌对空中目标就先打空战，火箭留给没有空中威胁的时候
        Entity air = TargetResolution.resolveFighterTarget(context, owner);
        if (air != null) {
            context.setRocketRunTicks(0);
            GunAttackStrategy.tickAir(context, owner, air);
            return;
        }
        if (!context.hasFired() && context.remainingAmmo() > 0) {
            // 对海火箭不攻击水下单位
            LivingEntity ground = TargetResolution.resolveGroundTarget(context, owner,
                    target -> !target.isUnderWater());
            if (ground != null) {
                tickMissileRun(context, owner, ground);
                return;
            }
        }
        context.setRocketRunTicks(0);
        if (context.stateTicks() >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get()) {
            context.resumeCruising();
        }
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        LivingEntity target = context.autonomousTarget();
        if (target == null) {
            context.startReturning("target_lost");
            return;
        }
        // 自主火箭机的目标来自最近玩家，属于对海目标：只用火箭。
        // 机枪只对空，不能对地；火箭用完或目标在水下就直接返航。
        if (!context.hasFired() && context.remainingAmmo() > 0
                && !target.isUnderWater()) {
            tickMissileRun(context, null, target);
        } else {
            context.setRocketRunTicks(0);
            context.startReturning("rocket_exhausted");
        }
    }

    private static void tickMissileRun(AircraftAttackContext context, @Nullable Player owner,
                                       LivingEntity target) {
        if (context.rocketRunTicks() > ModEquipmentConfig.ROCKET_FIGHTER_RUN_TICKS.get()) {
            context.setHasFired(true);
            context.setRemainingAmmo(0);
            return;
        }
        context.setRocketRunTicks(context.rocketRunTicks() + 1);

        Vec3 position = context.position();
        double dx = target.getX() - position.x;
        double dz = target.getZ() - position.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double desiredY = target.getY() + ModEquipmentConfig.ROCKET_FIGHTER_ATTACK_ALTITUDE_OFFSET.get();
        double altitudeDelta = position.y - desiredY;

        if (horizontalDistance >= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_HORIZONTAL_MIN.get()
                && horizontalDistance <= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_HORIZONTAL_MAX.get()
                && altitudeDelta >= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_ALTITUDE_MIN.get()
                && altitudeDelta <= ModEquipmentConfig.ROCKET_FIGHTER_WINDOW_ALTITUDE_MAX.get()
                && context.attackCooldown() <= 0) {
            Vec3 direction = new Vec3(dx, target.getEyeY()
                    + ModEquipmentConfig.ROCKET_FIGHTER_AIM_Y_OFFSET.get() - position.y, dz).normalize();
            float rocketDamage = context.panelDamage()
                    * ModEquipmentConfig.ROCKET_FIGHTER_DAMAGE_MULTIPLIER.get().floatValue();
            int toFire = context.salvoSize(target, rocketDamage);
            PiranPortDebug.event(
                    "Aircraft ROCKET_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    context.entityId(), context.ammoCapacity(), context.remainingAmmo(),
                    toFire, target.getHealth());
            float initialSpeed = MissileEntity.MissileType.ROCKET.initialSpeed();
            for (int i = 0; i < toFire; i++) {
                double spread = (i - (toFire - 1) / 2.0)
                        * ModEquipmentConfig.ROCKET_FIGHTER_SPREAD_STEP.get();
                double cos = Math.cos(spread);
                double sin = Math.sin(spread);
                Vec3 fanDirection = new Vec3(
                        direction.x * cos - direction.z * sin,
                        direction.y,
                        direction.x * sin + direction.z * cos).normalize();
                MissileEntity rocket = new MissileEntity(context.level(),
                        MissileEntity.MissileType.ROCKET,
                        rocketDamage, 0f,
                        ModEquipmentConfig.ROCKET_FIGHTER_EXPLOSION_POWER.get().floatValue(),
                        "piranport:rocket_ammo");
                rocket.moveTo(position.x,
                        position.y + ModEquipmentConfig.ROCKET_FIGHTER_SPAWN_Y_OFFSET.get(),
                        position.z, rocket.getYRot(), rocket.getXRot());
                rocket.setDeltaMovement(fanDirection.scale(initialSpeed));
                rocket.setOwner(owner);
                context.level().addFreshEntity(rocket);
            }
            context.setRemainingAmmo(context.remainingAmmo() - toFire);
            if (context.remainingAmmo() <= 0) {
                context.setHasFired(true);
            } else {
                context.setAttackCooldown(context.attackCooldownDuration());
            }
            return;
        }

        Vec3 toTarget = new Vec3(dx, desiredY - position.y, dz);
        double distance = toTarget.length();
        if (distance > 0.1) {
            context.setDeltaMovement(toTarget.normalize().scale(Math.min(context.panelSpeed()
                    * ModEquipmentConfig.ROCKET_FIGHTER_PURSUIT_MULTIPLIER.get()
                    * context.phaseSpeedCoefficient(), distance)));
        }
    }
}
