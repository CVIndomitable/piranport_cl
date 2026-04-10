package com.piranport.npc.ai.ballistic;

import net.minecraft.world.phys.Vec3;

/**
 * Calculates initial velocity for a parabolic (lobbed) projectile to hit a moving target.
 * Uses lead prediction for uniformly moving targets.
 */
public final class ParabolicCalculator {

    private ParabolicCalculator() {}

    /** Default gravity for projectiles (blocks/tick²). */
    private static final double GRAVITY = 0.05;

    /**
     * Calculate initial velocity vector for a parabolic shot.
     *
     * @param from         launch position
     * @param targetPos    current target position
     * @param targetVel    target velocity (blocks/tick), used for lead prediction
     * @param speed        desired launch speed (blocks/tick)
     * @param minArcHeight minimum arc height (blocks) at close range
     * @param maxArcHeight maximum arc height (blocks) at max range
     * @param maxRange     max range for arc interpolation
     * @return initial velocity vector, or null if no valid solution
     */
    public static Vec3 calculate(Vec3 from, Vec3 targetPos, Vec3 targetVel,
                                  double speed, double minArcHeight, double maxArcHeight,
                                  double maxRange) {
        // Lead prediction: estimate flight time, then predict target position
        double hDist = horizontalDistance(from, targetPos);
        if (hDist < 0.5) {
            return new Vec3(0, 0.5, 0);
        }
        double estimatedFlightTime = hDist / speed; // rough estimate
        Vec3 predictedPos = targetPos.add(targetVel.scale(estimatedFlightTime));

        // Recalculate horizontal distance to predicted position
        double dx = predictedPos.x - from.x;
        double dz = predictedPos.z - from.z;
        double hDistPredicted = Math.sqrt(dx * dx + dz * dz);
        double dy = predictedPos.y - from.y;

        // Interpolate arc height based on distance
        double t = Math.min(hDistPredicted / maxRange, 1.0);
        double arcHeight = minArcHeight + (maxArcHeight - minArcHeight) * t;

        // Calculate launch angle to achieve the desired arc
        // Using simplified trajectory: vy = (dy + arcHeight) / flightTime
        // vx/vz split from horizontal component
        double flightTime = hDistPredicted / speed;
        if (flightTime < 1) flightTime = 1; // minimum 1 tick

        double vy = (dy + arcHeight + 0.5 * GRAVITY * flightTime * flightTime) / flightTime;
        double hSpeed = hDistPredicted / flightTime;

        // Normalize horizontal direction
        if (hDistPredicted < 0.01) {
            return new Vec3(0, vy, 0);
        }

        double dirX = dx / hDistPredicted;
        double dirZ = dz / hDistPredicted;

        return new Vec3(dirX * hSpeed, vy, dirZ * hSpeed);
    }

    /**
     * Simplified version without lead prediction.
     */
    public static Vec3 calculate(Vec3 from, Vec3 targetPos, double speed,
                                  double minArcHeight, double maxArcHeight, double maxRange) {
        return calculate(from, targetPos, Vec3.ZERO, speed, minArcHeight, maxArcHeight, maxRange);
    }

    /**
     * Get the horizontal distance between two positions.
     */
    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
