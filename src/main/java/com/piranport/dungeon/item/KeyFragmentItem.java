package com.piranport.dungeon.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 钥匙碎片。
 *
 * <p>由深海 NPC 低概率掉落，4 个碎片合成 1 把当前进度章的完整钥匙。
 * 碎片只掉落不高于玩家当前进度章节的钥匙——防止刷出超前内容。</p>
 */
public class KeyFragmentItem extends Item {

    public KeyFragmentItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.piranport.key_fragment.desc")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("4 x 碎片 = 1 把钥匙").withStyle(ChatFormatting.GRAY));
    }
}
