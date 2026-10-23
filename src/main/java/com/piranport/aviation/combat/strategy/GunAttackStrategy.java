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

        double fireDistance = ModEquipmentConfig.FIGHTER_HOVER_DISTANCE.get();
        // 开火窗口取整段悬停死区（而不是死区的 1/4）：追击/退避时飞机穿过 ±band 的速度
        // 远快于 1/4 死区，窗口过窄会变成「贴到 3 格却偶尔才打一发」。
        double fireBand = Math.max(0.25, ModEquipmentConfig.FIGHTER_HOVER_BAND.get());
        if (context.attackCooldown() <= 0
                && distance <= ModEquipmentConfig.FIGHTER_BULLET_RANGE.get()
                && Math.abs(distance - fireDistance) <= fireBand) {
            // 保留 float 除法（.floatValue()），保证默认 8.0 时与下沉前 8f 逐字节一致。
            BulletEntity bullet = new BulletEntity(context.level(),
                    context.panelDamage()
                            / ModEquipmentConfig.FIGHTER_BULLET_DAMAGE_DIVISOR.get().floatValue());
            // 一阶提前量：子弹飞到目标眼位需要 distance / bullet_speed tick，
            // 幻翼这类快速目标在 3 格处不提前约差半格，正好从碰撞箱边上擦过去。
            double bulletSpeed = Math.max(0.1, ModEquipmentConfig.FIGHTER_BULLET_SPEED.get());
            Vec3 aimPoint = target.getEyePosition()
                    .add(target.getDeltaMovement().scale(distance / bulletSpeed));
            Vec3 direction = aimPoint.subtract(context.position()).normalize();
            Vec3 position = context.position();
            bullet.moveTo(position.x,
                    position.y + ModEquipmentConfig.FIGHTER_BULLET_SPAWN_Y_OFFSET.get(),
                    position.z, bullet.getYRot(), bullet.getXRot());
            bullet.setDeltaMovement(direction.scale(ModEquipmentConfig.FIGHTER_BULLET_SPEED.get()));
            bullet.setOwner(owner);
            // 无主人 = 深海自主战斗机：子弹不得命中玩家本体（《深海/01》战斗机只打飞机与中立生物）
            bullet.setSparesPlayers(owner == null);
            bullet.setSourceAircraftName(context.displayName());
            bullet.setSourceAircraft(context.self());
            context.level().addFreshEntity(bullet);
            // 机枪用独立短间隔；机型的 attack_cooldown 是投弹/一轮攻击的节奏，不能拿来卡狗斗。
            context.setAttackCooldown(ModEquipmentConfig.FIGHTER_GUN_COOLDOWN.get());
            // 战斗机机枪弹药固定无限，不再由配置改变行为。
        }
    }
}
