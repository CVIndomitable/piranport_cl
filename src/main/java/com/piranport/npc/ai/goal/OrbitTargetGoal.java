package com.piranport.npc.ai.goal;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Two-phase movement: chase when far, orbit when in range.
 * Smooth velocity interpolation on phase transitions (~1 second).
 * Triggers attack goals when target is in firing range.
 */
public class OrbitTargetGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private final double surfaceSpeed;

    private double orbitAngle;
    /** Ticks remaining for velocity interpolation on phase change. */
    private int transitionTicks = 0;
    private static final int TRANSITION_DURATION = 20; // 1 second
    private Vec3 prevMovement = Vec3.ZERO;

    private enum Phase { CHASE, ORBIT, RETREAT }
    private Phase currentPhase = Phase.CHASE;

    public OrbitTargetGoal(AbstractDeepOceanEntity mob, double surfaceSpeed) {
        this.mob = mob;
        this.surfaceSpeed = surfaceSpeed;
        this.orbitAngle = mob.getRandom().nextDouble() * Math.PI * 2;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        currentPhase = Phase.CHASE;
        transitionTicks = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        Vec3 toTarget = target.position().subtract(mob.position());
        double hDist = toTarget.horizontalDistance();
        double orbitDist = mob.getOrbitDistance();

        // Determine phase
        Phase newPhase;
        if (hDist > orbitDist * 1.2) {
            newPhase = Phase.CHASE;
        } else if (hDist < orbitDist * 0.7) {
            newPhase = Phase.RETREAT;
        } else {
            newPhase = Phase.ORBIT;
        }

        // Phase transition — start interpolation
        if (newPhase != currentPhase) {
            prevMovement = mob.getDeltaMovement();
            transitionTicks = TRANSITION_DURATION;
            currentPhase = newPhase;
        }

        Vec3 rawMovement;
        switch (currentPhase) {
            case CHASE -> {
                Vec3 dir = toTarget.normalize();
                rawMovement = new Vec3(dir.x * surfaceSpeed, 0, dir.z * surfaceSpeed);
            }
            case RETREAT -> {
                Vec3 dir = toTarget.normalize();
                rawMovement = new Vec3(-dir.x * surfaceSpeed * 0.8, 0, -dir.z * surfaceSpeed * 0.8);
            }
            case ORBIT -> {
                // Recalculate orbit angle every 10-20 ticks for performance
                orbitAngle += 0.02; // ~0.02 radians/tick
                double ox = target.getX() + Math.cos(orbitAngle) * orbitDist;
                double oz = target.getZ() + Math.sin(orbitAngle) * orbitDist;
                Vec3 toOrbit = new Vec3(ox - mob.getX(), 0, oz - mob.getZ());
                double oDist = toOrbit.horizontalDistance();
                if (oDist > 0.1) {
                    rawMovement = new Vec3(toOrbit.x / oDist * surfaceSpeed, 0,
                            toOrbit.z / oDist * surfaceSpeed);
                } else {
                    rawMovement = Vec3.ZERO;
                }
            }
            default -> rawMovement = Vec3.ZERO;
        }

        // Smooth interpolation during phase transition
        Vec3 finalMovement;
        if (transitionTicks > 0) {
            double t = 1.0 - (double) transitionTicks / TRANSITION_DURATION;
            finalMovement = prevMovement.lerp(rawMovement, t);
            transitionTicks--;
        } else {
            finalMovement = rawMovement;
        }

        // Apply horizontal movement, keep vertical from physics
        Vec3 current = mob.getDeltaMovement();
        mob.setDeltaMovement(finalMovement.x, current.y, finalMovement.z);
    }
}
