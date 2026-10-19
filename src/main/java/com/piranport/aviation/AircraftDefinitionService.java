package com.piranport.aviation;

import com.piranport.component.AircraftState;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;

/**
 * 飞机定义服务。
 *
 * <p>定义 ID 与飞机物品注册名一一对应。定义在数据加载阶段解析并缓存；飞行中的实体
 * 持有自己的定义 ID 与创建时快照，不读取可变注册表。没有“旧机种回退”路径：
 * 找不到定义就是数据错误，由 {@link AircraftCatalog} 在加载阶段硬校验。
 */
public final class AircraftDefinitionService {
    private static volatile Map<ResourceLocation, AircraftDefinition> definitions = Map.of();

    private AircraftDefinitionService() {}

    /** 注册或替换数据加载阶段构造的不可变定义。 */
    public static synchronized void register(AircraftDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        java.util.Map<ResourceLocation, AircraftDefinition> copy =
                new java.util.HashMap<>(definitions);
        copy.put(definition.id(), definition);
        definitions = Map.copyOf(copy);
    }

    /** 清空数据重载缓存；已生成实体不会调用此方法。 */
    public static synchronized void clear() {
        definitions = Map.of();
    }

    /** Replaces all resource-backed definitions as one reload operation. */
    public static synchronized void replaceAll(Map<ResourceLocation, AircraftDefinition> next) {
        Objects.requireNonNull(next, "definitions");
        java.util.Map<ResourceLocation, AircraftDefinition> copy = new java.util.HashMap<>();
        next.forEach((id, definition) -> {
            if (definition == null || !id.equals(definition.id())) {
                throw new IllegalArgumentException("definition map key does not match definition id: " + id);
            }
            copy.put(id, definition);
        });
        definitions = Map.copyOf(copy);
    }

    public static Map<ResourceLocation, AircraftDefinition> snapshot() {
        return definitions;
    }

    public static java.util.Collection<AircraftDefinition> all() {
        return definitions.values();
    }

    /** 按稳定定义 ID 查找；找不到返回 null。 */
    public static AircraftDefinition find(ResourceLocation id) {
        if (id == null) return null;
        return definitions.get(id);
    }

    /** 便捷重载：解析字符串 ID。 */
    public static AircraftDefinition find(String id) {
        if (id == null || id.isBlank()) return null;
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? null : find(parsed);
    }

    /** 按稳定定义 ID 查找；找不到视为数据错误。 */
    public static AircraftDefinition require(ResourceLocation id) {
        AircraftDefinition definition = find(id);
        if (definition == null) {
            throw new IllegalStateException("missing aircraft definition: " + id);
        }
        return definition;
    }

    /** 飞机物品栈的定义；空栈或非飞机物品返回 null。 */
    public static AircraftDefinition resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId == null ? null : find(itemId);
    }

    /** 飞机物品栈的定义；空栈/未注册/缺少定义时直接抛异常。 */
    public static AircraftDefinition require(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("aircraft stack must not be empty");
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            throw new IllegalArgumentException("aircraft item is not registered");
        }
        return require(itemId);
    }

    /** 读取物品上的运行时状态；没有组件时返回 EMPTY。 */
    public static AircraftState stateOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return AircraftState.EMPTY;
        AircraftState state = stack.get(ModDataComponents.AIRCRAFT_STATE.get());
        return state == null ? AircraftState.EMPTY : state;
    }
}
