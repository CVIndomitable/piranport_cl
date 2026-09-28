package com.piranport.terminal;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.aviation.ResolvedAircraftStats;
import com.piranport.combat.cannon.ammo.AmmoDefinition;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.TerminalConfigValue;
import com.piranport.item.ShipType;
import com.piranport.item.TorpedoItem;
import com.piranport.item.RadarItem;
import com.piranport.item.SonarItem;
import com.piranport.item.FireControlRadarItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** 从当前数据包定义和代码默认值生成终端目录；重载后下次查询立即反映新基准。 */
public final class TerminalParameterCatalog {
    private TerminalParameterCatalog() {}

    public static Collection<TerminalParameterSpec> all() {
        Map<String, TerminalParameterSpec> specs = new LinkedHashMap<>();
        // 初始化仍被玩法读取的通用配置默认值；旧六舰种表没有玩法消费者。
        Object ignored = ModCommonConfig.WATER_WALKING_ACCELERATION;
        ignored = ModEquipmentConfig.SONAR_RANGE;
        ignored = ModArtilleryConfig.ARTILLERY_MAX_PROJECTILES;
        ignored = ModProjectilesConfig.AP_DAMAGE_MULTIPLIER;
        for (TerminalParameterSpec spec : TerminalConfigValue.specs()) {
            if (!spec.key().startsWith("global.ships.")) {
                specs.put(spec.key(), spec);
            }
        }

        for (String id : ArtilleryConfig.getAllCannonNames()) {
            ArtilleryCannonData c = ArtilleryConfig.get(id);
            String target = id;
            add(specs, "cannon", target, "damage", c.damage(), 0, 10000);
            add(specs, "cannon", target, "reload_time", c.reloadTime(), 0, 12000);
            add(specs, "cannon", target, "durability", c.durability(), 1, 100000);
            add(specs, "cannon", target, "scope_zoom", c.scopeZoom(), 0.1, 100);
            add(specs, "cannon", target, "initial_speed", c.initialSpeed(), 0.01, 100);
            add(specs, "cannon", target, "drag_coeff", c.dragCoeff(), 0.00001, 1);
            add(specs, "cannon", target, "gravity", c.gravity(), 0, 100);
            add(specs, "cannon", target, "explosion_power", c.explosionPower(), 0, 100);
            add(specs, "cannon", target, "dispersion", c.dispersion(), 0, 180);
            add(specs, "cannon", target, "fire_cooldown", c.fireCooldown(), 0, 12000);
            add(specs, "cannon", target, "salvo_count", c.salvoCount(), 1, 100);
            add(specs, "cannon", target, "salvo_interval", c.salvoInterval(), 0, 12000);
            add(specs, "cannon", target, "projectile_weight", c.projectileWeight(), 0.01, 100000);
            add(specs, "cannon", target, "vertical_spread", c.verticalSpread(), 0, 180);
            add(specs, "cannon", target, "horizontal_spread", c.horizontalSpread(), 0, 180);
            add(specs, "cannon", target, "max_elevation", c.maxElevation(), -90, 90);
            add(specs, "cannon", target, "min_elevation", c.minElevation(), -90, 90);
            add(specs, "cannon", target, "turret_speed", c.turretSpeed(), 0, 180);
        }
        for (AircraftDefinition a : AircraftDefinitionService.snapshot().values()) {
            String target = a.id();
            add(specs, "aircraft", target, "fuel_capacity", a.fuelCapacity(), 1, 100000);
            add(specs, "aircraft", target, "ammo_capacity", a.ammoCapacity(), 0, 100000);
            add(specs, "aircraft", target, "panel_damage", a.panelDamage(), 0, 10000);
            add(specs, "aircraft", target, "panel_speed", a.panelSpeed(), 0.01, 100);
            add(specs, "aircraft", target, "weight", a.weight(), 0, 100000);
            add(specs, "aircraft", target, "health", a.health(), 1, 100000);
            add(specs, "aircraft", target, "attack_cooldown", a.attackCooldown(), 1, 12000);
            add(specs, "aircraft", target, "reload_time", ResolvedAircraftStats.DEFAULT_RELOAD_TIME, 1, 12000);
            add(specs, "aircraft", target, "launch_speed_coefficient", 0.3, 0.05, 5.0);
            add(specs, "aircraft", target, "cruise_speed_coefficient", 0.3, 0.05, 5.0);
            add(specs, "aircraft", target, "attack_speed_coefficient", 0.5, 0.05, 5.0);
            add(specs, "aircraft", target, "return_speed_coefficient", 0.52, 0.05, 5.0);
            add(specs, "aircraft", target, "follow_speed_coefficient", 0.35, 0.05, 5.0);
            add(specs, "aircraft", target, "recon_speed_coefficient", 0.4, 0.05, 5.0);
        }
        Set<String> ammoTargets = new HashSet<>();
        for (AmmoDefinition a : AmmoDefinitionService.all().values()) {
            String target = a.itemId().toString();
            if (!ammoTargets.add(target)) continue;
            add(specs, "ammo", target, "damage_multiplier", a.damageMultiplier(), 0, 100);
            add(specs, "ammo", target, "explosion_multiplier", a.explosionMultiplier(), 0, 100);
            add(specs, "ammo", target, "armor_ignore", a.armorIgnore(), 0, 1);
            add(specs, "ammo", target, "underwater_explosion", a.underwaterExplosion());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof TorpedoItem torpedo) || torpedo.getModelKey() == null) continue;
            String target = torpedo.getModelKey();
            add(specs, "torpedo", target, "damage", torpedo.getBaseDamage(), 0, 10000);
            add(specs, "torpedo", target, "range", torpedo.getBaseRange(), 1, 100000);
            add(specs, "torpedo", target, "speed", torpedo.getBaseSpeed(), 0.01, 10);

