package com.piranport.client.gecko;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 有 GeckoLib 舰装模型的武器清单。
 *
 * <p>未登记的武器继续由 {@code ShipRiggingLayer} 用物品模型占位；
 * 登记过的武器则只由 {@link RiggingAttachedLayer} 画，占位层会跳过，避免同一挂点出现双份。
 *
 * <p>这张表在客户端首次渲染前只读，不涉及服务端。
 */
public final class RiggingModelRegistry {

    private static final String MISSILE_DIR = "missile";

    /** key 是物品注册 ID；查表时用 BuiltInRegistries 的 key，避免依赖 ModItems 的静态初始化顺序。 */
    private static final Map<ResourceLocation, RiggingModelDef> BY_ITEM_ID = new LinkedHashMap<>();
    private static final List<RiggingModelDef> ALL;

    static {
        // 四个导弹发射器的模型局部前向不一致：
        // sy1 / terrier 导弹朝 +Z，mk14 / sea_dart 导弹朝 -Z（建模时与模型代理确认）。
        register(missile("sy1_launcher", 180.0f, 0.30f));
        register(missile("mk14_harpoon_launcher", 0.0f, 0.30f));
        register(missile("terrier_launcher", 180.0f, 0.26f));
        register(missile("sea_dart_launcher", 0.0f, 0.30f));
        ALL = List.copyOf(BY_ITEM_ID.values());
    }

    private RiggingModelRegistry() {}

    private static RiggingModelDef missile(String id, float yawOffset, float scale) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath("piranport", id);
        String base = "rigging/" + MISSILE_DIR + "/" + id;
        return new RiggingModelDef(
                itemId,
                ResourceLocation.fromNamespaceAndPath("piranport", "geo/" + base + ".geo.json"),
                ResourceLocation.fromNamespaceAndPath("piranport", "textures/" + base + ".png"),
                ResourceLocation.fromNamespaceAndPath("piranport", "animations/" + base + ".animation.json"),
                yawOffset,
                scale,
                RiggingModelDef.DEFAULT_IDLE_ANIMATION,
                RiggingModelDef.DEFAULT_FIRE_ANIMATION);
    }

    private static void register(RiggingModelDef def) {
        BY_ITEM_ID.put(def.itemId(), def);
    }

    /** 返回该物品的 GeckoLib 模型定义；没有登记时返回 null（调用方回退到物品模型占位）。 */
    public static RiggingModelDef find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return BY_ITEM_ID.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isRegistered(ItemStack stack) {
        return find(stack) != null;
    }

    /** 全部已登记模型，供 {@code ClientModEvents.onAddLayers} 每模型挂一个渲染层。 */
    public static List<RiggingModelDef> all() {
        return ALL;
    }

    /** 仅用于诊断/测试：已登记的物品 ID 快照。 */
    public static List<ResourceLocation> registeredItemIds() {
        return new ArrayList<>(BY_ITEM_ID.keySet());
    }
}
