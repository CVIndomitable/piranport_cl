package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 通关纪念章 — 章节通关奖励，用于合成下一章节钥匙。
 */
public class DeployMedalItem extends Item {

    private final int chapterNumber;

    public DeployMedalItem(int chapterNumber, Properties properties) {
        super(properties);
        this.chapterNumber = chapterNumber;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.deploy_medal.chapter", chapterNumber)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.piranport.deploy_medal.use")
                .withStyle(ChatFormatting.GRAY));
    }
}
