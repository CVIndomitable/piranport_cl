package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EngineItem extends Item {
    private final double speedBonus;
    private final int weight;

    public EngineItem(Properties properties, double speedBonus, int weight) {
        super(properties);
        this.speedBonus = speedBonus;
        this.weight = weight;
    }

    public double getSpeedBonus() { return speedBonus; }
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
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.piranport.engine.speed_bonus",
                        String.format("%.0f", speedBonus * 100)).withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
