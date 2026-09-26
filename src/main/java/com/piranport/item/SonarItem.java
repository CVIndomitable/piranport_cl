package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import com.piranport.terminal.TerminalParameters;

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

    public int getWeight() { return TerminalParameters.getInt(parameterKey("weight"), weight); }
    public int getBaseWeight() { return weight; }
    public int getRadius() { return TerminalParameters.getInt(parameterKey("range"), radius); }
    public int getBaseRadius() { return radius; }

    private String parameterKey(String property) {
        var id = BuiltInRegistries.ITEM.getKey(this);
        return "equipment." + (id == null ? "sonar" : id) + "." + property;
    }

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
                tooltip.add(Component.translatable("tooltip.piranport.sonar.radius", getRadius())
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.piranport.sonar.weight", getWeight())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
