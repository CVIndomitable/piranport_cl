package com.piranport.aviation;

import com.piranport.component.AircraftInfo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

/**
 * 迁移期飞机定义服务。定义在创建飞机时解析一次并缓存；飞行中的实体持有自己的定义 ID，不读取可变注册表。
 */
public final class AircraftDefinitionService {
    public static final String ID_PREFIX = "piranport:aircraft/";
    private static volatile Map<String, AircraftDefinition> definitions = Map.of();


    private AircraftDefinitionService() {}

    /** 注册或替换数据加载阶段构造的不可变定义。 */
    public static void register(AircraftDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        Map<String, AircraftDefinition> copy = new HashMap<>(definitions);
        copy.put(definition.id(), definition);
        definitions = Map.copyOf(copy);
    }

    /** 清空数据重载缓存；已生成实体不会调用此方法。 */
    public static void clear() {
        definitions = Map.of();
    }

    /** Replaces all resource-backed definitions as one reload operation. */
    public static void replaceAll(Map<String, AircraftDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        Map<String, AircraftDefinition> copy = new HashMap<>();
        definitions.forEach((id, definition) -> {
            if (definition == null || !id.equals(definition.id())) {
                throw new IllegalArgumentException("definition map key does not match definition id: " + id);
            }
            copy.put(id, definition);
        });
        definitions = Map.copyOf(copy);
    }

    /** Resource ID used by aircraft JSON files. */
    public static String resourceId(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return id.toString();
    }

    public static AircraftDefinition find(String id) {
        if (id == null || id.isBlank()) return null;
        return definitions.get(id);
    }

    /** 按稳定定义 ID解析，找不到时用旧类型生成兼容定义并缓存。 */
    public static AircraftDefinition resolve(AircraftInfo info) {
        return resolve(info, info.definitionId());
    }

    /** Resolve the immutable definition carried by an aircraft item. */
    public static AircraftDefinition resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info == null) return null;

        // Registered item IDs are more specific than the legacy type ID. This
        // keeps two aircraft of one class from sharing the first resolved stats.
        net.minecraft.resources.ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String stableId = key != null && !"minecraft".equals(key.getNamespace())
                ? resourceId(key) : info.definitionId();
        return resolve(info, stableId);
    }

    /**
     * 为物品注册声明提供唯一的迁移 ID。按机种生成的 legacyId 只能用于旧存档，
     * 否则同一机种的不同飞机会把第一份数值错误地复用给后续飞机。
     */
    public static AircraftInfo withItemId(AircraftInfo info, String itemId) {
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(itemId, "itemId");
        String normalized = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return new AircraftInfo(info.aircraftType(), info.fuelCapacity(), info.ammoCapacity(),
                info.currentFuel(), info.panelDamage(), info.panelSpeed(), info.weight(),
                info.bombingMode(), info.payloadLoaded(), "piranport:aircraft/" + normalized);
    }

    /**
     * 从物品注册名解析定义。物品注册名比旧的机种枚举更细，可以让同一机种的不同型号拥有独立快照。
     */
    public static AircraftDefinition resolve(AircraftInfo info, String stableId) {
        Objects.requireNonNull(info, "info");
        String id = stableId == null || stableId.isBlank() ? info.definitionId() : stableId;
        AircraftDefinition known = find(id);
        if (known == null) {
            String canonical = canonicalResourceId(id);
            if (!canonical.equals(id)) known = find(canonical);
        }
        if (known != null) return known;
        AircraftDefinition migrated = AircraftDefinition.fromLegacy(id, info);
        register(migrated);
        return migrated;
    }

    /** Canonical resource IDs live under data/<namespace>/aircraft/. */
    public static String canonicalResourceId(String id) {
        Objects.requireNonNull(id, "id");
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null || !"piranport".equals(parsed.getNamespace())
                || parsed.getPath().startsWith("aircraft/")) return id;
        return ID_PREFIX + parsed.getPath();
    }

    /** 给旧构造器使用的稳定 ID。旧物品没有独立 ID 时按机种生成确定性兼容 ID。 */
    public static String legacyId(AircraftInfo.AircraftType type) {
        Objects.requireNonNull(type, "type");
        return ID_PREFIX + type.getSerializedName();
    }

    public static Map<String, AircraftDefinition> snapshot() {
        return Map.copyOf(definitions);
    }
}
