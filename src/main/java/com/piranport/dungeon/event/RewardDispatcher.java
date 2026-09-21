package com.piranport.dungeon.event;

import com.piranport.PiranPort;
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
        if (item == null) {
            // 决策/副本/21 §4.4：加载期校验已拦截未注册物品，这里是兜底——
            // 万一有遗漏，至少留下可追溯的日志，而不是静默什么都不发。
            PiranPort.LOGGER.warn("副本奖励物品未注册，已跳过发放: {}", reward.item());
            return;
        }

        ItemStack stack = new ItemStack(item, reward.count());
        if (namesOut != null) {
            namesOut.add(stack.getHoverName().getString() + " x" + reward.count());
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
