package com.piranport.combat.cannon.ammo;

import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 稳定的炮弹定义注册表；后续数据包加载可替换快照而无需改变查询方。 */
public final class AmmoDefinitionService {
    private static final Map<ResourceLocation, AmmoDefinition> DEFINITIONS = createDefaults();

    private AmmoDefinitionService() {}

    public static Optional<AmmoDefinition> find(ResourceLocation itemId) {
        return Optional.ofNullable(DEFINITIONS.get(itemId));
    }

    public static AmmoDefinition require(ResourceLocation itemId) {
        return find(itemId).orElseThrow(() ->
                new IllegalArgumentException("No cannon ammo definition for " + itemId));
    }

    public static Map<ResourceLocation, AmmoDefinition> all() {
        return Map.copyOf(DEFINITIONS);
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
        DEFINITIONS.clear();
        DEFINITIONS.putAll(copy);
    }

    /** 恢复内置定义；数据包重载失败或测试清理时使用。 */
    public static synchronized void resetDefaults() {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(createDefaults());
    }

    private static Map<ResourceLocation, AmmoDefinition> createDefaults() {
        LinkedHashMap<ResourceLocation, AmmoDefinition> map = new LinkedHashMap<>();
        register(map, "small_he_shell", AmmoBehavior.HE, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "medium_he_shell", AmmoBehavior.HE, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "large_he_shell", AmmoBehavior.HE, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "small_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "medium_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "large_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "type_91_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "type_1_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "super_heavy_ap_shell", AmmoBehavior.AP, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "small_vt_shell", AmmoBehavior.VT, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "small_type3_shell", AmmoBehavior.TYPE3, CannonAmmoRules.CaliberFamily.SMALL);
        register(map, "medium_type3_shell", AmmoBehavior.TYPE3, CannonAmmoRules.CaliberFamily.MEDIUM);
        register(map, "large_type3_shell", AmmoBehavior.TYPE3, CannonAmmoRules.CaliberFamily.LARGE);
        register(map, "mk23_nuclear_shell", AmmoBehavior.MK23, CannonAmmoRules.CaliberFamily.LARGE);
        return Collections.synchronizedMap(map);
    }

    private static void register(Map<ResourceLocation, AmmoDefinition> map, String path,
                                 AmmoBehavior behavior, CannonAmmoRules.CaliberFamily family) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("piranport", path);
        map.put(id, new AmmoDefinition(id, behavior, family));
    }
}
