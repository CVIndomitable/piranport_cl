package com.piranport.terminal;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.combat.cannon.ammo.AmmoDefinition;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.TerminalConfigValue;
import com.piranport.item.ShipType;
import com.piranport.item.TorpedoItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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
            if (!spec.key().startsWith("global.aircraft.") && !spec.key().startsWith("global.ships.")) {
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
        }
        for (AmmoDefinition a : AmmoDefinitionService.all().values()) {
            String target = a.itemId().toString();
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
        return new ArrayList<>(specs.values());
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
