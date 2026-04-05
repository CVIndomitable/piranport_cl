package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SonarItem extends Item {
    private final int weight;

    public SonarItem(Properties properties, int weight) {
        super(properties);
        this.weight = weight;
    }

    public int getWeight() { return weight; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
