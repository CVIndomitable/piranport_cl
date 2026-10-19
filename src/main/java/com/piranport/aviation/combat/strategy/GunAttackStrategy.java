package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.BulletEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 机枪战斗机策略（profile=gun）。
 *
 * <p>战斗机弹药无限；索敌走「敌机优先、火控锁定兜底」。战术本体是本类的
 * {@link #tickAir}，火箭机对空退化时也复用它。
 */
public final class GunAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.GUN.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.GUN; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        Entity target = TargetResolution.resolveFighterTarget(context, owner);
        if (target == null) {
            if (context.stateTicks() >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get()) {
                context.resumeCruising();
            }
            return;
        }
        tickAir(context, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        Entity target = TargetResolution.resolveAutonomousFighterTarget(context);
        if (target == null) {
            context.resumeCruising();
            return;
        }
        tickAir(context, null, target);
    }

    /** 对空机枪战术；火箭机打完火箭后退化到这里。 */
    public static void tickAir(AircraftAttackContext context, @Nullable Player owner, Entity target) {
        Vec3 toTarget = target.getEyePosition().subtract(context.position());
        double distance = toTarget.length();
        if (distance < 0.01) return; // 零距离保护：防止 normalize 产生 NaN

        double preferred = ModEquipmentConfig.FIGHTER_HOVER_DISTANCE.get();
        double band = ModEquipmentConfig.FIGHTER_HOVER_BAND.get();
        if (distance > preferred + band) {
            context.setDeltaMovement(toTarget.normalize().scale(Math.min(context.panelSpeed()
                    * ModEquipmentConfig.FIGHTER_PURSUIT_MULTIPLIER.get()
                    * context.phaseSpeedCoefficient(), distance)));
        } else if (distance < preferred - band) {
            context.setDeltaMovement(toTarget.normalize().scale(-context.panelSpeed()
                    * ModEquipmentConfig.FIGHTER_RETREAT_MULTIPLIER.get()
                    * context.phaseSpeedCoefficient()));
        } else {
            context.setDeltaMovement(context.deltaMovement()
                    .scale(ModEquipmentConfig.FIGHTER_HOVER_DAMPING.get()));
        }

        if (context.attackCooldown() <= 0 && distance < ModEquipmentConfig.FIGHTER_BULLET_RANGE.get()) {
            // 保留 float 除法（.floatValue()），保证默认 8.0 时与下沉前 8f 逐字节一致。
            BulletEntity bullet = new BulletEntity(context.level(),
                    context.panelDamage()
                            / ModEquipmentConfig.FIGHTER_BULLET_DAMAGE_DIVISOR.get().floatValue());
            Vec3 direction = toTarget.normalize();
            Vec3 position = context.position();
            bullet.moveTo(position.x,
                    position.y + ModEquipmentConfig.FIGHTER_BULLET_SPAWN_Y_OFFSET.get(),
                    position.z, bullet.getYRot(), bullet.getXRot());
            bullet.setDeltaMovement(direction.scale(ModEquipmentConfig.FIGHTER_BULLET_SPEED.get()));
            bullet.setOwner(owner);
            bullet.setSourceAircraftName(context.displayName());
            bullet.setSourceAircraft(context.self());
            context.level().addFreshEntity(bullet);
            context.setAttackCooldown(context.attackCooldownDuration());
            // 战斗机机枪弹药固定无限，不再由配置改变行为。
        }
    }
}
