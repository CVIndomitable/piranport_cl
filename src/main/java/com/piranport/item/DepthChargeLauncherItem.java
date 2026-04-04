package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.piranport.component.WeaponCategory;
import com.piranport.registry.ModDataComponents;

import java.util.List;

/**
 * 深弹投射器 — 消耗深水炸弹向前投掷，不同型号有不同散布模式。
 * 标准型：1枚，直线；改良型：2枚，前后散布；先进型：3枚，三角形散布。
 */
public class DepthChargeLauncherItem extends Item {

    public enum SpreadPattern {
        /** 单发直投 */
        SINGLE,
        /** 两枚，一前一后 */
        FRONT_BACK,
        /** 三枚，三角形散布 */
        TRIANGLE
    }

    private final int chargeCount;
    private final int cooldownTicks;
    private final SpreadPattern spreadPattern;

    public DepthChargeLauncherItem(Properties properties, int chargeCount, int cooldownTicks,
                                    SpreadPattern spreadPattern) {
        super(properties);
        this.chargeCount = chargeCount;
        this.cooldownTicks = cooldownTicks;
        this.spreadPattern = spreadPattern;
    }

    public int getChargeCount() {
        return chargeCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public SpreadPattern getSpreadPattern() {
        return spreadPattern;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ShipCoreItem.tryFireFromInventory(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltipComponents.add(Component.translatable("tooltip.piranport.dc_launcher.count", chargeCount)
                .withStyle(ChatFormatting.AQUA));
        String patternKey = switch (spreadPattern) {
            case SINGLE -> "tooltip.piranport.dc_launcher.pattern.single";
            case FRONT_BACK -> "tooltip.piranport.dc_launcher.pattern.front_back";
            case TRIANGLE -> "tooltip.piranport.dc_launcher.pattern.triangle";
        };
        tooltipComponents.add(Component.translatable(patternKey).withStyle(ChatFormatting.GRAY));
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
