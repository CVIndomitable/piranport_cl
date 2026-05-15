package com.piranport.artillery;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 炮弹物品基类。
 * Phase 1: 预留基类，功能与 AmmoItem 重叠，后续阶段扩展。
 */
public class ShellItem extends Item {
    private final int caliber;
    private final String shellType; // HE, AP, VT, TYPE3

    public ShellItem(Properties properties, int caliber, String shellType) {
        super(properties);
        this.caliber = caliber;
        this.shellType = shellType;
    }

    public int getCaliber() { return caliber; }
    public String getShellType() { return shellType; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.piranport.ammo_type." + shellType.toLowerCase() + "_shell"));
    }
}
