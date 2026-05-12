package com.piranport.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side cache mapping UUID→Entity via WeakReference.
 * Avoids scanning all entities every tick when only a known set of targets needs highlighting.
 * On cache miss, iterates the entity list once to find the UUID.
 * Entries are invalidated when the entity is garbage-collected or no longer alive.
 */
public class EntityUuidCache {
    private final Map<UUID, WeakReference<Entity>> cache = new HashMap<>();

    public Entity get(Level level, UUID uuid) {
        WeakReference<Entity> ref = cache.get(uuid);
        Entity entity = ref != null ? ref.get() : null;
        if (entity == null || !entity.isAlive()) {
            entity = findEntityByUuid(level, uuid);
            if (entity != null) {
                cache.put(uuid, new WeakReference<>(entity));
            } else {
                cache.remove(uuid);
            }
        }
        return entity;
    }

    public void clear() {
        cache.clear();
    }

    private static Entity findEntityByUuid(Level level, UUID uuid) {
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel cl) {
            for (Entity e : cl.entitiesForRendering()) {
                if (e.getUUID().equals(uuid)) return e;
            }
        }
        return null;
    }
}
