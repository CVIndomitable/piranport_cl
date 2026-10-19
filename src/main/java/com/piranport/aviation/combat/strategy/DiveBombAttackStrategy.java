package com.piranport.aviation.combat.strategy;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.AttackStrategy;
import com.piranport.aviation.combat.DiveBombState;
import com.piranport.aviation.combat.TargetResolution;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AerialBombEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
            if (context.tryAutoResupplyAmmo()) {
                context.resumeCruising();
            } else {
                context.startReturning("dive_bomber_done");
            }
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

        if (!state.committed) {
            state.committed = true;
            Vec3 targetPosition = target.getEyePosition();
            double estimatedDistance = position.distanceTo(targetPosition);
            double diveSpeed = Math.max(context.panelSpeed()
                    * ModEquipmentConfig.DIVE_BOMBER_DIVE_SPEED_MULTIPLIER.get() * phaseSpeed, 0.1);
            double estimatedTicks = estimatedDistance / diveSpeed;
            state.target = targetPosition.add(target.getDeltaMovement().scale(estimatedTicks));
        }

        if (state.target == null) return;
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
        } else {
            double speed = Math.min(context.panelSpeed()
                    * ModEquipmentConfig.DIVE_BOMBER_DIVE_APPROACH_MULTIPLIER.get()
                    * phaseSpeed, diveDistance);
            context.setDeltaMovement(toDive.normalize().scale(speed));
        }
    }
}
