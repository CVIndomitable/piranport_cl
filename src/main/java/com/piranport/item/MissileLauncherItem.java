package com.piranport.item;

import com.piranport.entity.MissileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * 导弹/火箭弹发射器。
 * <p>
 * 反舰导弹 / 火箭弹：可连续发射 burstCount 次（无冷却），仅在装填设施装填。
 * 防空导弹：每次发射后 cooldownTicks 冷却，自动从弹药库消耗弹药。
 */
public class MissileLauncherItem extends Item {

    private final MissileEntity.MissileType missileType;
    private final float damage;
    private final float armorPen;
    private final float explosionPower;
    private final int burstCount;
    private final int cooldownTicks;
    private final Supplier<Item> ammoItem;

    public MissileLauncherItem(Properties properties, MissileEntity.MissileType missileType,
                                float damage, float armorPen, float explosionPower,
                                int burstCount, int cooldownTicks, Supplier<Item> ammoItem) {
        super(properties);
        this.missileType = missileType;
        this.damage = damage;
        this.armorPen = armorPen;
        this.explosionPower = explosionPower;
        this.burstCount = burstCount;
        this.cooldownTicks = cooldownTicks;
        this.ammoItem = ammoItem;
    }

    public MissileEntity.MissileType getMissileType() { return missileType; }
    public float getDamage() { return damage; }
    public float getArmorPen() { return armorPen; }
    public float getExplosionPower() { return explosionPower; }
    public int getBurstCount() { return burstCount; }
    public int getCooldownTicks() { return cooldownTicks; }
    public Item getAmmoItem() { return ammoItem.get(); }

    /** 是否为手动装填模式（反舰/火箭：需要在装填设施装弹）。 */
    public boolean isManualReload() {
        return missileType == MissileEntity.MissileType.ANTI_SHIP
                || missileType == MissileEntity.MissileType.ROCKET;
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
        // 导弹类型
        String typeKey = switch (missileType) {
            case ANTI_SHIP -> "tooltip.piranport.missile.type.anti_ship";
            case ANTI_AIR -> "tooltip.piranport.missile.type.anti_air";
            case ROCKET -> "tooltip.piranport.missile.type.rocket";
        };
        tooltipComponents.add(Component.translatable(typeKey).withStyle(ChatFormatting.GRAY));

        // 伤害
        if (armorPen > 0) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.missile.damage_ap",
                    String.format("%.0f", damage), String.format("%.0f", armorPen))
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.piranport.missile.damage",
                    String.format("%.0f", damage)).withStyle(ChatFormatting.RED));
        }

        // 连装数（反舰/火箭）
        if (isManualReload() && burstCount > 1) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.missile.burst", burstCount)
                    .withStyle(ChatFormatting.AQUA));
        }

        // 爆炸（防空/火箭）
        if (missileType != MissileEntity.MissileType.ANTI_SHIP) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.missile.explosion",
                    String.format("%.1f", explosionPower)).withStyle(ChatFormatting.GOLD));
        }

        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
