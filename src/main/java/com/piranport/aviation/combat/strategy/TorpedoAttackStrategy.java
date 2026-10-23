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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
            // 目标在投雷前死亡/消失时，不能保留上一 tick 的航线和预瞄点；否则
            // 飞机会继续沿旧的预测方向飞，看起来像突然掉头或反向追击。
            context.setTorpedoAimPoint(null);
            context.torpedoRunState().reset();
            context.resumeCruising();
            return;
        }
        tickTorpedoRun(context, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        LivingEntity target = context.autonomousTarget();
        if (target == null || !target.isAlive()) {
            context.setTorpedoAimPoint(null);
            context.torpedoRunState().reset();
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
        // 中途换目标（手动多锁定里前一个死亡、或 SPREAD 模式改选最近目标）时，
        // 旧航线的 runStart/approachDirection/平滑水面高度全部作废，按新目标重新进入。
        UUID targetId = target.getUUID();
        if (state.activeTarget != null && !state.activeTarget.equals(targetId)) {
            state.reset();
        }
        state.activeTarget = targetId;
        Vec3 position = context.position();
        double dx = target.getX() - position.x;
        double dz = target.getZ() - position.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double phaseSpeed = context.phaseSpeedCoefficient();
        // 入水速度按载机系别：美/德 2.5 m/s（0.125 block/tick），日/英/意 2.8 m/s（0.14 block/tick）。
        // 法/未知不在对照表内，按较快的日/英/意档兜底。提前量与 setSpeed 共用此值。
        double torpedoSpeed = torpedoSpeedFor(context.definition().nation());

        // 提前量解算：与玩家鱼雷发射器共用同一套水平匀速拦截方程
        // （FireControlPrediction#torpedoIntercept），不再用 t=D/s 的一元近似——
        // 那只在目标静止或纯横向时准确，迎面目标会把预测点算到机身附近甚至身后。
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 targetVelocityHorizontal = new Vec3(targetVelocity.x, 0, targetVelocity.z);
        Vec3 targetXZ = new Vec3(target.getX(), 0, target.getZ());
        Vec3 originXZ = new Vec3(position.x, 0, position.z);
        Vec3 intercept = com.piranport.combat.FireControlPrediction.torpedoIntercept(
                originXZ, targetXZ, targetVelocityHorizontal, Math.max(1.0e-3, torpedoSpeed));
        Vec3 predicted;
        if (intercept != null) {
            predicted = new Vec3(intercept.x, 0, intercept.z);
        } else {
            // 目标速度快于鱼雷（无解）时退回线性外推，再由 max_lead 封顶。
            double timeToHit = horizontalDistance / Math.max(1.0e-3, torpedoSpeed);
            predicted = targetXZ.add(targetVelocityHorizontal.scale(timeToHit));
        }
        // 提前量上限：无论精确解还是兜底外推，都不得超过终端配置，防止预测点脱缰。
        double maxLead = ModEquipmentConfig.TORPEDO_BOMBER_MAX_LEAD.get();
        Vec3 lead = predicted.subtract(targetXZ);
        double leadLength = lead.length();
        if (leadLength > maxLead) predicted = targetXZ.add(lead.scale(maxLead / leadLength));
        double surfaceY = waterSurfaceY(context, predicted.x, predicted.z, target.getY());
        // 水面高度按方块量化的，预测点横向移动时会 0/1 地跳变；直接喂给高度控制会让
        // 下降段一格一格地抽。用一阶低通把台阶抹成连续值（只影响飞行/预瞄显示，
        // 不影响鱼雷入水后的实际贴水面解算）。
        double smoothedSurfaceY = state.smoothedSurfaceY == null
                ? surfaceY
                : state.smoothedSurfaceY + (surfaceY - state.smoothedSurfaceY) * 0.25;
        state.smoothedSurfaceY = smoothedSurfaceY;
        Vec3 toPredicted = new Vec3(predicted.x - position.x, 0, predicted.z - position.z);
        double distanceToPredicted = toPredicted.length();
        // 预测点是否在机头前方。进入投雷航线和实际投雷都要用同一判据，
        // 避免「目标已经被超到身后还继续压近」。
        Vec3 horizontalVelocity = new Vec3(context.deltaMovement().x, 0,
                context.deltaMovement().z);
        boolean predictedAhead = horizontalVelocity.lengthSqr() < 1.0e-6
                || toPredicted.dot(horizontalVelocity) > 0.0;

        // 阶段 1：接近段（顺便从 approach_altitude 平滑降到 run_altitude）
        if (state.runStart == null) {
            context.setTorpedoAimPoint(null);
            double runDistance = ModEquipmentConfig.TORPEDO_BOMBER_RUN_DISTANCE.get();
            double runAltitude = ModEquipmentConfig.TORPEDO_BOMBER_RUN_ALTITUDE.get();
            double tolerance = ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get();
            double runY = smoothedSurfaceY + runAltitude;
            double approachY = smoothedSurfaceY
                    + ModEquipmentConfig.TORPEDO_BOMBER_APPROACH_ALTITUDE.get();
            double descentSpeed = Math.min(context.panelSpeed()
                    * ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_SPEED_MULTIPLIER.get()
                    * phaseSpeed, ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_MAX_SPEED.get());
            double maxVertical = Math.max(0.05, descentSpeed * 0.35);
            double fixedDescentStart = Math.max(runDistance,
                    ModEquipmentConfig.TORPEDO_BOMBER_DESCENT_START_DISTANCE.get());
            // 按当前高度反推「最晚何时开始下降」：下滑角固定 0.35（与 maxVertical/horizontal 同值），
            // 这样无论飞机是从 80 格外还是刚交战时的高空进入，都能在 runDistance 处刚好降到 runY。
            // 旧实现用固定的 80 格一刀切，飞机在 48~80 格才接敌时会来不及下降，只能贴着目标
            // 盘旋耗高度（turn_radius=2 时为半径 2 的紧螺旋），并把超时投雷打向身后。
            double requiredDescentStart = runDistance
                    + Math.max(0.0, position.y - runY) / 0.35;
            double descentStart = Math.max(fixedDescentStart, requiredDescentStart);
            double desiredAltitude;
            if (horizontalDistance >= descentStart) {
                desiredAltitude = approachY;
            } else {
                // 动态下滑线从「当前高度」出发；降到 approach_altitude 以下后改从该高度出发。
                // 取 max 让两条线在切换点连续，不会因为 topY 突变造成高度抽动。
                double topY = Math.max(approachY, position.y);
                double t = clamp01((horizontalDistance - runDistance)
                        / Math.max(1.0e-6, descentStart - runDistance));
                desiredAltitude = runY + (topY - runY) * t;
            }
            boolean atRunAltitude = position.y - runY <= tolerance;

            // 从远处正常接近越过入口距离的情况不算过近，只有开场就在入口以内才拉开。
            if (horizontalDistance >= runDistance) state.standoffReached = true;
            if (!state.standoffReached && horizontalDistance < runDistance) state.egressing = true;
            if (state.egressing && horizontalDistance >= runDistance && atRunAltitude) state.egressing = false;
            if (state.egressing) {
                tickEgress(context, position, target, dx, dz, horizontalDistance, phaseSpeed, runAltitude);
                return;
            }

            if (horizontalDistance <= runDistance && atRunAltitude
                    && context.attackCooldown() <= 0 && predictedAhead) {
                state.runStart = position;
                state.runTicks = 0;
            } else if (horizontalDistance <= runDistance
                    && (context.attackCooldown() > 0 || !atRunAltitude)) {
                // 已到航线入口但还在冷却/还没降到平飞高度：在入口外侧盘旋等待，
                // 不再继续向目标压近，否则会飞过头再从身后投雷。
                Vec3 radial = horizontalDistance > 0.01
                        ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1);
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
                double holdSpeed = Math.min(context.panelSpeed() * 0.5 * phaseSpeed, 0.8);
                double radialError = horizontalDistance - runDistance;
                double radialSpeed = Math.max(-holdSpeed * 0.5,
                        Math.min(holdSpeed * 0.5, radialError * 0.1));
                double yAdjust = smoothVertical(context,
                        (runY - position.y) * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get(),
                        maxVertical);
                context.setDeltaMovement(new Vec3(
                        tangent.x * holdSpeed + radial.x * radialSpeed,
                        yAdjust,
                        tangent.z * holdSpeed + radial.z * radialSpeed));
                return;
            } else {
                double altitudeDifference = position.y - desiredAltitude;
                if (altitudeDifference > tolerance) {
                    Vec3 toTarget = smoothApproachDirection(state, horizontalDistance > 0.01
                            ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1));
                    // 前馈项：按下滑线斜率直接给下降速度，避免只靠高度误差反馈导致
                    // 每 tick 在「下降/平飞」之间来回切换，看起来一抽一抽。
                    double glideTopY = Math.max(approachY, position.y);
                    double glideSlope = Math.max(0.0, Math.min(0.6,
                            (glideTopY - runY) / Math.max(1.0, descentStart - runDistance)));
                    double feedForward = -glideSlope * descentSpeed;
                    double desiredYAdjust = (desiredAltitude - position.y)
                            * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get() + feedForward;
                    double yAdjust = smoothVertical(context, desiredYAdjust, maxVertical);
                    context.setDeltaMovement(new Vec3(
                            toTarget.x * descentSpeed, yAdjust, toTarget.z * descentSpeed));
                    return;
                }
                Vec3 toTarget = smoothApproachDirection(state, new Vec3(dx, 0, dz).normalize()).scale(Math.min(context.panelSpeed()
                        * ModEquipmentConfig.TORPEDO_BOMBER_APPROACH_SPEED_MULTIPLIER.get()
                        * phaseSpeed, horizontalDistance));
                double yAdjust = (desiredAltitude - position.y)
                        * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
                context.setDeltaMovement(new Vec3(toTarget.x, yAdjust, toTarget.z));
                return;
            }
        }

        // 阶段 2：低空贴水面平飞，朝预测撞击点直飞。
        // 预瞄线只同步「目标当前水面投影」：客户端从飞机投影点连到目标投影点，
        // 和玩家鱼雷发射器的水面线同观感；实际投雷仍用本地含提前量的 predicted。
        context.setTorpedoAimPoint(new Vec3(target.getX(), smoothedSurfaceY, target.getZ()));
        state.runTicks++;
        double runAltitude = ModEquipmentConfig.TORPEDO_BOMBER_RUN_ALTITUDE.get();
        double desiredY = smoothedSurfaceY + runAltitude;
        double runAltitudeDifference = position.y - desiredY;
        boolean atRunAltitude = runAltitudeDifference
                <= ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get();
        boolean timeout = state.runTicks
                > ModEquipmentConfig.TORPEDO_BOMBER_RUN_TIMEOUT_TICKS.get();

        // 预测点必须在本机前方才允许投雷（predictedAhead 与进入航线共用）。
        // 目标横向机动/被超车时 toPredicted 会转到身后，旧实现照投不误，鱼雷出来就
        // 朝载机后方跑（玩家目击的「鱼雷反向」）。此时中断本轮，背向拉开后重新进入。
        if (!predictedAhead && (timeout || horizontalDistance
                <= ModEquipmentConfig.TORPEDO_BOMBER_RELEASE_DISTANCE.get())) {
            abortTorpedoRun(state);
            return;
        }

        if (predictedAhead
                && (horizontalDistance <= ModEquipmentConfig.TORPEDO_BOMBER_RELEASE_DISTANCE.get()
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
                // 伤害取载机面板伤害（JSON panel_damage，终端可调）：
                // 与水平轰炸/俯冲轰炸一致，鱼雷机的标伤就是单发雷的伤害，不再落到 torpedo.default_damage 兜底值。
                torpedo.setDamage(context.panelDamage());
                double offsetX = -direction.z * (i - (toFire - 1) / 2.0)
                        * ModEquipmentConfig.TORPEDO_BOMBER_SPREAD_OFFSET.get();
                double offsetZ = direction.x * (i - (toFire - 1) / 2.0)
                        * ModEquipmentConfig.TORPEDO_BOMBER_SPREAD_OFFSET.get();
                torpedo.moveTo(position.x + offsetX,
                        position.y - ModEquipmentConfig.TORPEDO_BOMBER_DROP_HEIGHT_OFFSET.get(),
                        position.z + offsetZ, 0, 0);
                torpedo.setSpeed((float) torpedoSpeed);
                // 航空鱼雷必须有独立寿命：不设置时会落回默认 1200 tick（60 秒），
                // 未命中的雷会一直跑到 170 格外才自毁。
                torpedo.setLifetime(ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_LIFETIME.get());
                torpedo.setAirDrop(true, direction);
                Vec3 velocity = direction.scale(torpedoSpeed)
                        .add(0, ModEquipmentConfig.TORPEDO_BOMBER_TORPEDO_VERTICAL_SPEED.get(), 0);
                torpedo.setDeltaMovement(velocity);
                torpedo.setOwner(owner);
                // 与战斗机子弹同口径：击杀/命中提示显示载机名，而不是笼统的「航空鱼雷」。
                torpedo.setSourceAircraft(context.self());
                torpedo.setSourceAircraftName(context.displayName());
                context.level().addFreshEntity(torpedo);
            }
            // 本轮把剩余弹全部投出（toFire = 全部剩余），投完必然空弹，直接返航；
            // 不需要再判一次剩余——那条分支永远为真，是死代码。
            context.setRemainingAmmo(0);
            context.setAttackCooldown(context.attackCooldownDuration());
            context.startReturning("torpedo_launched");
            return;
        }

        double runSpeed = Math.min(context.panelSpeed()
                * ModEquipmentConfig.TORPEDO_BOMBER_RUN_SPEED_MULTIPLIER.get()
                * phaseSpeed, Math.max(distanceToPredicted, 0.1));
        Vec3 runDirection = distanceToPredicted > 0.01 ? smoothApproachDirection(state, toPredicted.normalize())
                : (horizontalDistance > 0.01 ? new Vec3(dx, 0, dz).normalize() : new Vec3(0, 0, 1));
        double desiredYAdjust = (desiredY - position.y)
                * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
        if (runAltitudeDifference > ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_TOLERANCE.get()) {
            desiredYAdjust -= Math.min(runAltitudeDifference * 0.2,
                    ModEquipmentConfig.TORPEDO_BOMBER_DESCEND_MAX_SPEED.get());
        }
        double maxVertical = Math.max(0.05, runSpeed * 0.25);
        double yAdjust = smoothVertical(context, desiredYAdjust, maxVertical);
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
        double desiredYAdjust = (waterSurfaceY(context, target.getX(), target.getZ(), target.getY())
                + runAltitude - position.y)
                * ModEquipmentConfig.TORPEDO_BOMBER_ALTITUDE_ADJUST.get();
        double yAdjust = smoothVertical(context, desiredYAdjust, maxVertical);
        context.setDeltaMovement(new Vec3(
                outward.x * horizontalSpeed, yAdjust, outward.z * horizontalSpeed));
    }

    /**
     * 中断当前投雷航线：清掉平飞标记并转入背向拉开。
     * 下一 tick 会沿目标反方向飞出，等距离/高度重新满足入口条件后再进入，
     * 不会在原地以 turn_radius 为半径绕圈。
     */
    private static void abortTorpedoRun(TorpedoRunState state) {
        state.runStart = null;
        state.runTicks = 0;
        state.egressing = true;
        state.standoffReached = false;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** 下降控制使用一阶平滑，避免每 tick 直接反转垂直速度造成抽搐。 */
    private static double smoothVertical(AircraftAttackContext context, double desired, double max) {
        double blended = context.deltaMovement().y * 0.65 + desired * 0.35;
        return Math.max(-max, Math.min(max, blended));
    }

    private static Vec3 smoothApproachDirection(TorpedoRunState state, Vec3 desired) {
        if (desired.lengthSqr() < 1.0e-8) {
            return state.approachDirection == null ? new Vec3(0, 0, 1) : state.approachDirection;
        }
        desired = new Vec3(desired.x, 0, desired.z).normalize();
        if (state.approachDirection == null) {
            state.approachDirection = desired;
        } else {
            Vec3 blended = state.approachDirection.scale(0.82).add(desired.scale(0.18));
            state.approachDirection = blended.lengthSqr() < 1.0e-8
                    ? desired : new Vec3(blended.x, 0, blended.z).normalize();
        }
        return state.approachDirection;
    }

    /** 返回预测点所在列的水面高度；没有水时回退到目标高度，保持陆地目标兼容。 */
    private static double waterSurfaceY(AircraftAttackContext context, double x, double z, double fallback) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int centerY = (int) Math.floor(fallback);
        for (int y = centerY + 8; y >= centerY - 16; y--) {
            if (context.level().getFluidState(new BlockPos(blockX, y, blockZ)).is(Fluids.WATER)) {
                // 与 TorpedoEntity.STILL_WATER_SURFACE_HEIGHT 对齐：静止水面顶面在方块底边之上
                // 0.875 格，而不是整格 1.0。预瞄线/下滑线贴着真实水面，不会悬空 1/8 格。
                return y + 0.875;
            }
        }
        return fallback;
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
