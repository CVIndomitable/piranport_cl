package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.aviation.combat.DiveBombState;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AerialBombEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 俯冲轰炸策略（profile=dive_bomb）。 */
public final class DiveBombAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.DIVE_BOMB.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.DIVE_BOMB; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        LivingEntity target = TargetResolution.resolveGroundTarget(context, owner);
        if (target == null) {
            // 目标中途消失时不能保留上一 tick 的下冲速度等回退计时——飞机会带着俯冲姿态
            // 继续往水里扎最多 AIRCRAFT_ATTACK_FALLBACK_TICKS 个 tick。
            pullUp(context);
            if (context.stateTicks() >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get()) {
                context.resumeCruising();
            }
            return;
        }
        tickDive(context, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        LivingEntity target = context.autonomousTarget();
        if (target == null) {
            context.startReturning("target_lost");
            return;
        }
        tickDive(context, null, target);
    }

    private static void tickDive(AircraftAttackContext context, @Nullable Player owner,
                                 LivingEntity target) {
        if (context.hasFired()) {
            context.startReturning("dive_bomber_done");
            return;
        }

        DiveBombState state = context.diveBombState();
        Vec3 position = context.position();
        double climbY = target.getY() + ModEquipmentConfig.DIVE_BOMBER_CLIMB_ALTITUDE_OFFSET.get();
        double heightDiff = Math.max(0, climbY - position.y);
        double phaseSpeed = context.phaseSpeedCoefficient();
        int climbTimeout = Math.max(ModEquipmentConfig.DIVE_BOMBER_CLIMB_TIMEOUT_MIN_TICKS.get(),
                (int) Math.ceil(heightDiff / Math.max(context.panelSpeed()
                        * ModEquipmentConfig.DIVE_BOMBER_CLIMB_SPEED_MULTIPLIER.get()
                        * phaseSpeed, 0.1)));
        if (!state.committed
                && position.y < climbY - ModEquipmentConfig.DIVE_BOMBER_CLIMB_ARRIVAL_TOLERANCE.get()
                && context.stateTicks() < climbTimeout) {
            Vec3 toClimb = new Vec3(target.getX() - position.x, climbY - position.y,
                    target.getZ() - position.z);
            double distance = toClimb.length();
            context.setDeltaMovement(toClimb.normalize().scale(Math.min(context.panelSpeed()
                    * ModEquipmentConfig.DIVE_BOMBER_CLIMB_SPEED_MULTIPLIER.get()
                    * phaseSpeed, distance)));
            return;
        }

        // 安全高度 = max(水面, 目标脚底) + 余量。目标在水里/跳水时也不能把瞄准点压到水下。
        double safeY = diveSafeY(context, target);

        if (!state.committed) {
            state.committed = true;
            Vec3 targetPosition = target.getEyePosition();
            double estimatedDistance = position.distanceTo(targetPosition);
            double diveSpeed = Math.max(context.panelSpeed()
                    * ModEquipmentConfig.DIVE_BOMBER_DIVE_SPEED_MULTIPLIER.get() * phaseSpeed, 0.1);
            double estimatedTicks = estimatedDistance / diveSpeed;
            Vec3 lead = target.getDeltaMovement().scale(estimatedTicks);
            // 提前量封顶：目标瞬时竖直速度 × 上百 tick 的预估时间足以把投弹点拉到水下几十格，
            // 也会把投弹点甩到目标几十格外（敌方航弹甚至落到客户端实体追踪范围之外）。
            double maxLead = ModEquipmentConfig.DIVE_BOMBER_MAX_LEAD.get();
            if (lead.length() > maxLead) {
                lead = lead.scale(maxLead / lead.length());
            }
            Vec3 aimPoint = targetPosition.add(lead);
            state.target = aimPoint.y < safeY ? new Vec3(aimPoint.x, safeY, aimPoint.z) : aimPoint;
        }

        if (state.target == null) return;

        // 目标大幅偏离提交的投弹点：拉起中止，回到巡航重新索敌/重新提交。
        // 保留 committed dive：只允许重新瞄准，不做无限追踪。
        if (target.getEyePosition().distanceTo(state.target)
                > ModEquipmentConfig.DIVE_BOMBER_ABORT_DEVIATION.get()) {
            abortDive(context);
            return;
        }

        // 安全高度保护：任何情况下都不许继续下潜到安全高度以下。
        if (position.y < safeY) {
            abortDive(context);
            return;
        }

        Vec3 toDive = state.target.subtract(position);
        double diveDistance = toDive.length();
        if (diveDistance < ModEquipmentConfig.DIVE_BOMBER_RELEASE_DISTANCE.get()) {
            Level level = context.level();
            float bombPower = context.panelDamage();
            int bombCount = Math.max(1, context.remainingAmmo());
            for (int i = 0; i < bombCount; i++) {
                AerialBombEntity bomb = new AerialBombEntity(level, bombPower / bombCount,
                        ModEquipmentConfig.DIVE_BOMBER_EXPLOSION_POWER.get().floatValue());
                double spreadX = (level.random.nextDouble() - 0.5)
                        * ModEquipmentConfig.DIVE_BOMBER_BOMB_SPREAD.get();
                double spreadZ = (level.random.nextDouble() - 0.5)
                        * ModEquipmentConfig.DIVE_BOMBER_BOMB_SPREAD.get();
                bomb.moveTo(position.x + spreadX, position.y, position.z + spreadZ, 0, 0);
                double dropHeight = Math.max(1.0, position.y - target.getY());
                bomb.setDispersionDeg((float) Math.toDegrees(Math.atan(
                        ModEquipmentConfig.DIVE_BOMBER_BOMB_SPREAD.get() * 0.5 / dropHeight)));
                // 保留载机水平动量；俯冲投弹与水平轰炸的前抛是两种机制，各自可调。
                Vec3 aircraftVelocity = context.deltaMovement();
                bomb.setDeltaMovement(
                        aircraftVelocity.x * ModEquipmentConfig.DIVE_BOMBER_HORIZONTAL_MOMENTUM.get(),
                        Math.min(ModEquipmentConfig.DIVE_BOMBER_MIN_VERTICAL_VELOCITY.get(),
                                aircraftVelocity.y),
                        aircraftVelocity.z * ModEquipmentConfig.DIVE_BOMBER_HORIZONTAL_MOMENTUM.get());
                bomb.setOwner(owner);
                level.addFreshEntity(bomb);
            }
            context.setRemainingAmmo(0);
            context.setHasFired(true);
            // 投弹当 tick 就换成上仰速度：否则外层还会用上一 tick 的下冲速度再走一步，
            // 低空投弹时这一步正好把飞机推进水里。自主机下一 tick 即销毁，这一步就是它最后的姿态。
            pullUp(context);
            return;
        }

        double speed = Math.min(context.panelSpeed()
                * ModEquipmentConfig.DIVE_BOMBER_DIVE_APPROACH_MULTIPLIER.get()
                * phaseSpeed, diveDistance);
        Vec3 diveVelocity = toDive.normalize().scale(speed);
        // 单 tick 下潜量也不许穿过安全高度（转弯约束可能让机头偏离瞄准点方向）。
        double maxDescend = safeY - position.y;
        if (diveVelocity.y < maxDescend) {
            diveVelocity = new Vec3(diveVelocity.x, maxDescend, diveVelocity.z);
        }
        context.setDeltaMovement(diveVelocity);
    }

    /** 拉起/中止共用：改向上仰速度 → 清本次俯冲状态 → 回巡航重新进入攻击。 */
    private static void abortDive(AircraftAttackContext context) {
        pullUp(context);
        context.diveBombState().reset();
        context.resumeCruising();
    }

    /**
     * 把当前速度换成「保持水平航向 + 向上爬升」的离场速度。
     *
     * <p>水平分量取当前航向而不是瞄准点方向：投弹/中止瞬间的目标方向已经不可信。
     * 阶段系数按调用时的状态读取（中止场景要在 resumeCruising 之前调用）。</p>
     */
    private static void pullUp(AircraftAttackContext context) {
        Vec3 velocity = context.deltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double forward = context.panelSpeed() * context.phaseSpeedCoefficient()
                * ModEquipmentConfig.DIVE_BOMBER_PULL_UP_FORWARD_MULTIPLIER.get();
        Vec3 direction = horizontal > 1.0e-6
                ? new Vec3(velocity.x / horizontal, 0, velocity.z / horizontal)
                : Vec3.ZERO;
        context.setDeltaMovement(new Vec3(direction.x * forward,
                ModEquipmentConfig.DIVE_BOMBER_PULL_UP_CLIMB_RATE.get(),
                direction.z * forward));
    }

    /** 俯冲安全高度：水面与目标脚底取高者，再抬一个终端余量。 */
    private static double diveSafeY(AircraftAttackContext context, LivingEntity target) {
        double waterY = waterSurfaceY(context, target.getX(), target.getZ(), target.getY());
        return Math.max(target.getY(), waterY) + ModEquipmentConfig.DIVE_BOMBER_MIN_ALTITUDE.get();
    }

    /**
     * 目标所在列的水面高度；扫不到水（陆地/其他维度）时退回目标脚底。
     * 与鱼雷机同口径：静止水面顶面在方块底边之上 0.875 格。
     */
    private static double waterSurfaceY(AircraftAttackContext context, double x, double z,
                                        double fallback) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int centerY = (int) Math.floor(fallback);
        for (int y = centerY + 8; y >= centerY - 16; y--) {
            if (context.level().getFluidState(new BlockPos(blockX, y, blockZ)).is(Fluids.WATER)) {
                return y + 0.875;
            }
        }
        return fallback;
    }
}
