package com.piranport.artillery;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 炮弹物品基类。
 * Phase 1: 预留基类，功能与 AmmoItem 重叠，后续阶段扩展。
 *
 * @deprecated Phase 12: 功能与 {@link com.piranport.item.AmmoItem} 完全重叠，
 * 未在任何地方被引用。所有炮弹注册使用 AmmoItem。保留以防存档兼容需要，
 * 新代码不应使用此类。
 */
@Deprecated(forRemoval = false)
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
