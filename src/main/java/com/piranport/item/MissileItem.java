package com.piranport.item;

import com.piranport.entity.MissileEntity.MissileType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 导弹/火箭弹弹药物品。存储展示用伤害值和穿甲值（实际伤害由发射器决定）。
 */
public class MissileItem extends Item {

    private final MissileType ammoType;
    private final float displayDamage;
    private final float displayAP;

    public MissileItem(Properties properties, MissileType ammoType, float displayDamage) {
        this(properties, ammoType, displayDamage, 0f);
    }

    public MissileItem(Properties properties, MissileType ammoType,
                        float displayDamage, float displayAP) {
        super(properties);
        this.ammoType = ammoType;
        this.displayDamage = displayDamage;
        this.displayAP = displayAP;
    }

    public MissileType getAmmoType() {
        return ammoType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(ammoType.translationKey)
                .withStyle(ammoType.color));
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (displayAP > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.missile.damage_ap",
                            String.format("%.0f", displayDamage), String.format("%.0f", displayAP))
                            .withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.missile.damage",
                            String.format("%.0f", displayDamage)).withStyle(ChatFormatting.RED));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
