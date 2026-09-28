package com.piranport.terminal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.WeakHashMap;

/** Applies terminal overrides to deep-ocean, ship-girl, and vanilla living entities. */
public final class TerminalEntityAttributeOverrides {
    private static final Map<LivingEntity, double[]> BASE_VALUES = new WeakHashMap<>();

    private TerminalEntityAttributeOverrides() {}

    public static void apply(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) return;
        String group = group(id);
        String prefix = group + "." + id + ".";
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance follow = entity.getAttribute(Attributes.FOLLOW_RANGE);
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        double[] base = BASE_VALUES.computeIfAbsent(entity, ignored -> new double[] {
                base(health, 20), base(speed, 0.1), base(attack, 2), base(follow, 16), base(armor, 0)
        });
        set(health, TerminalParameters.getDouble(prefix + "max_health", base[0]));
        set(speed, TerminalParameters.getDouble(prefix + "movement_speed", base[1]));
        set(attack, TerminalParameters.getDouble(prefix + "attack_damage", base[2]));
        set(follow, TerminalParameters.getDouble(prefix + "follow_range", base[3]));
        set(armor, TerminalParameters.getDouble(prefix + "armor", base[4]));
        if (health != null) entity.setHealth(Math.min(entity.getHealth(), (float) health.getValue()));
    }

    private static double base(AttributeInstance instance, double fallback) {
        return instance == null ? fallback : instance.getBaseValue();
    }

    private static void set(AttributeInstance instance, double value) {
        if (instance != null && Double.isFinite(value) && value >= 0
                && Math.abs(instance.getBaseValue() - value) > 1.0e-9) {
            instance.setBaseValue(value);
        }
    }

    private static String group(ResourceLocation id) {
        if ("minecraft".equals(id.getNamespace())) return "vanilla_mob";
        String path = id.getPath();
        if ("ship_girl".equals(path)) return "ship_girl";
        if (path.startsWith("deep_ocean_") || "low_tier_destroyer".equals(path)
                || "goldencatcat".equals(path)) return "deep_ocean";
        return "vanilla_mob";
    }

}
