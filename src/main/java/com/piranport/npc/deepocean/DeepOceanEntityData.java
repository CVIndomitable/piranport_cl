package com.piranport.npc.deepocean;

import com.google.gson.JsonObject;

/**
 * 深海 NPC 数据记录。依据：策划决策/架构/03-数据驱动vs硬编码.md
 *
 * <p>字段名与 JSON 一致；调用方通过 {@link DeepOceanDataLoader#get} 拿到此对象。</p>
 */
public record DeepOceanEntityData(
        String entityType,
        /** 最大血量；null = 用子类 createAttributes() 的兜底值。 */
        Double maxHealth,
        /** 移动速度；null = 用子类兜底值。 */
        Double movementSpeed,
        Double followRange,
        double attackDamage,
        double knockbackResistance,
        /** 护甲；null = 用子类兜底值。护甲不受副本 difficulty_scale 影响。 */
        Double armor,
        float shellDamage,
        float explosionPower,
        int fireInterval,
        double orbitDistance,
        Double torpedoRange,
        /** 鱼雷装填时间（tick）；null = 用全局 {@code torpedo_salvo.salvo_cooldown} 兜底。 */
        Integer torpedoCooldown,
        boolean canUseTorpedoes,
        float torpedoDamage,
        int trackingIntervalMin,
        int trackingIntervalMax,
        boolean canLaunchAircraft,
        int maxAircraft,
        /** 防空对空弹伤害；仅防空类实体（高级轻巡火炮、防驱导弹）读取。 */
        float aaDamage,
        /** 防空发射间隔（tick）；仅防空类实体读取。 */
        int aaFireInterval
) {
    public static DeepOceanEntityData fromJson(JsonObject obj) {
        return new DeepOceanEntityData(
                optString(obj, "entity_type", "unknown"),
                optNullableDouble(obj, "max_health"),
                optNullableDouble(obj, "movement_speed"),
                optNullableDouble(obj, "follow_range"),
                optDouble(obj, "attack_damage", 5.0),
                optDouble(obj, "knockback_resistance", 0.6),
                optNullableDouble(obj, "armor"),
                optFloat(obj, "shell_damage", 5.0f),
                optFloat(obj, "explosion_power", 1.5f),
                optInt(obj, "fire_interval", 80),
                optDouble(obj, "orbit_distance", 16.0),
                optNullableDouble(obj, "torpedo_range"),
                optNullableInt(obj, "torpedo_cooldown"),
                optBool(obj, "can_use_torpedoes", false),
                optFloat(obj, "torpedo_damage", 8.0f),
                optInt(obj, "tracking_interval_min", 3),
                optInt(obj, "tracking_interval_max", 6),
                optBool(obj, "can_launch_aircraft", false),
                optInt(obj, "max_aircraft", 0),
                optFloat(obj, "aa_damage", 6.0f),
                optInt(obj, "aa_fire_interval", 80)
        );
    }

    private static String optString(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }
    /** 可空数值：字段缺失时返回 null，供调用方保留子类硬编码兜底值。 */
    private static Double optNullableDouble(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : null;
    }
    private static Integer optNullableInt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : null;
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