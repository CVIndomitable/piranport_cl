package com.piranport.npc.deepocean;

import com.google.gson.JsonObject;

/**
 * 深海 NPC 数据记录。依据：策划决策/架构/03-数据驱动vs硬编码.md
 *
 * <p>字段名与 JSON 一致；调用方通过 {@link DeepOceanDataLoader#get} 拿到此对象。</p>
 */
public record DeepOceanEntityData(
        String entityType,
        double maxHealth,
        double movementSpeed,
        double followRange,
        double attackDamage,
        double knockbackResistance,
        double armor,
        float shellDamage,
        float explosionPower,
        int fireInterval,
        double orbitDistance,
        boolean canUseTorpedoes,
        float torpedoDamage,
        int trackingIntervalMin,
        int trackingIntervalMax,
        boolean canLaunchAircraft,
        int maxAircraft
) {
    public static DeepOceanEntityData fromJson(JsonObject obj) {
        return new DeepOceanEntityData(
                optString(obj, "entity_type", "unknown"),
                optDouble(obj, "max_health", 50.0),
                optDouble(obj, "movement_speed", 0.25),
                optDouble(obj, "follow_range", 32.0),
                optDouble(obj, "attack_damage", 5.0),
                optDouble(obj, "knockback_resistance", 0.6),
                optDouble(obj, "armor", 8.0),
                optFloat(obj, "shell_damage", 5.0f),
                optFloat(obj, "explosion_power", 1.5f),
                optInt(obj, "fire_interval", 80),
                optDouble(obj, "orbit_distance", 16.0),
                optBool(obj, "can_use_torpedoes", false),
                optFloat(obj, "torpedo_damage", 8.0f),
                optInt(obj, "tracking_interval_min", 3),
                optInt(obj, "tracking_interval_max", 6),
                optBool(obj, "can_launch_aircraft", false),
                optInt(obj, "max_aircraft", 0)
        );
    }

    private static String optString(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }
    private static double optDouble(JsonObject o, String k, double d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : d;
    }
    private static float optFloat(JsonObject o, String k, float d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsFloat() : d;
    }
    private static int optInt(JsonObject o, String k, int d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : d;
    }
    private static boolean optBool(JsonObject o, String k, boolean d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : d;
    }
}