package com.piranport.compat.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public final class AmmoConsumer {
    private AmmoConsumer() {}

    public static Player ownerPlayer(EntityMaid maid) {
        return maid.getOwner() instanceof Player p ? p : null;
    }

    public static boolean isFreebie(Player player) {
        return player != null && player.getAbilities().instabuild;
    }

    public static int count(Player player, Predicate<ItemStack> pred) {
        if (player == null) return 0;
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (pred.test(s)) total += s.getCount();
        }
        return total;
    }

    public static boolean has(Player player, Predicate<ItemStack> pred, int amount) {
        if (player == null) return false;
        if (isFreebie(player)) return true;
        return count(player, pred) >= amount;
    }

    public static int consume(Player player, Predicate<ItemStack> pred, int amount) {
        if (player == null || amount <= 0) return 0;
        if (isFreebie(player)) return amount;
        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!pred.test(s)) continue;
            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;
        }
        inv.setChanged();
        return amount - remaining;
    }

    public static boolean hasItem(Player player, Item item, int amount) {
        return has(player, s -> !s.isEmpty() && s.is(item), amount);
    }

    public static int consumeItem(Player player, Item item, int amount) {
        return consume(player, s -> !s.isEmpty() && s.is(item), amount);
    }

    /**
     * Get player's preferred ammo type from a list of candidates.
     * Priority: offhand item > first available item in list.
     * Returns null if player has none of the candidate ammo types.
     */
    public static Item getPreferredAmmo(Player player, List<Item> candidates) {
        if (player == null || candidates.isEmpty()) return null;

        // Check offhand first - if player is holding ammo, use that type
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            Item offhandItem = offhand.getItem();
            if (candidates.contains(offhandItem) && hasItem(player, offhandItem, 1)) {
                return offhandItem;
            }
        }

        // Otherwise, return first available ammo type from candidates
        for (Item candidate : candidates) {
            if (hasItem(player, candidate, 1)) {
                return candidate;
            }
        }

        return null;
    }
}
