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

public class TorpedoLauncherItem extends Item {
    private final int caliber;
    private final int tubeCount;
    private final int cooldownTicks;

    public TorpedoLauncherItem(Properties properties, int caliber, int tubeCount, int cooldownTicks) {
        super(properties);
        this.caliber = caliber;
        this.tubeCount = tubeCount;
        this.cooldownTicks = cooldownTicks;
    }

    public int getCaliber() {
        return caliber;
    }

    public int getTubeCount() {
        return tubeCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ShipCoreItem.tryFireFromInventory(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Right-click manual loading disabled — must use 装填设施 (Reload Facility) machine block.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        return false;
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
                tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.caliber", caliber)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.tubes", tubeCount)
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                        String.format("%.1f", cooldownTicks / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (stack.isDamageableItem()) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.durability",
                            stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
