package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Simple ammo item that displays its ammo type in the tooltip. */
public class AmmoItem extends Item {
    private final String typeKey;

    public AmmoItem(Properties properties, String typeKey) {
        super(properties);
        this.typeKey = typeKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(typeKey)
                .withStyle(ChatFormatting.DARK_GREEN));
    }
}
