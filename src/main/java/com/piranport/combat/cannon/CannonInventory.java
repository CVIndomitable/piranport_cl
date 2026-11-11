package com.piranport.combat.cannon;

import com.piranport.combat.TransformationManager;
import com.piranport.combat.data.WeaponState;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.piranport.item.ShipCoreItem;
import static com.piranport.combat.cannon.CannonAmmoRules.isCannonReadyToFire;

/** 火炮背包定位：核心位置、武器实例与同型可射击火炮选择。 */
public final class CannonInventory {
    private CannonInventory() {}

    public static int findCoreSlotIndex(Inventory inv, Player player, int weaponSlot) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                return i;
            }
        }
        if (weaponSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (oh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(oh)) {
                return 40;
            }
        }
        ItemStack configCore = TransformationManager.getCoreFromConfiguredSlot(player);
        if (configCore.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(configCore)) {
            return -2;
        }
        return -1;
    }

    public static int[] findBestArtillerySlot(Player player, Item weaponType, ItemStack coreStack) {
        return findReadyArtillerySlot(player, weaponType, -1);
    }

    static int[] findNextArtillerySlot(Player player, Item weaponType, int previousSlot) {
        return findReadyArtillerySlot(player, weaponType, previousSlot);
    }

    private static int[] findReadyArtillerySlot(Player player, Item weaponType, int previousSlot) {
        Inventory inv = player.getInventory();
        long now = player.level().getGameTime();
        java.util.function.IntPredicate ready = i -> {
            ItemStack stack = weaponAt(inv, i);
            return stack.getItem() == weaponType
                    && !new WeaponState(stack).isOnCooldown(now)
                    && !CannonStats.isCannonDamaged(stack, player.level())
                    && isCannonReadyToFire(stack, player.level());
        };
        int slot = previousSlot < 0 ? chooseReadySlot(inv.items.size(), inv.selected, ready)
                : chooseNextReadySlot(inv.items.size(), previousSlot, ready);
        return slot < 0 ? null : new int[]{slot, findCoreSlotIndex(inv, player, slot)};
    }

    static int chooseReadySlot(int size, int selected, java.util.function.IntPredicate ready) {
        if (selected >= 0 && selected < size && ready.test(selected)) return selected;
        for (int i = 0; i < size; i++) {
            if (i != selected && ready.test(i)) return i;
        }
        return ready.test(40) ? 40 : -1;
    }

    static int chooseNextReadySlot(int size, int previousSlot, java.util.function.IntPredicate ready) {
        int start = previousSlot == 40 ? size : previousSlot;
        for (int offset = 1; offset <= size + 1; offset++) {
            int index = (start + offset) % (size + 1);
            int slot = index == size ? 40 : index;
            if (ready.test(slot)) return slot;
        }
        return -1;
    }

    public static List<int[]> findMatchingArtillerySlots(Player player, Item weaponType, ItemStack coreStack) {
        List<int[]> result = new ArrayList<>();
        Inventory inv = player.getInventory();
        long now = player.level().getGameTime();

        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() == weaponType
                    && !new WeaponState(stack).isOnCooldown(now)
                    && isCannonReadyToFire(stack, player.level())) {
                int coreSlot = findCoreSlotIndex(inv, player, i);
                result.add(new int[]{i, coreSlot});
            }
        }

        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() == weaponType
                && !new WeaponState(offhand).isOnCooldown(now)
                && isCannonReadyToFire(offhand, player.level())) {
            int coreSlot = findCoreSlotIndex(inv, player, 40);
            result.add(new int[]{40, coreSlot});
        }

        return result;
    }

    /** 按实例定位，副手使用 40；不把脱离背包的旧 ItemStack 当成主手。 */
    public static int findWeaponSlot(Inventory inventory, ItemStack weapon) {
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (inventory.items.get(slot) == weapon) return slot;
        }
        return inventory.offhand.get(0) == weapon ? 40 : -1;
    }

    public static ItemStack weaponAt(Inventory inventory, int slot) {
        if (slot == 40) return inventory.offhand.get(0);
        return slot >= 0 && slot < inventory.items.size() ? inventory.items.get(slot) : ItemStack.EMPTY;
    }

    public static ItemStack coreAt(Player player, int slot) {
        return slot == -2 ? TransformationManager.getCoreFromConfiguredSlot(player)
                : weaponAt(player.getInventory(), slot);
    }
}
