package com.piranport.npc.ai.ballistic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Proportional Navigation guidance for tracking projectiles.
 * The projectile starts tracking after passing its apex (highest point).
 * Turn rate = N × LOS angular rate, with max-g clamp for natural-looking curves.
 */
public final class TrackingCalculator {

    private TrackingCalculator() {}

    /** Navigation constant (dimensionless). Higher = more aggressive tracking. */
    private static final double NAV_CONSTANT = 3.0;
    /** Max turn rate per tick (radians). Prevents unnatural 90° turns. */
    private static final double MAX_TURN_RATE = 0.08; // ~4.6 degrees/tick

    /**
     * Adjust velocity of a projectile to track a target using proportional navigation.
     * Should only be called after the projectile has passed its apex.
     *
     * @param projectilePos current projectile position
     * @param currentVel    current projectile velocity
     * @param target        the target entity
     * @return adjusted velocity vector (same speed, new direction)
     */
    public static Vec3 steer(Vec3 projectilePos, Vec3 currentVel, Entity target) {
        double speed = currentVel.length();
        if (speed < 0.1) return currentVel;

        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 los = targetCenter.subtract(projectilePos); // Line Of Sight
        double losDist = los.length();
        if (losDist < 0.5) return currentVel; // Very close, don't adjust

        Vec3 losDir = los.normalize();
        Vec3 velDir = currentVel.normalize();

        // Calculate the angle between current velocity and LOS
        double dot = velDir.dot(losDir);
        dot = Math.max(-1.0, Math.min(1.0, dot)); // clamp for acos safety
        double angle = Math.acos(dot);

        // Proportional navigation: desired turn = N * angle * (1/time_to_intercept)
        double timeToIntercept = losDist / speed;
        double desiredTurn = NAV_CONSTANT * angle / Math.max(timeToIntercept, 1.0);

        // Clamp turn rate
        double actualTurn = Math.min(desiredTurn, MAX_TURN_RATE);

        if (angle < 0.001) return currentVel; // Already on target

        // Calculate rotation axis (cross product of velDir and losDir)
        Vec3 rotAxis = velDir.cross(losDir);
        double axisLen = rotAxis.length();
        if (axisLen < 0.0001) {
            // Parallel — tiny random perturbation to break symmetry
            return currentVel;
        }
        rotAxis = rotAxis.normalize();

        // Apply rotation using Rodrigues' formula
        Vec3 newDir = rotateAround(velDir, rotAxis, actualTurn);
        return newDir.scale(speed);
    }

    /**
     * Check if a projectile has passed its apex (highest point of trajectory).
     */
    public static boolean hasPassedApex(Vec3 currentVel, Vec3 prevVel) {
        // Apex is when vertical velocity transitions from positive to non-positive
        return prevVel.y > 0 && currentVel.y <= 0;
    }

    /**
     * Rotate vector v around axis by angle (radians) using Rodrigues' rotation formula.
     */
    private static Vec3 rotateAround(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // v_rot = v*cos + (axis×v)*sin + axis*(axis·v)*(1-cos)
        Vec3 cross = axis.cross(v);
        double dot = axis.dot(v);
        return v.scale(cos).add(cross.scale(sin)).add(axis.scale(dot * (1 - cos)));
    }
}
