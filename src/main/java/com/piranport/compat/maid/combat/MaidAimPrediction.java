package com.piranport.compat.maid.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 女仆直射武器的轻量提前量估算。弹速单位为方块/tick。 */
public final class MaidAimPrediction {
    private MaidAimPrediction() {}

    public static Vec3 predict(LivingEntity target, Vec3 origin, double projectileSpeed) {
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 velocity = target.getDeltaMovement();
        double speed = Double.isFinite(projectileSpeed) ? Math.max(0.02, projectileSpeed) : 0.5;
        double flightTicks = Math.min(40.0, origin.distanceTo(aim) / speed);
        if (!Double.isFinite(velocity.x) || !Double.isFinite(velocity.y) || !Double.isFinite(velocity.z)
                || velocity.lengthSqr() > 16.0) {
            velocity = Vec3.ZERO;
        }
        return aim.add(velocity.scale(flightTicks));
    }
}
