package com.piranport.item;

import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.piranport.component.WeaponCategory;

import java.util.List;

public class CannonItem extends Item {
    private final float damage;
    private final int cooldownTicks;
    private final int barrelCount;

    public CannonItem(Properties properties, float damage, int cooldownTicks, int barrelCount) {
        super(properties);
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
        this.barrelCount = barrelCount;
    }

    public float getDamage() { return damage; }
    public int getCooldownTicks() { return cooldownTicks; }
    public int getBarrelCount() { return barrelCount; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ShipCoreItem.tryFireFromInventory(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Right-click ammo onto the cannon in the inventory to load one round (manual reload mode only).
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) return false;
        if (other.isEmpty() || !ShipCoreItem.matchesCaliber(other, stack)) return false;

        LoadedAmmo current = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (current.hasAmmo()) return false; // already loaded

        if (other.getCount() < barrelCount) return false; // not enough ammo for full salvo

        String ammoId = BuiltInRegistries.ITEM.getKey(other.getItem()).toString();
        stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(barrelCount, ammoId));
        other.shrink(barrelCount);

        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.barrel_count", barrelCount)
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.damage",
                        String.format("%.1f", damage)).withStyle(net.minecraft.ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                        String.format("%.1f", cooldownTicks / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
