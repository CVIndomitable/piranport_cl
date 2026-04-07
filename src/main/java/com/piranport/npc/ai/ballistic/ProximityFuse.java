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
        if (projectile.tickCount < armTicks) return false;

        AABB searchBox = projectile.getBoundingBox().inflate(detonateRange);
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
            double dist = entity.position().add(0, entity.getBbHeight() * 0.5, 0).distanceTo(pos);
            if (dist <= detonateRange) {
                return true;
            }
        }
        return false;
    }
}
