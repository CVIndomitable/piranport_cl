package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftInfo;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AircraftItem extends Item {

    public AircraftItem(Properties properties) {
        super(properties);
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
     * Right-click aviation_fuel onto aircraft in inventory to manually load fuel (manual reload mode only).
     * One fuel item fills the aircraft to full capacity.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) return false;
        if (!other.is(ModItems.AVIATION_FUEL.get())) return false;

        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info == null || info.currentFuel() >= info.fuelCapacity()) return false;

        stack.set(ModDataComponents.AIRCRAFT_INFO.get(), info.withCurrentFuel(info.fuelCapacity()));
        other.shrink(1);

        if (!player.level().isClientSide) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.5f, 1.2f);

            // Apply FlammableEffect if enabled and player is currently transformed
            if (com.piranport.config.ModCommonConfig.FLAMMABLE_EFFECT_ENABLED.get()
                    && isPlayerTransformed(player)) {
                player.addEffect(new MobEffectInstance(ModMobEffects.FLAMMABLE, 999999, 0, false, true));
            }
        }
        return true;
    }

    /** Returns true if the player has any ship core in transformed state. */
    private static boolean isPlayerTransformed(Player player) {
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) {
            if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) return true;
        }
        ItemStack offhand = inv.offhand.get(0);
        return offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_type." + info.aircraftType().getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_ammo_capacity", info.ammoCapacity()));
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_damage",
                    String.format("%.1f", info.panelDamage())));
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_speed",
                    String.format("%.1f", info.panelSpeed())));
            tooltipComponents.add(Component.translatable("tooltip.piranport.weight", info.weight()));
            if (info.currentFuel() > 0) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_fueled")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_not_fueled")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
