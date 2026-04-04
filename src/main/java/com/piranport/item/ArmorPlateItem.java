package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorPlateItem extends Item {
    private final int armorBonus;
    private final int weight;
    private final int protectionLevel;

    public ArmorPlateItem(Properties properties, int armorBonus, int weight, int protectionLevel) {
        super(properties);
        this.armorBonus = armorBonus;
        this.weight = weight;
        this.protectionLevel = protectionLevel;
    }

    public int getArmorBonus() { return armorBonus; }
    public int getWeight() { return weight; }
    public int getProtectionLevel() { return protectionLevel; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.armor_bonus", armorBonus)
                .withStyle(ChatFormatting.BLUE));
        if (protectionLevel > 0) {
            tooltip.add(Component.translatable("tooltip.piranport.protection_level", protectionLevel)
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.piranport.weight", weight)
                .withStyle(ChatFormatting.GRAY));
    }
}
