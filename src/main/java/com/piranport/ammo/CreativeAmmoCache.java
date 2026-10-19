package com.piranport.ammo;

import com.piranport.component.CreativeAmmoSelection;
import com.piranport.item.TorpedoItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 创造模式弹药库的判定入口。
 *
 * <p>语义：物品栏里存在 {@code creative_ammo_cache}，且其组件中勾选了某个弹药/燃料时，
 * 该物品被视为"数量无限"——所有从背包读取弹药的地方都要先问这里，再读真实物品堆。</p>
 *
 * <p>只认勾选项，不因为玩家处于创造模式就全开：GUI 的用途正是精确控制测试条件。</p>
 */
public final class CreativeAmmoCache {

    /** 可用数量视为无限时的返回值，避免各调用点自行定义哨兵值。 */
    public static final int INFINITE = Integer.MAX_VALUE;

    private CreativeAmmoCache() {}

    public static boolean isCacheStack(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.CREATIVE_AMMO_CACHE.get());
    }

    public static CreativeAmmoSelection selection(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CREATIVE_AMMO_SELECTION.get(),
                CreativeAmmoSelection.EMPTY);
    }

    /** 玩家背包（含副手/盔甲槽）中是否存在勾选了该物品的弹药库。 */
    public static boolean isInfinite(@Nullable Player player, @Nullable Item item) {
        return player != null && isInfinite(player.getInventory(), item);
    }

    public static boolean isInfinite(Inventory inventory, @Nullable Item item) {
        if (item == null || item == Items.AIR) return false;
        String id = CreativeAmmoCatalog.idOf(item);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCacheStack(stack) && selection(stack).contains(id)) return true;
        }
        return false;
    }

    /** 玩家的弹药库里是否勾选了任意一项（用于"是否启用"类 UI 判断）。 */
    public static boolean hasAnySelection(@Nullable Player player) {
        return player != null && !selectedIds(player.getInventory()).isEmpty();
    }

    public static Set<String> selectedIds(Inventory inventory) {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCacheStack(stack)) ids.addAll(selection(stack).itemIds());
        }
        return ids;
    }

    /**
     * 按弹药库清单顺序找到第一个"已勾选且满足条件"的弹药/燃料。
     * 用清单顺序而不是勾选顺序，保证同一种情况下总是选中同一个弹种。
     */
    @Nullable
    public static Item findInfinite(@Nullable Player player, Predicate<Item> predicate) {
        return player == null ? null : findInfinite(player.getInventory(), predicate);
    }

    /** 不依赖 Player 的版本，供只拿到 Inventory 的装填扫描器使用。 */
    @Nullable
    public static Item findInfinite(Inventory inventory, Predicate<Item> predicate) {
        Set<String> ids = selectedIds(inventory);
        if (ids.isEmpty()) return null;
        for (Item item : CreativeAmmoCatalog.allItems()) {
            if (ids.contains(CreativeAmmoCatalog.idOf(item)) && predicate.test(item)) return item;
        }
        return null;
    }

    @Nullable
    public static TorpedoItem findInfiniteTorpedo(@Nullable Player player, int caliber) {
        Item item = findInfinite(player, candidate ->
                candidate instanceof TorpedoItem torpedo && torpedo.getCaliber() == caliber);
        return item instanceof TorpedoItem torpedo ? torpedo : null;
    }

    /** 同口径的已勾选鱼雷是否存在；供不持有实体鱼雷的装填路径使用。 */
    public static boolean hasInfiniteTorpedo(@Nullable Player player, int caliber) {
        return findInfiniteTorpedo(player, caliber) != null;
    }

    /** 某具体物品是否在弹药库中被勾选；{@code null} 玩家/物品一律 false。 */
    public static boolean isSelected(@Nullable Player player, @Nullable Item item) {
        if (player == null || item == null) return false;
        String id = CreativeAmmoCatalog.idOf(item);
        return selectedIds(player.getInventory()).contains(id);
    }
}
