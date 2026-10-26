package com.piranport.npc.ai.ballistic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Proximity fuze logic for deep ocean projectiles.
 * Detonates when a target entity is within the configured range.
 */
public final class ProximityFuse {

    private ProximityFuse() {}

    /**
     * Check if any valid target is within detonation range.
     *
     * @param projectile    the projectile entity
     * @param detonateRange detonation distance (blocks)
     * @param armTicks      ticks before fuze arms (grace period after launch)
     * @return true if should detonate
     */
    public static boolean shouldDetonate(Entity projectile, double detonateRange, int armTicks) {
        return shouldDetonate(projectile, projectile.position(), detonateRange, armTicks);
    }

    /**
     * 带「上一 tick 位置」的版本：按本 tick 的位移线段判距，而不是只看当前点。
     *
     * <p>WHY：防空弹初速 2.8~3.0 格/tick，比近炸球半径（3.0）还大。只采样 tick 端点时，
     * 弹道可能从球内穿过而两个端点都在球外（步长 &gt; 2×√(r²−b²) 时发生），表现为
     * 「明明擦着飞机过去却不爆」。这在对高速目标射击时是系统性漏爆，不是散布问题。</p>
     *
     * @param previousPos 上一 tick 结束时（= 本 tick 移动前）的位置
     */
    public static boolean shouldDetonate(Entity projectile, Vec3 previousPos, double detonateRange, int armTicks) {
        if (projectile.tickCount < armTicks) return false;

        // 搜索盒要覆盖整段位移，否则线段起点附近的目标会被提前裁掉。
        AABB searchBox = projectile.getBoundingBox()
                .expandTowards(previousPos.subtract(projectile.position()))
                .inflate(detonateRange);
        List<Entity> nearby = projectile.level().getEntities(projectile, searchBox, e -> {
            // Don't detonate on owner or other deep ocean entities
            if (e instanceof com.piranport.npc.deepocean.AbstractDeepOceanEntity) return false;
            Entity owner = null;
            if (projectile instanceof net.minecraft.world.entity.projectile.Projectile proj) {
                owner = proj.getOwner();
            }
            if (e == owner) return false;
            return e.isAlive() && e.isPickable();
        });

        Vec3 pos = projectile.position();
        for (Entity entity : nearby) {
            Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            if (distanceToSegment(center, previousPos, pos) <= detonateRange) {
                return true;
            }
        }
        return false;
    }

    /** 点到线段的最短距离。 */
    public static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-9) return point.distanceTo(end);
        double t = point.subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        return point.distanceTo(start.add(segment.scale(t)));
    }
}
