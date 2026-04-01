package com.piranport.dungeon.key;

import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Determines and manages the flagship (key-holder) of a dungeon instance.
 */
public final class FlagshipManager {
    private FlagshipManager() {}

    /**
     * Finds the dungeon key in a player's inventory matching the given instance ID.
     * Returns the slot index, or -1 if not found.
     */
    public static int findKeySlot(ServerPlayer player, UUID instanceId) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                UUID keyInstanceId = DungeonKeyItem.getInstanceId(stack);
                if (instanceId.equals(keyInstanceId)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Finds any dungeon key in a player's inventory (regardless of instance).
     * Returns the slot index, or -1 if not found.
     */
    public static int findAnyKeySlot(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the key ItemStack from a player's inventory for a given instance.
     */
    public static ItemStack getKeyStack(ServerPlayer player, UUID instanceId) {
        int slot = findKeySlot(player, instanceId);
        return slot >= 0 ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }

    /**
     * Transfers the key to another player in the same dungeon instance.
     * The key is removed from the source and added to the target's inventory.
     */
    public static boolean transferKey(ServerPlayer from, ServerPlayer to, UUID instanceId) {
        int slot = findKeySlot(from, instanceId);
        if (slot < 0) return false;

        ItemStack key = from.getInventory().getItem(slot).copy();
        from.getInventory().setItem(slot, ItemStack.EMPTY);

        if (!to.getInventory().add(key)) {
            // If target inventory is full, drop it
            to.drop(key, false);
        }
        return true;
    }
}
