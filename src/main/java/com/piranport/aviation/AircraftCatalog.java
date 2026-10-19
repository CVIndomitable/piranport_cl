package com.piranport.aviation;

import com.piranport.PiranPort;
import com.piranport.item.AircraftItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞机“物品注册 ↔ 数据定义”的集中目录。
 *
 * <p>加载定义时做硬校验：
 * <ul>
 *   <li>每个已注册的 {@link AircraftItem} 必须有同名定义；</li>
 *   <li>每个定义必须有对应的已注册飞机物品；</li>
 *   <li>挂载物 ID 必须指向已注册物品；</li>
 *   <li>视觉 ID 必须在 {@link AircraftVisualIds} 白名单内（由定义构造器校验）。</li>
 * </ul>
 *
 * <p>创造标签、调试终端、调试命令、测试统一从这里取列表，避免新增飞机时
 * 漏加标签或漏加终端参数。
 */
public final class AircraftCatalog {
    private static volatile Map<ResourceLocation, Item> itemsById = Map.of();
    private static volatile Map<Item, AircraftDefinition> definitionsByItem = Map.of();
    private static volatile List<AircraftDefinition> definitions = List.of();

    private AircraftCatalog() {}

    /** 校验并重建目录。任何不一致都会抛异常，让数据错误在加载阶段暴露。 */
    public static synchronized void rebuild(Map<ResourceLocation, AircraftDefinition> loaded) {
        Map<ResourceLocation, Item> items = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof AircraftItem)) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            items.put(id, item);
        }

        List<String> errors = new ArrayList<>();
        for (ResourceLocation itemId : items.keySet()) {
            if (!loaded.containsKey(itemId)) {
                errors.add("aircraft item has no definition: " + itemId);
            }
        }
        for (AircraftDefinition definition : loaded.values()) {
            if (!items.containsKey(definition.id())) {
                errors.add("aircraft definition has no registered item: " + definition.id());
            }
            if (definition.payloadItem() != null
                    && BuiltInRegistries.ITEM.get(definition.payloadItem()) == null) {
                errors.add("payload item is not registered: " + definition.payloadItem()
                        + " (aircraft " + definition.id() + ")");
            }
            // 侦察机不声明 attack_profile，跳过策略校验；其余机种必须能找到策略
            if (definition.attackProfile() != null && AttackStrategyLookup.missing(definition)) {
                errors.add("attack profile has no strategy: " + definition.attackProfile().id()
                        + " (aircraft " + definition.id() + ")");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("invalid aircraft catalog:\n - " + String.join("\n - ", errors));
        }

        Map<Item, AircraftDefinition> byItem = new HashMap<>();
        items.forEach((id, item) -> byItem.put(item, loaded.get(id)));
        List<AircraftDefinition> ordered = new ArrayList<>(loaded.values());
        ordered.sort(Comparator.comparing(definition -> definition.id().toString()));

        itemsById = Map.copyOf(items);
        definitionsByItem = Map.copyOf(byItem);
        definitions = List.copyOf(ordered);
        PiranPort.LOGGER.info("Aircraft catalog validated: {} item(s)", items.size());
    }

    public static void clear() {
        itemsById = Map.of();
        definitionsByItem = Map.of();
        definitions = List.of();
    }

    /** 已校验的定义，按 ID 排序，供终端/标签/测试遍历。 */
    public static List<AircraftDefinition> definitions() {
        return definitions;
    }

    public static Map<ResourceLocation, Item> items() {
        return itemsById;
    }

    public static AircraftDefinition definition(Item item) {
        return definitionsByItem.get(item);
    }

    /**
     * 延迟查询攻击策略，避免 aviation 包在类初始化阶段反向依赖 combat 注册表。
     * 只用于加载阶段的校验，失败视为数据错误。
     */
    private static final class AttackStrategyLookup {
        private static boolean missing(AircraftDefinition definition) {
            return com.piranport.aviation.combat.AttackStrategyRegistry
                    .byId(definition.attackProfile().id()).isEmpty();
        }
    }
}