            // covered above; equipment is added in the type-specific pass below
        }
        for (Item item : BuiltInRegistries.ITEM) {
            String target = BuiltInRegistries.ITEM.getKey(item).toString();
            if (item instanceof RadarItem radar) {
                add(specs, "equipment", target, "weight", radar.getBaseWeight(), 0, 112);
                add(specs, "equipment", target, "range", radar.getBaseRange(), 1, 1024);
                add(specs, "equipment", target, "target", radar.getBaseTarget().ordinal(), 0, 2);
            } else if (item instanceof SonarItem sonar) {
                add(specs, "equipment", target, "weight", sonar.getBaseWeight(), 0, 112);
                add(specs, "equipment", target, "range", sonar.getBaseRadius(), 1, 1024);
            } else if (item instanceof FireControlRadarItem radar) {
                add(specs, "equipment", target, "weight", radar.getBaseWeight(), 0, 112);
                add(specs, "equipment", target, "range", radar.getBaseSnapRangeChunks(), 1, 1024);
            }
        }
        for (ShipType core : ShipType.values()) {
            add(specs, "core", core.name(), "health_bonus", core.healthBonus, -19, 1000);
            add(specs, "core", core.name(), "max_load", core.maxLoad, 1, 10000);
            add(specs, "core", core.name(), "fuel_capacity", core.fuelCapacity, 1, 10000);
            add(specs, "core", core.name(), "distance_per_fuel", core.distancePerFuel, 0.01, 100000);
            add(specs, "core", core.name(), "full_load_speed", core.fullLoadSpeed, 0.01, 10);
            add(specs, "core", core.name(), "empty_speed", core.emptySpeed, 0.01, 10);
            add(specs, "core", core.name(), "base_armor", core.baseArmor, 0, 1000);
            add(specs, "core", core.name(), "armor_toughness", core.armorToughness, 0, 1000);
            add(specs, "core", core.name(), "speed_bonus", 0.0, -5, 5);
        }
        addLivingEntitySpecs(specs);
        return new ArrayList<>(specs.values());
    }

    private static void addLivingEntitySpecs(Map<String, TerminalParameterSpec> specs) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.MISC) continue;
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null) continue;
            String path = id.getPath();
            String group = id.getNamespace().equals("minecraft") ? "vanilla_mob"
                    : path.equals("ship_girl") ? "ship_girl"
                    : path.startsWith("deep_ocean_") || path.equals("low_tier_destroyer")
                    || path.equals("goldencatcat") ? "deep_ocean" : "vanilla_mob";
            AttributeSupplier defaults = null;
            try {
                @SuppressWarnings("unchecked")
                EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
                defaults = DefaultAttributes.getSupplier(livingType);
            } catch (Throwable ignored) {
                // A third-party entity may not expose a default supplier. The
                // generic fallback keeps its terminal entry editable.
            }
            add(specs, group, id.toString(), "max_health", attribute(defaults, Attributes.MAX_HEALTH, 20), 1, 100000);
            add(specs, group, id.toString(), "movement_speed", attribute(defaults, Attributes.MOVEMENT_SPEED, 0.1), 0.001, 100);
            add(specs, group, id.toString(), "attack_damage", attribute(defaults, Attributes.ATTACK_DAMAGE, 2), 0, 100000);
            add(specs, group, id.toString(), "follow_range", attribute(defaults, Attributes.FOLLOW_RANGE, 16), 1, 2048);
            add(specs, group, id.toString(), "armor", attribute(defaults, Attributes.ARMOR, 0), 0, 10000);
        }
    }

    private static double attribute(AttributeSupplier defaults,
                                    net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> key,
                                    double fallback) {
        if (defaults == null) return fallback;
        try {
            return defaults.getBaseValue(key);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static TerminalParameterSpec find(String key) {
        if (key == null) return null;
        for (TerminalParameterSpec spec : all()) if (spec.key().equals(key)) return spec;
        return null;
    }

    public static TerminalParameterSpec get(String key) { return find(key); }

    private static void add(Map<String, TerminalParameterSpec> out, String group, String target,
                            String property, int base, int min, int max) {
        put(out, new TerminalParameterSpec(group + "." + target + "." + property, group, target,
                property, TerminalParameterSpec.ValueType.INTEGER, Integer.toString(base),
                Math.min(min, base), Math.max(max, base)));
    }

    private static void add(Map<String, TerminalParameterSpec> out, String group, String target,
                            String property, double base, double min, double max) {
        if (!Double.isFinite(base)) return;
        put(out, new TerminalParameterSpec(group + "." + target + "." + property, group, target,
                property, TerminalParameterSpec.ValueType.DOUBLE, Double.toString(base),
                Math.min(min, base), Math.max(max, base)));
    }

    private static void add(Map<String, TerminalParameterSpec> out, String group, String target,
                            String property, boolean base) {
        put(out, new TerminalParameterSpec(group + "." + target + "." + property, group, target,
                property, TerminalParameterSpec.ValueType.BOOLEAN, Boolean.toString(base), 0, 1));
    }

    private static void put(Map<String, TerminalParameterSpec> out, TerminalParameterSpec spec) {
        out.put(spec.key(), spec);
    }
}
