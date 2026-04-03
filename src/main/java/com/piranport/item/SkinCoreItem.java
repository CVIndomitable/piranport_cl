package com.piranport.item;

import com.piranport.skin.SkinManager;
import net.minecraft.ChatFormatting;
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

public class SkinCoreItem extends Item {
    private final int skinId;

    public SkinCoreItem(Properties properties, int skinId) {
        super(properties);
        this.skinId = skinId;
    }

    public int getSkinId() {
        return skinId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            int currentSkin = SkinManager.getActiveSkin(player);

            if (currentSkin == skinId) {
                // Same skin already active → revert, don't consume (player already has it in hand)
                SkinManager.clearActiveSkin(sp);
                sp.displayClientMessage(
                        Component.translatable("message.piranport.skin_removed"), true);
            } else {
                // Return old skin core if switching
                if (currentSkin > 0) {
                    SkinManager.returnSkinCore(sp, currentSkin);
                }
                // Apply new skin, consume the core from hand
                SkinManager.setActiveSkin(sp, skinId);
                stack.shrink(1);
                sp.displayClientMessage(
                        Component.translatable("message.piranport.skin_applied", skinId), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.skin_core", skinId)
                .withStyle(ChatFormatting.AQUA));
    }
}
