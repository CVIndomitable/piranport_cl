package com.piranport.ammo;

import com.piranport.item.AmmoItem;
import com.piranport.item.MissileItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoItem;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 创造模式弹药库可勾选的弹药/燃料清单。
 *
 * <p>按运行时注册表构建，而不是手写常量表：新增弹药时 GUI 自动出现，
 * 不会出现"新弹药忘了加进调试库"的问题。分类优先用物品标签/类型判断，
 * 只有真正属于弹药或燃料的物品才会进入清单。</p>
 */
public final class CreativeAmmoCatalog {

    public record Category(String translationKey, List<Item> items) {}

    /** 一次性构建并整体发布，避免三个字段之间出现可见性裂缝。 */
    private record Snapshot(List<Category> categories, List<Item> allItems,
                            Set<String> allIds, Map<Item, String> idByItem) {}

    private static volatile Snapshot snapshot;

    private CreativeAmmoCatalog() {}

    private static Snapshot snapshot() {
        Snapshot local = snapshot;
        if (local == null) {
            synchronized (CreativeAmmoCatalog.class) {
                local = snapshot;
                if (local == null) {
                    List<Category> categories = build();
                    List<Item> items = categories.stream()
                            .flatMap(category -> category.items().stream())
                            .toList();
                    Map<Item, String> idByItem = new HashMap<>();
                    for (Item item : items) {
                        idByItem.put(item, BuiltInRegistries.ITEM.getKey(item).toString());
                    }
                    local = new Snapshot(List.copyOf(categories), items,
                            Set.copyOf(idByItem.values()), Map.copyOf(idByItem));
                    snapshot = local;
                }
            }
        }
        return local;
    }

    public static List<Category> categories() {
        return snapshot().categories();
    }

    public static List<Item> allItems() {
        return snapshot().allItems();
    }

    public static boolean isValid(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        return snapshot().allIds().contains(itemId);
    }

    @Nullable
    public static Item byId(String itemId) {
        if (!isValid(itemId)) return null;
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? null : item;
    }

    /** 物品 ID；清单内物品走缓存，避免工具提示逐帧渲染时反复分配字符串。 */
    public static String idOf(Item item) {
        Snapshot local = snapshot;
        if (local != null) {
            String cached = local.idByItem().get(item);
            if (cached != null) return cached;
        }
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static List<Category> build() {
        List<Item> shells = new ArrayList<>();
        List<Item> torpedoes = new ArrayList<>();
        List<Item> missiles = new ArrayList<>();
        List<Item> aviation = new ArrayList<>();
        List<Item> fuel = new ArrayList<>();

        Item cacheItem = ModItems.CREATIVE_AMMO_CACHE.get();
        Item shipFuel = ModItems.FUEL.get();
        Item aviationFuel = ModItems.AVIATION_FUEL.get();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || item == cacheItem) continue;
            ItemStack stack = new ItemStack(item);
            // 炮弹的权威归属是三个口径标签；数据包扩展的弹药也会自动进来。
            if (stack.is(ShipCoreItem.SMALL_SHELLS)
                    || stack.is(ShipCoreItem.MEDIUM_SHELLS)
                    || stack.is(ShipCoreItem.LARGE_SHELLS)) {
                shells.add(item);
            } else if (item instanceof TorpedoItem) {
                torpedoes.add(item);
            } else if (item instanceof MissileItem) {
                missiles.add(item);
            } else if (item == shipFuel || item == aviationFuel) {
                fuel.add(item);
            } else if (item instanceof AmmoItem) {
                // 航空燃料、航空炸弹、航空鱼雷、深弹、机炮子弹等统一归入航空弹药。
                aviation.add(item);
            }
        }

        List<Category> result = new ArrayList<>();
        addIfNotEmpty(result, "gui.piranport.creative_ammo_cache.category.shells", shells);
        addIfNotEmpty(result, "gui.piranport.creative_ammo_cache.category.torpedoes", torpedoes);
        addIfNotEmpty(result, "gui.piranport.creative_ammo_cache.category.missiles", missiles);
        addIfNotEmpty(result, "gui.piranport.creative_ammo_cache.category.aviation", aviation);
        addIfNotEmpty(result, "gui.piranport.creative_ammo_cache.category.fuel", fuel);
        return List.copyOf(result);
    }

    private static void addIfNotEmpty(List<Category> out, String key, List<Item> items) {
        if (!items.isEmpty()) out.add(new Category(key, List.copyOf(items)));
    }
}
