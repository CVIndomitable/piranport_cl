package com.piranport.compat;

import com.piranport.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.Map;

/**
 * 材料等价兼容层。
 *
 * <p>制造台配方目前把材料写死成原版物品（铁锭、铜锭、火药……），但装了其他模组时，
 * 等价材料通常只挂在 c: 通用标签上（例如科技模组的铝锭、其他模组的钢锭）。若继续用
 * {@code ItemStack.is(item)} 精确匹配，玩家拿等价材料也合不了，只能被迫去找原版物，
 * 体验很差。这里把「配方材料 → 通用标签」的映射集中到一处，所有材料校验统一走
 * 标签感知匹配，避免每个调用点各写一份 if。</p>
 *
 * <p>未收录的 required 退化为精确物品匹配（{@link #tagFor} 返回 null），行为与改动前
 * 完全一致。铝锭/生铝是本模组材料，需映射到约定的 c: 标签。</p>
 */
public final class MaterialCompat {

    // 本模组材料对应的 c: 通用标签；NeoForge 的 Tags 里没有收录，需自行创建
    /** 铝锭通用标签；配方展示也用同一份定义，避免 getIngredients 与 matches 口径不一致。 */
    public static final TagKey<Item> INGOTS_ALUMINUM = commonTag("ingots/aluminum");
    private static final TagKey<Item> RAW_MATERIALS_ALUMINUM = commonTag("raw_materials/aluminum");

    private MaterialCompat() {}

    /**
     * 判断候选物品能否顶替配方要求的物品。
     *
     * @param required  配方写死的物品（null 视为不匹配）
     * @param candidate 实际放入/持有的物品堆
     */
    public static boolean matches(Item required, ItemStack candidate) {
        if (required == null || candidate == null || candidate.isEmpty()) return false;
        if (candidate.is(required)) return true;
        TagKey<Item> tag = tagFor(required);
        return tag != null && candidate.is(tag);
    }

    /**
     * {@link ItemStack} 重载：required 为空栈时直接判否，避免把空气当成材料。
     */
    public static boolean matches(ItemStack required, ItemStack candidate) {
        if (required == null || required.isEmpty()) return false;
        return matches(required.getItem(), candidate);
    }

    /**
     * 在一份「物品 → 数量」清单里统计可顶替 required 的总数。
     * 必须遍历真实条目而不是只查 required 本身，才能把标签内的等价材料算进来。
     */
    public static int count(Map<Item, Integer> available, Item required) {
        if (available == null || required == null) return 0;
        int total = 0;
        for (Map.Entry<Item, Integer> entry : available.entrySet()) {
            if (matchesItem(required, entry.getKey())) {
                total += entry.getValue();
            }
        }
        return total;
    }

    // Item 版匹配：count() 手里只有物品类型，避免为了查标签而新建 ItemStack
    private static boolean matchesItem(Item required, Item candidate) {
        if (required == null || candidate == null) return false;
        if (candidate == required) return true;
        TagKey<Item> tag = tagFor(required);
        return tag != null && candidate.builtInRegistryHolder().is(tag);
    }

    // 惰性 if 链：只在真正校验到该材料时才触碰 ModItems，避免过早触发其静态初始化
    private static TagKey<Item> tagFor(Item required) {
        if (required == Items.IRON_INGOT) return Tags.Items.INGOTS_IRON;
        if (required == Items.COPPER_INGOT) return Tags.Items.INGOTS_COPPER;
        if (required == Items.GOLD_INGOT) return Tags.Items.INGOTS_GOLD;
        if (required == Items.NETHERITE_INGOT) return Tags.Items.INGOTS_NETHERITE;
        if (required == Items.IRON_NUGGET) return Tags.Items.NUGGETS_IRON;
        if (required == Items.GOLD_NUGGET) return Tags.Items.NUGGETS_GOLD;
        if (required == Items.DIAMOND) return Tags.Items.GEMS_DIAMOND;
        if (required == Items.EMERALD) return Tags.Items.GEMS_EMERALD;
        if (required == Items.LAPIS_LAZULI) return Tags.Items.GEMS_LAPIS;
        if (required == Items.AMETHYST_SHARD) return Tags.Items.GEMS_AMETHYST;
        if (required == Items.QUARTZ) return Tags.Items.GEMS_QUARTZ;
        if (required == Items.GLOWSTONE_DUST) return Tags.Items.DUSTS_GLOWSTONE;
        if (required == Items.REDSTONE) return Tags.Items.DUSTS_REDSTONE;
        if (required == Items.GUNPOWDER) return Tags.Items.GUNPOWDERS;
        if (required == Items.SLIME_BALL) return Tags.Items.SLIME_BALLS;
        if (required == Items.ENDER_PEARL) return Tags.Items.ENDER_PEARLS;
        if (required == Items.FEATHER) return Tags.Items.FEATHERS;
        if (required == Items.STRING) return Tags.Items.STRINGS;
        if (required == Items.BONE) return Tags.Items.BONES;
        if (required == Items.LEATHER) return Tags.Items.LEATHERS;
        if (required == Items.STICK) return Tags.Items.RODS_WOODEN;
        if (required == Items.EGG) return Tags.Items.EGGS;
        if (required == Items.WATER_BUCKET) return Tags.Items.BUCKETS_WATER;
        if (required == Items.MILK_BUCKET) return Tags.Items.BUCKETS_MILK;
        if (required == Items.WHEAT) return Tags.Items.CROPS_WHEAT;
        if (required == Items.BREAD) return Tags.Items.FOODS_BREAD;
        if (required == ModItems.ALUMINUM_INGOT.get()) return INGOTS_ALUMINUM;
        if (required == ModItems.RAW_ALUMINUM.get()) return RAW_MATERIALS_ALUMINUM;
        return null;
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
