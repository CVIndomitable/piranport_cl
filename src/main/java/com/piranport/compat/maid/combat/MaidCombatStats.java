package com.piranport.compat.maid.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MaidCombatStats {
    private static final Map<UUID, Stats> STATS_MAP = new ConcurrentHashMap<>();

    public static class Stats {
        public int kills = 0;
        public float totalDamage = 0f;
        public int shotsFired = 0;

        public void reset() {
            kills = 0;
            totalDamage = 0f;
            shotsFired = 0;
        }
    }

    public static Stats get(LivingEntity maid) {
        return STATS_MAP.computeIfAbsent(maid.getUUID(), k -> new Stats());
    }

    public static void recordKill(LivingEntity maid) {
        get(maid).kills++;
    }

    public static void recordDamage(LivingEntity maid, float damage) {
        get(maid).totalDamage += damage;
    }

    public static void recordShot(LivingEntity maid) {
        get(maid).shotsFired++;
    }

    public static void reset(LivingEntity maid) {
        Stats stats = STATS_MAP.get(maid.getUUID());
        if (stats != null) stats.reset();
    }

    public static void remove(UUID uuid) {
        STATS_MAP.remove(uuid);
    }
}
