package com.piranport.dungeon.event;

import com.piranport.dungeon.data.NodeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Centralizes reward dispensing for resource nodes / first-clear / dungeon completion.
 * Handles chance roll, item lookup, inventory add with overflow drop, and optional
 * display-name capture for the result screen.
 */
public final class RewardDispatcher {
    private RewardDispatcher() {}

    /**
     * Give a single reward to the player.
     * @param namesOut optional list to append "ItemName xCount"; pass null if not needed.
     */
    public static void give(ServerPlayer player, NodeData.RewardEntry reward, List<String> namesOut) {
        if (reward.chance() < 1.0f && player.getRandom().nextFloat() > reward.chance()) return;

        Item item = reward.resolvedItem();
        if (item == null) return;

        ItemStack stack = new ItemStack(item, reward.count());
        if (namesOut != null) {
            namesOut.add(stack.getHoverName().getString() + " x" + reward.count());
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
