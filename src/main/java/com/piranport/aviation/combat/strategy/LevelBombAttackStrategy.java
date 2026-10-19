package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.aviation.combat.LevelBombState;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.combat.LevelBombLead;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AerialBombEntity;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 水平轰炸策略（profile=level_bomb）。
 *
 * <p>保留前抛：投弹时机由 {@link LevelBombLead#releaseDistanceToMovingTarget} 按真实
 * 弹道反推，不再使用固定距离。
 */
public final class LevelBombAttackStrategy implements AttackStrategy {
    @Override public String id() { return AircraftDefinition.AttackProfile.LEVEL_BOMB.id(); }
    @Override public AircraftDefinition.AttackProfile profile() { return AircraftDefinition.AttackProfile.LEVEL_BOMB; }

    @Override
    public void execute(AircraftAttackContext context, @Nullable Player owner) {
        LivingEntity target = TargetResolution.resolveGroundTarget(context, owner);
        if (target == null) {
            if (context.stateTicks() >= ModEquipmentConfig.AIRCRAFT_ATTACK_FALLBACK_TICKS.get()) {
                context.resumeCruising();
            }
            return;
        }
        tickLevel(context, owner, target);
    }

    @Override
    public void executeAutonomous(AircraftAttackContext context) {
        LivingEntity target = context.autonomousTarget();
        if (target == null) {
            context.startReturning("target_lost");
            return;
        }
        tickLevel(context, null, target);
    }

    private static void tickLevel(AircraftAttackContext context, @Nullable Player owner,
                                  LivingEntity target) {
        if (context.remainingAmmo() <= 0) {
            if (context.tryAutoResupplyAmmo()) {
                context.resumeCruising();
            } else {
                context.startReturning("level_bomber_ammo_depleted");
            }
            return;
        }

        LevelBombState state = context.levelBombState();
        Vec3 position = context.position();
        double dx = target.getX() - position.x;
        double dz = target.getZ() - position.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double bombAltitude = target.getY() + ModEquipmentConfig.LEVEL_BOMBER_ALTITUDE_OFFSET.get();

        // Phase 1: 爬升到投弹高度
        if (position.y < bombAltitude - ModEquipmentConfig.LEVEL_BOMBER_ALTITUDE_TOLERANCE.get()) {
            Vec3 toAltitude = new Vec3(dx, bombAltitude - position.y, dz);
            context.setDeltaMovement(toAltitude.normalize().scale(Math.min(context.panelSpeed()
                    * ModEquipmentConfig.LEVEL_BOMBER_CLIMB_SPEED_MULTIPLIER.get()
                    * context.phaseSpeedCoefficient(), toAltitude.length())));
            return;
        }

        // Phase 2a: 移动目标先绕到其运动方向后方
        if (!state.rearApproachDone) {
            Vec3 targetVelocity = new Vec3(target.getDeltaMovement().x, 0,
                    target.getDeltaMovement().z);
            if (targetVelocity.lengthSqr() < 1.0e-4) {
                state.rearApproachDone = true;
            } else {
                Vec3 targetDirection = targetVelocity.normalize();
                double rearDistance = ModEquipmentConfig.LEVEL_BOMBER_REAR_APPROACH_DISTANCE.get();
                Vec3 rearPoint = new Vec3(target.getX() - targetDirection.x * rearDistance,
                        bombAltitude,
                        target.getZ() - targetDirection.z * rearDistance);
                Vec3 toRear = rearPoint.subtract(position);
                double runSpeedNow = context.panelSpeed()
                        * ModEquipmentConfig.LEVEL_BOMBER_RUN_SPEED_MULTIPLIER.get()
                        * context.phaseSpeedCoefficient();
                if (toRear.length() <= Math.max(runSpeedNow / 3.0, 2.0)) {
                    state.rearApproachDone = true;
                } else {
                    context.setDeltaMovement(toRear.normalize().scale(
                            Math.min(runSpeedNow, toRear.length())));
                    return;
                }
            }
        }

        // Phase 2b: 锁定航线并执行投弹
        if (state.runDirection == null) {
            state.runDirection = new Vec3(dx, 0, dz).normalize();
            state.runTicks = 0;
            state.abortTicks = 0;
        }

        double runSpeed = context.panelSpeed()
                * ModEquipmentConfig.LEVEL_BOMBER_RUN_SPEED_MULTIPLIER.get()
                * context.phaseSpeedCoefficient();
        Vec3 runDirection = state.runDirection;
        Vec3 runVelocity = new Vec3(runDirection.x * runSpeed, 0, runDirection.z * runSpeed);

        // 侧偏修正：垂直 runDir、指向目标，逐步清零侧偏
        Vec3 toTargetHorizontal = new Vec3(dx, 0, dz);
        Vec3 lateral = toTargetHorizontal.subtract(runDirection.scale(toTargetHorizontal.dot(runDirection)));
        double lateralDistance = lateral.length();
        if (lateralDistance > 0.01) {
            double correctionSpeed = Math.min(lateralDistance * 0.5, runSpeed * 0.3);
            runVelocity = runVelocity.add(lateral.normalize().scale(correctionSpeed));
        }
        context.setDeltaMovement(runVelocity);
        state.runTicks++;

        if (!state.dropped) {
            Vec3 delta = context.deltaMovement();
            double craftHorizontalSpeed = delta.x * runDirection.x + delta.z * runDirection.z;
            double horizontalMultiplier =
                    ModEquipmentConfig.LEVEL_BOMBER_HORIZONTAL_VELOCITY_MULTIPLIER.get();
            double dropYOffset = ModEquipmentConfig.LEVEL_BOMBER_DROP_Y_OFFSET.get();
            double verticalVelocity = ModEquipmentConfig.LEVEL_BOMBER_VERTICAL_VELOCITY.get();
            double dropHeight = (position.y - dropYOffset) - target.getY();
            Vec3 targetVelocity = target.getDeltaMovement();
            double targetVelocityAlongRun = targetVelocity.x * runDirection.x
                    + targetVelocity.z * runDirection.z;
            double leadDistance = LevelBombLead.releaseDistanceToMovingTarget(
                    dropHeight, AerialBombEntity.gravity(),
                    craftHorizontalSpeed * horizontalMultiplier, verticalVelocity,
                    targetVelocityAlongRun);

            Vec3 toTarget = new Vec3(dx, 0, dz);
            boolean inRunCone = horizontalDistance < 1.0
                    || toTarget.normalize().dot(runDirection) >= 0.9;
            if (!inRunCone) {
                state.abortTicks++;
            } else {
                state.abortTicks = 0;
            }
            if (state.abortTicks >= ModEquipmentConfig.LEVEL_BOMBER_ABORT_TICKS.get()) {
                state.runDirection = null;
                state.dropped = false;
                state.abortTicks = 0;
                state.rearApproachDone = false;
                context.setDeltaMovement(new Vec3(runDirection.x * runSpeed,
                        context.panelSpeed()
                                * ModEquipmentConfig.LEVEL_BOMBER_CLIMB_SPEED_MULTIPLIER.get()
                                * context.phaseSpeedCoefficient(),
                        runDirection.z * runSpeed));
                return;
            }

            if (inRunCone
                    && (horizontalDistance <= Math.max(leadDistance,
                    ModEquipmentConfig.LEVEL_BOMBER_MIN_RELEASE_DISTANCE.get())
                    || state.runTicks > ModEquipmentConfig.LEVEL_BOMBER_RUN_TIMEOUT_TICKS.get())) {
                // 副本/08：核航弹的威力是同机普通航弹的 10 倍。倍率在投弹时一次性写进航弹，
                // 命中阶段不再乘系数；伤害、方块破坏与水中规则仍由原版爆炸统一处理。
                boolean nuclear = context.nuclearPayload();
                float powerMultiplier = nuclear
                        ? ModEquipmentConfig.LEVEL_BOMBER_NUCLEAR_POWER_MULTIPLIER.get().floatValue()
                        : 1.0f;
                float bombPower = context.panelDamage() * powerMultiplier;
                float explosionPower = ModEquipmentConfig.LEVEL_BOMBER_EXPLOSION_POWER.get().floatValue()
                        * powerMultiplier;
                int bombCount = Math.max(1, context.remainingAmmo());
                for (int i = 0; i < bombCount; i++) {
                    AerialBombEntity bomb = new AerialBombEntity(context.level(), bombPower / bombCount,
                            explosionPower);
                    if (nuclear) {
                        // 视觉与命中提示都按核航弹显示；投掷物默认物品是普通航弹。
                        bomb.setItem(new ItemStack(ModItems.NUCLEAR_AERIAL_BOMB.get()));
                    }
                    double spreadX = (context.level().random.nextDouble() - 0.5)
                            * ModEquipmentConfig.LEVEL_BOMBER_BOMB_SPREAD.get();
                    double spreadZ = (context.level().random.nextDouble() - 0.5)
                            * ModEquipmentConfig.LEVEL_BOMBER_BOMB_SPREAD.get();
                    bomb.moveTo(position.x + spreadX, position.y - dropYOffset,
                            position.z + spreadZ, 0, 0);
                    bomb.setDeltaMovement(
                            runDirection.x * craftHorizontalSpeed * horizontalMultiplier,
                            verticalVelocity,
                            runDirection.z * craftHorizontalSpeed * horizontalMultiplier);
                    bomb.setOwner(owner);
                    context.level().addFreshEntity(bomb);
                }
                context.setRemainingAmmo(0);
                state.dropped = true;
                state.dropPoint = context.position();
            }
        }

        if (state.dropped
                && state.runTicks > ModEquipmentConfig.LEVEL_BOMBER_RETURN_TICKS.get()) {
            context.startReturning("level_bomber_run_complete");
        }
    }
}
