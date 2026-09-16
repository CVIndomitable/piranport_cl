package com.piranport.dungeon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 通关纪念章（出击纪念章）。
 *
 * <p>由副本通关奖励必掉 1 个，用于合成第 2 章起钥匙时的章节门控消耗。
 * 纪念章在被合成钥匙时消耗，不累积库存。</p>
 */
public class DeployMedalItem extends Item {

    public DeployMedalItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.piranport.deploy_medal.desc")
                .withStyle(ChatFormatting.GOLD));
    }
}
