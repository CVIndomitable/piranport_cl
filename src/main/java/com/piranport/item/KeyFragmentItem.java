package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 钥匙碎片 — 4 个碎片合成 1 把钥匙。
 */
public class KeyFragmentItem extends Item {

    private final int chapterNumber;

    public KeyFragmentItem(int chapterNumber, Properties properties) {
        super(properties);
        this.chapterNumber = chapterNumber;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.key_fragment.chapter", chapterNumber)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.piranport.key_fragment.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
