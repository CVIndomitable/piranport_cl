package com.piranport.combat.data;

import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 弹药背包扫描器 — 封装背包中弹药的查找、统计和消耗逻辑。
 * <p>自动跳过核心槽和武器槽，支持偏好弹种优先级和创造模式兜底。</p>
 */
public class AmmoInventory {

    private final Inventory inventory;
    private final int coreSlotIndex;
    private final int weaponSlotIndex;

    /**
     * 构造弹药背包扫描器。
     * @param inventory 玩家背包
     * @param coreSlotIndex 核心槽位（-2=头盔/config, -1=未找到, 0-39=背包, 40=副手）
     * @param weaponSlotIndex 武器槽位（0-39=背包, 40=副手）
     */
    public AmmoInventory(Inventory inventory, int coreSlotIndex, int weaponSlotIndex) {
        this.inventory = inventory;
        this.coreSlotIndex = coreSlotIndex;
        this.weaponSlotIndex = weaponSlotIndex;
    }

    /**
     * 在背包中查找指定类型的第一个弹药堆叠（跳过核心槽和武器槽）。
     * @param ammoType 弹药物品类型
     * @return 第一个匹配堆叠，未找到返回 null
     */
    @Nullable
    public ItemStack findFirstAmmoStack(Item ammoType) {
        for (int i = 0; i < inventory.items.size(); i++) {
            if (shouldSkipSlot(i)) continue;
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && stack.getItem() == ammoType) {
                return stack;
            }
        }

        if (weaponSlotIndex != 40 && coreSlotIndex != 40) {
            ItemStack offhand = inventory.offhand.get(0);
            if (!offhand.isEmpty() && offhand.getItem() == ammoType) {
                return offhand;
            }
        }

        return null;
    }

    /**
     * 统计背包中指定类型弹药的总数（跳过核心槽和武器槽）。
     * @param ammoType 弹药物品类型
     * @return 弹药总数
     */
    public int countAmmo(Item ammoType) {
        int count = 0;

        for (int i = 0; i < inventory.items.size(); i++) {
            if (shouldSkipSlot(i)) continue;
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && stack.getItem() == ammoType) {
                count += stack.getCount();
            }
        }

        if (weaponSlotIndex != 40 && coreSlotIndex != 40) {
            ItemStack offhand = inventory.offhand.get(0);
            if (!offhand.isEmpty() && offhand.getItem() == ammoType) {
                count += offhand.getCount();
            }
        }

        return count;
    }

    /**
     * 消耗指定数量的弹药（按槽位顺序扫描并 shrink stacks）。
     * @param ammoType 弹药物品类型
     * @param required 需要消耗的数量
     * @return true 表示成功消耗足够弹药，false 表示背包中弹药不足
     */
    public boolean consumeAmmo(Item ammoType, int required) {
        if (countAmmo(ammoType) < required) {
            return false;
        }

        int toConsume = required;

        for (int i = 0; i < inventory.items.size() && toConsume > 0; i++) {
            if (shouldSkipSlot(i)) continue;
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && stack.getItem() == ammoType) {
                int take = Math.min(toConsume, stack.getCount());
                com.piranport.debug.PiranPortDebug.consumeAmmo(stack, take);
                toConsume -= take;
            }
        }

        if (toConsume > 0 && weaponSlotIndex != 40 && coreSlotIndex != 40) {
            ItemStack offhand = inventory.offhand.get(0);
            if (!offhand.isEmpty() && offhand.getItem() == ammoType) {
                int take = Math.min(toConsume, offhand.getCount());
                com.piranport.debug.PiranPortDebug.consumeAmmo(offhand, take);
                toConsume -= take;
            }
        }

        return toConsume <= 0;
    }

    /**
     * 根据装填优先级选择弹药：偏好弹种优先，不足时按背包顺序回退，创造模式默认弹种兜底。
     * @param weapon 武器物品堆叠（用于读取偏好弹种和匹配口径）
     * @param required 需要装填的数量（通常为管数）
     * @param creative 是否为创造模式（true 时无视背包数量）
     * @param level 世界对象（用于应用配置覆盖后的口径匹配，可为 null）
     * @return 选中的弹药物品类型，无可用弹药返回 null
     */
    @Nullable
    public Item chooseReloadAmmo(ItemStack weapon, int required, boolean creative, @Nullable Level level) {
        WeaponState weaponState = new WeaponState(weapon);
        var preferred = weaponState.getSelectedAmmoType();

        if (preferred.hasSelection()) {
            ResourceLocation rl = ResourceLocation.tryParse(preferred.ammoItemId());
            if (rl != null) {
                Item preferredItem = BuiltInRegistries.ITEM.get(rl);
                if (preferredItem != null && preferredItem != net.minecraft.world.item.Items.AIR) {
                    ItemStack ammoStack = new ItemStack(preferredItem);
                    if (matchesCaliber(ammoStack, weapon, level)) {
                        if (creative || countAmmo(preferredItem) >= required) {
                            return preferredItem;
                        }
                    }
                }
            }
        }

        Item fallback = findFirstSufficientAmmoByInventoryOrder(weapon, required, level);

        if (fallback == null && creative) {
            return getDefaultAmmoForWeapon(weapon, level);
        }

        return fallback;
    }

    /**
     * 偏好弹种不足时，按背包槽位顺序查找第一种同口径且足量的弹药类型。
     * @param weapon 武器物品堆叠（用于匹配口径）
     * @param required 需要装填的数量
     * @param level 世界对象（用于应用配置覆盖后的口径匹配，可为 null）
     * @return 第一种足量弹药类型，未找到返回 null
     */
    @Nullable
    public Item findFirstSufficientAmmoByInventoryOrder(ItemStack weapon, int required, @Nullable Level level) {
        for (int i = 0; i < inventory.items.size(); i++) {
            if (shouldSkipSlot(i)) continue;
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()
                    && matchesCaliber(stack, weapon, level)
                    && countAmmo(stack.getItem()) >= required) {
                return stack.getItem();
            }
        }

        if (weaponSlotIndex != 40 && coreSlotIndex != 40) {
            ItemStack offhand = inventory.offhand.get(0);
            if (!offhand.isEmpty()
                    && matchesCaliber(offhand, weapon, level)
                    && countAmmo(offhand.getItem()) >= required) {
                return offhand.getItem();
            }
        }

        return null;
    }

    /**
     * 将当前消耗的弹种写入武器的 SELECTED_AMMO_TYPE（装填优先级记忆）。
     * @param weapon 武器物品堆叠
     * @param ammoItem 本次使用的弹药类型
     */
    public void recordAmmoType(ItemStack weapon, Item ammoItem) {
        String id = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
        weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(),
                new com.piranport.component.SelectedAmmoType(id));
    }

    private boolean shouldSkipSlot(int slotIndex) {
        return slotIndex == coreSlotIndex || slotIndex == weaponSlotIndex;
    }

    private static boolean matchesCaliber(ItemStack ammo, ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            int caliber = level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber();
            if (caliber <= 4) return ammo.is(ShipCoreItem.SMALL_SHELLS);
            if (caliber <= 8) return ammo.is(ShipCoreItem.MEDIUM_SHELLS);
            return ammo.is(ShipCoreItem.LARGE_SHELLS);
        }
        return false;
    }

    @Nullable
    private static Item getDefaultAmmoForWeapon(ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            int caliber = level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber();
            if (caliber <= 4) return ModItems.SMALL_AP_SHELL.get();
            if (caliber <= 8) return ModItems.MEDIUM_AP_SHELL.get();
            return ModItems.LARGE_AP_SHELL.get();
        }
        return null;
    }
}
