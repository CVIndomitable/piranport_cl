package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SonarItem extends Item {
    private final int weight;
    /** Phase 27：策划 §3.6 表 3.2 - 声呐扫描半径（标准/改进/先进 = 24/32/40） */
    private final int radius;

    public SonarItem(Properties properties, int weight) {
        this(properties, weight, 24); // 默认标准型半径 24
    }

    public SonarItem(Properties properties, int weight, int radius) {
        super(properties);
        this.weight = weight;
        this.radius = radius;
    }

    public int getWeight() { return weight; }
    public int getRadius() { return radius; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        if (ClientHooks.isClient()) {
            if (ClientHooks.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.piranport.sonar.radius", radius)
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.piranport.sonar.weight", weight)
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}