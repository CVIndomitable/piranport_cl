package com.piranport.combat;

import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Shared friendly-fire logic for all piranport projectiles.
 * Returns true if the projectile should skip hitting the given target.
 */
public final class FriendlyFireHelper {

    private FriendlyFireHelper() {}

    /**
     * @param target   the entity the projectile is about to hit
     * @param owner    the entity that fired the projectile (may be null)
     * @return true if the hit should be blocked (target is friendly)
     */
    public static boolean shouldBlockHit(Entity target, Entity owner) {
        // Friendly fire protection: skip other players when config disabled
        if (!ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()
                && target instanceof Player && owner instanceof Player) {
            return true;
        }
        // Don't hit any player-owned aircraft when friendly fire is disabled
        if (target instanceof AircraftEntity aircraft && aircraft.getOwnerUUID() != null) {
            if (owner instanceof Player p && p.getUUID().equals(aircraft.getOwnerUUID())) {
                return true; // always protect own aircraft
            }
            if (!ModCommonConfig.FRIENDLY_FIRE_ENABLED.get() && owner instanceof Player) {
                return true; // protect other players' aircraft when FF disabled
            }
        }
        return false;
    }
}
