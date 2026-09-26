package com.piranport.combat.cannon.ammo;

import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.piranport.terminal.TerminalParameters;

/** 稳定的炮弹定义注册表；后续数据包加载可替换快照而无需改变查询方。 */
public final class AmmoDefinitionService {
    private record Snapshot(Map<ResourceLocation, AmmoDefinition> byId, java.util.List<AmmoDefinition> ordered) {}
    private static volatile Snapshot snapshot = snapshotOf(createDefaults());

    private AmmoDefinitionService() {}

    public static Optional<AmmoDefinition> find(ResourceLocation itemId) {
        AmmoDefinition base = snapshot.byId().get(itemId);
        if (base == null) return Optional.empty();
        String key = "ammo." + itemId + ".";
        AmmoDefinition effective = new AmmoDefinition(base.itemId(), base.behavior(), base.caliberFamily(),
                (float) TerminalParameters.getDouble(key + "damage_multiplier", base.damageMultiplier()),
                (float) TerminalParameters.getDouble(key + "explosion_multiplier", base.explosionMultiplier()),
                (float) TerminalParameters.getDouble(key + "armor_ignore", base.armorIgnore()),
                TerminalParameters.getBoolean(key + "underwater_explosion", base.underwaterExplosion()),
                base.impactKindOverride());
        // 无有效变化时保留快照对象身份，避免查询方看到伪造的新定义。
        return Optional.of(effective.equals(base) ? base : effective);
    }

    public static AmmoDefinition require(ResourceLocation itemId) {
        return find(itemId).orElseThrow(() ->
                new IllegalArgumentException("No cannon ammo definition for " + itemId));
    }

    public static Map<ResourceLocation, AmmoDefinition> all() {
        return snapshot.byId();
    }

    /** Returns definitions in stable registration order for candidate selection. */
    public static synchronized java.util.List<AmmoDefinition> allInOrder() {
        return snapshot.ordered();
    }

    /** Returns a fresh immutable snapshot of code-provided defaults for reload baselines. */
    public static java.util.List<AmmoDefinition> builtInInOrder() {
        return java.util.List.copyOf(createDefaults().values());
    }

    /** 只供数据驱动加载器使用；调用方应在初始化阶段一次性替换，避免半更新状态。 */
    public static synchronized void replaceAll(Map<ResourceLocation, AmmoDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<ResourceLocation, AmmoDefinition> copy = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> {
            if (!id.equals(definition.itemId())) {
                throw new IllegalArgumentException("Definition key does not match item id: " + id);
            }
            if (copy.put(id, definition) != null) {
                throw new IllegalArgumentException("Duplicate ammo definition: " + id);
            }
        });
        publish(copy);
    }

    /** 恢复内置定义；数据包重载失败或测试清理时使用。 */
    public static synchronized void resetDefaults() {
        publish(createDefaults());
    }

    private static void publish(Map<ResourceLocation, AmmoDefinition> next) {
        snapshot = snapshotOf(next);
    }

    private static Snapshot snapshotOf(Map<ResourceLocation, AmmoDefinition> next) {
        return new Snapshot(Map.copyOf(next), java.util.List.copyOf(next.values()));
    }

    private static Map<ResourceLocation, AmmoDefinition> createDefaults() {
        LinkedHashMap<ResourceLocation, AmmoDefinition> map = new LinkedHashMap<>();
        register(map, "small_he_shell", AmmoBehavior.HE, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "medium_he_shell", AmmoBehavior.HE, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "large_he_shell", AmmoBehavior.HE, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "small_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "medium_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "large_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "type_91_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.LARGE, 1f, 1f, 0.20f, false, null);
        register(map, "type_1_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.LARGE, 1f, 1f, 0.50f, false, null);
        register(map, "super_heavy_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.MEDIUM, 1f, 1f, 0.35f, false, null);
        register(map, "small_vt_shell", AmmoBehavior.VT, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "small_type3_shell", AmmoBehavior.TYPE3, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "medium_type3_shell", AmmoBehavior.TYPE3, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "large_type3_shell", AmmoBehavior.TYPE3, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "mk23_nuclear_shell", AmmoBehavior.MK23, CannonAmmoRules.CaliberFamily.LARGE, 1f, 10f, 0f, true, AmmoBehaviorStrategy.ImpactKind.HE);
        return Collections.synchronizedMap(map);
    }

    private static void register(Map<ResourceLocation, AmmoDefinition> map, String path,
                                 AmmoBehavior behavior, CannonAmmoRules.CaliberFamily family) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("piranport", path);
        map.put(id, new AmmoDefinition(id, behavior, family));
    }

    private static void register(Map<ResourceLocation, AmmoDefinition> map, String path,
                                 AmmoBehavior behavior, CannonAmmoRules.CaliberFamily family,
                                 float damageMultiplier, float explosionMultiplier, float armorIgnore,
                                 boolean underwaterExplosion, AmmoBehaviorStrategy.ImpactKind impactKind) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("piranport", path);
        map.put(id, new AmmoDefinition(id, behavior, java.util.Optional.of(family), damageMultiplier,
                explosionMultiplier, armorIgnore, underwaterExplosion, java.util.Optional.ofNullable(impactKind)));
    }
}
