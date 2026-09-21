package com.piranport.client;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 瞄准射线取最近的实际交点；独立于客户端世界，便于验证实体和水面遮挡。 */
final class ScopeTargeting {
    private ScopeTargeting() {}

    record Target(Vec3 position, boolean entity, boolean valid) {}

    static Target pick(Vec3 start, Vec3 end, HitResult surfaceHit, Iterable<AABB> entityBounds) {
        boolean hitSurface = surfaceHit.getType() == HitResult.Type.BLOCK;
        Target nearest = new Target(hitSurface ? surfaceHit.getLocation() : end, false, hitSurface);
        double nearestDistance = start.distanceToSqr(nearest.position());

        for (AABB bounds : entityBounds) {
            // 原版带 Level 参数的 ProjectileUtil 重载只返回实体脚底坐标，不能用于火控落点。
            Vec3 intersection = bounds.contains(start) ? start : bounds.clip(start, end).orElse(null);
            if (intersection == null) continue;
            double distance = start.distanceToSqr(intersection);
            if (distance < nearestDistance) {
                nearest = new Target(intersection, true, true);
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
