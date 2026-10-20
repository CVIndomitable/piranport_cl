package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.aviation.combat.TorpedoRunState;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.debug.PiranPortDebug;
import com.piranport.entity.TorpedoEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 鱼雷机攻击策略（profile=torpedo）。
 *
 * <p>开场过近时先背向拉开到平飞入口距离；接近 → 低空贴水面平飞 → 到预测投放点投雷。平飞段每 tick 用一元一次近似解算
 * 预测撞击点，并同步给客户端画预瞄线。
 */
public final class TorpedoAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.TORPEDO.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.TORPEDO; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        LivingEntity target = TargetResolution.resolveGroundTarget(context, owner);
        if (target == null) {
            if (context.stateTicks() >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get()) {
                context.resumeCruising();
            }
            return;
        }
        tickTorpedoRun(context, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        LivingEntity target = context.autonomousTarget();
        if (target == null) {
            context.startReturning("target_lost");
            return;
        }
        tickTorpedoRun(context, null, target);
    }

    private static void tickTorpedoRun(AircraftAttackContext context, @Nullable Player owner,
                                       LivingEntity target) {
        if (context.remainingAmmo() <= 0) {
            context.startReturning("torpedo_bomber_done");
            return;
        }

        TorpedoRunState state = context.torpedoRunState();
        Vec3 position = context.position();
        double dx = target.getX() - position.x;
        double dz = target.getZ() - position.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double phaseSpeed = context.phaseSpeedCoefficient();
        // 入水速度按载机系别：美/德 2.5 m/s（0.125 block/tick），日/英/意 2.8 m/s（0.14 block/tick）。
        // 法/未知不在对照表内，按较快的日/英/意档兜底。提前量与 setSpeed 共用此值。
        double torpedoSpeed = torpedoSpeedFor(context.definition().nation());

        // 提前量解算：t = 当前水平距离 / 雷速，P = T + u·t
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 targetVelocityHorizontal = new Vec3(targetVelocity.x, 0, targetVelocity.z);
        double timeToHit = torpedoSpeed > 0.01
                ? horizontalDistance / torpedoSpeed : horizontalDistance / 0.01;
        Vec3 lead = targetVelocityHorizontal.scale(timeToHit);
        double leadLength = lead.length();
        double maxLead = ModEquipmentConfig.TORPEDO_BOMBER_MAX_LEAD.get();
        if (leadLength > maxLead) lead = lead.scale(maxLead / leadLength);
        Vec3 predicted = new Vec3(target.getX(), 0, target.getZ()).add(lead);
        Vec3 toPredicted = new Vec3(predicted.x - position.x, 0, predicted.z - position.z);
        double distanceToPredicted = toPredicted.length();

        // 阶段 1：接近段（顺便从 approach_altitude 平滑降到 run_altitude）
        if (state.runStart == null) {
            context.setTorpedoAimPoint(null);
            double runDistance = ModEquipmentConfig.TORPEDO_BOMBER_RUN_DISTANCE.get();
            double runAltitude = ModEquipmentConfig.TORPEDO_BOMBER_RUN_ALTITUDE.get();
            double tolerance = ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get();
            double descentStart = Math.max(runDistance,
                    ModEquipmentConfig.TORPEDO_BOMBER_DESCENT_START_DISTANCE.get());
            double desiredAltitude = target.getY() + (horizontalDistance <= descentStart
                    ? runAltitude
                    : ModEquipmentConfig.TORPEDO_BOMBER_APPROACH_ALTITUDE.get());
            boolean atRunAltitude = position.y - (target.getY() + runAltitude) <= tolerance;

            // 从远处正常接近越过入口距离的情况不算过近，只有开场就在入口以内才拉开。
            if (horizontalDistance >= runDistance) state.standoffReached = true;
            if (!state.standoffReached && horizontalDistance < runDistance) state.egressing = true;
            if (state.egressing && horizontalDistance >= runDistance && atRunAltitude) state.egressing = false;
            if (state.egressing) {
                tickEgress(context, position, target, dx, dz, horizontalDistance, phaseSpeed, runAltitude);
                return;
            }

            if (horizontalDistance <= runDistance && atRunAltitude && context.attackCooldown() <= 0) {
                state.runStart = position;
                state.runTicks = 0;
            } else if (horizontalDistance <= runDistance && context.attackCooldown() > 0) {
                // 已到航线入口但武器还在冷却：盘旋等待，避免贴着目标投雷
                Vec3 radial = horizontalDistance > 0.01
                        ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1);
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
                double holdSpeed = Math.min(context.panelSpeed() * 0.5 * phaseSpeed, 0.8);
                double radialError = horizontalDistance - runDistance;
                double radialSpeed = Math.max(-holdSpeed * 0.5,
                        Math.min(holdSpeed * 0.5, radialError * 0.1));
                double yAdjust = (target.getY() + runAltitude - position.y)
                        * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
                context.setDeltaMovement(new Vec3(
                        tangent.x * holdSpeed + radial.x * radialSpeed,
                        yAdjust,
                        tangent.z * holdSpeed + radial.z * radialSpeed));
                return;
            } else {
                double altitudeDifference = position.y - desiredAltitude;
                if (altitudeDifference > tolerance) {
                    Vec3 toTarget = horizontalDistance > 0.01
                            ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1);
                    double horizontalSpeed = Math.min(context.panelSpeed()
                            * ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_SPEED_MULTIPLIER.get()
                            * phaseSpeed, ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_MAX_SPEED.get());
                    double maxVertical = Math.max(0.05, horizontalSpeed * 0.35);
                    double yAdjust = Math.max(-maxVertical, Math.min(maxVertical,
                            (desiredAltitude - position.y)
                                    * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get()));
                    context.setDeltaMovement(new Vec3(
                            toTarget.x * horizontalSpeed, yAdjust, toTarget.z * horizontalSpeed));
                    return;
                }
                Vec3 toTarget = new Vec3(dx, 0, dz).normalize().scale(Math.min(context.panelSpeed()
                        * ModEquipmentConfig.TORPEDO_BOMBER_APPROACH_SPEED_MULTIPLIER.get()
                        * phaseSpeed, horizontalDistance));
                double yAdjust = (desiredAltitude - position.y)
                        * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
                context.setDeltaMovement(new Vec3(toTarget.x, yAdjust, toTarget.z));
                return;
            }
        }

        // 阶段 2：低空贴水面平飞，朝预测撞击点直飞
        context.setTorpedoAimPoint(new Vec3(predicted.x, target.getY(), predicted.z));
        state.runTicks++;
        double runAltitude = ModEquipmentConfig.TORPEDO_BOMBER_RUN_ALTITUDE.get();
        double desiredY = target.getY() + runAltitude;
        double runAltitudeDifference = position.y - desiredY;
        boolean atRunAltitude = runAltitudeDifference
                <= ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get();
        boolean timeout = state.runTicks
                > ModEquipmentConfig.TORPEDO_BOMBER_RUN_TIMEOUT_TICKS.get();

        if ((horizontalDistance <= ModEquipmentConfig.TORPEDO_BOMBER_RELEASE_DISTANCE.get()
                && atRunAltitude || timeout)
                && context.attackCooldown() <= 0 && distanceToPredicted > 0.01) {
            Vec3 direction = toPredicted.normalize();
            int toFire = context.remainingAmmo();
            PiranPortDebug.event(
                    "Aircraft TORPEDO_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                    context.entityId(), context.ammoCapacity(), context.remainingAmmo(),
                    toFire, target.getHealth());
            for (int i = 0; i < toFire; i++) {
                TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(),
                        context.level());
                torpedo.setAerialTorpedo(true);
                double offsetX = -direction.z * (i - (toFire - 1) / 2.0)
                        * ModEquipmentConfig.TORPEDO_BOMBER_SPREAD_OFFSET.get();
                double offsetZ = direction.x * (i - (toFire - 1) / 2.0)
                        * ModEquipmentConfig.TORPEDO_BOMBER_SPREAD_OFFSET.get();
                torpedo.moveTo(position.x + offsetX,
                        position.y - ModEquipmentConfig.TORPEDO_BOMBER_DROP_HEIGHT_OFFSET.get(),
                        position.z + offsetZ, 0, 0);
                torpedo.setSpeed((float) torpedoSpeed);
                torpedo.setAirDrop(true, direction);
                Vec3 velocity = direction.scale(torpedoSpeed)
                        .add(0, ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_VERTICAL_SPEED.get(), 0);
                torpedo.setDeltaMovement(velocity);
                torpedo.setOwner(owner);
                context.level().addFreshEntity(torpedo);
            }
            context.setRemainingAmmo(context.remainingAmmo() - toFire);
            context.setAttackCooldown(context.attackCooldownDuration());
            if (context.remainingAmmo() <= 0) {
                context.startReturning("torpedo_launched");
            }
            return;
        }

        double runSpeed = Math.min(context.panelSpeed()
                * ModEquipmentConfig.TORPEDO_BOMBER_RUN_SPEED_MULTIPLIER.get()
                * phaseSpeed, Math.max(distanceToPredicted, 0.1));
        Vec3 runDirection = distanceToPredicted > 0.01 ? toPredicted.normalize()
                : (horizontalDistance > 0.01 ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1));
        double yAdjust = (desiredY - position.y)
                * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
        if (runAltitudeDifference > ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get()) {
            yAdjust -= Math.min(runAltitudeDifference * 0.2,
                    ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_MAX_SPEED.get());
        }
        double maxVertical = Math.max(0.05, runSpeed * 0.25);
        yAdjust = Math.max(-maxVertical, Math.min(maxVertical, yAdjust));
        context.setDeltaMovement(new Vec3(runDirection.x * runSpeed, yAdjust, runDirection.z * runSpeed));
    }

    /**
     * 背向拉开：沿目标到本机的方向飞出，同时把高度调到平飞高度，直到距离与高度都满足平飞入口。
     */
    private static void tickEgress(AircraftAttackContext context, Vec3 position, LivingEntity target,
                                   double dx, double dz, double horizontalDistance,
                                   double phaseSpeed, double runAltitude) {
        Vec3 outward = horizontalDistance > 0.01
                ? new Vec3(-dx, 0, -dz).normalize() : new Vec3(0, 0, 1);
        double horizontalSpeed = context.panelSpeed()
                * ModEquipmentConfig.TORPEDO_BOMBER_EGRESS_SPEED_MULTIPLIER.get() * phaseSpeed;
        double maxVertical = Math.max(0.05, horizontalSpeed * 0.35);
        double yAdjust = Math.max(-maxVertical, Math.min(maxVertical,
                (target.getY() + runAltitude - position.y)
                        * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get()));
        context.setDeltaMovement(new Vec3(
                outward.x * horizontalSpeed, yAdjust, outward.z * horizontalSpeed));
    }

    /**
     * 航空鱼雷入水速度（block/tick）。美/德 2.5 m/s，日/英/意 2.8 m/s。
     *
     * <p>法/未知不在对照表内，走终端参数 {@code global.aircraft.torpedo_bomber_combat.torpedo_speed}
     * 作为兜底，默认已同步为 0.14（2.8 m/s）；不让该参数变成「能改但不生效」的死参数。</p>
     */
    static double torpedoSpeedFor(AircraftDefinition.Nation nation) {
        return switch (nation) {
            case US, DE -> 0.125;
            case JP, GB, IT -> 0.14;
            case FR, UNKNOWN -> ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_SPEED.get();
        };
    }
}
