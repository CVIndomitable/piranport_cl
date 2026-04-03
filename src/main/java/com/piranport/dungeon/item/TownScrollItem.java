package com.piranport.dungeon.item;

import com.piranport.aviation.ReconManager;
import com.piranport.dungeon.DungeonConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Town scroll — allows players to leave a dungeon instance.
 * One is automatically given when entering a dungeon.
 */
public class TownScrollItem extends Item {

    public TownScrollItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Block usage while in recon mode to prevent aircraft loss
        if (level.isClientSide()) {
            if (com.piranport.aviation.ClientReconData.isInReconMode()) {
                player.displayClientMessage(
                        Component.translatable("item.piranport.town_scroll.recon_blocked"), true);
                return InteractionResultHolder.fail(stack);
            }
            // Open confirmation screen client-side
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.piranport.dungeon.client.TownScrollScreen());
            return InteractionResultHolder.success(stack);
        }

        if (ReconManager.isInRecon(player.getUUID())) {
            return InteractionResultHolder.fail(stack);
        }

        // Server-side handled by TownScrollUsePayload after confirmation
        player.getCooldowns().addCooldown(this, DungeonConstants.TOWN_SCROLL_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.piranport.town_scroll.tooltip"));
    }
}
