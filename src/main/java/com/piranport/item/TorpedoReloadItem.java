package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 鱼雷再装填强化部件 — 安装在舰装核心的强化槽中，
 * 使鱼雷发射器可以自动从弹药槽/背包中装填鱼雷。
 * 不安装此部件时，必须使用装填设施方块预装填鱼雷发射器。
 */
public class TorpedoReloadItem extends Item {
    private final int weight;

    public TorpedoReloadItem(Properties properties, int weight) {
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
