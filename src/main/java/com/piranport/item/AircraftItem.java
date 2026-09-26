package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.component.AircraftInfo;
import com.piranport.component.WeaponCategory;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
        if (ShipCoreCombat.tryFireFromInventory(level, player, hand)) {
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
        if (!other.is(ModItems.AVIATION_FUEL.get())) return false;

        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        AircraftDefinition definition = AircraftDefinitionService.resolve(stack);
        if (info == null || definition == null || info.currentFuel() >= com.piranport.aviation.AircraftStatsService.resolve(definition).fuelCapacity()) return false;

        stack.set(ModDataComponents.AIRCRAFT_INFO.get(), info.withCurrentFuel(
                com.piranport.aviation.AircraftStatsService.resolve(definition).fuelCapacity()));
        other.shrink(1);

        if (!player.level().isClientSide) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ExperienceShellItem.appendEnhancementTooltip(stack, tooltipComponents);

        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_type." + info.aircraftType().getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            if (ClientHooks.isClient() && ClientHooks.hasShiftDown()) {
                if (info.panelDamage() > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_damage",
                            String.format("%.1f", info.panelDamage())).withStyle(net.minecraft.ChatFormatting.RED));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_speed",
                        String.format("%.1f", info.panelSpeed())).withStyle(net.minecraft.ChatFormatting.GREEN));
            } else if (ClientHooks.isClient()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
            // 出击准备状态（燃料 + 对海挂载）：必须常驻可见，不折叠进 Shift 详情，
            // 否则玩家无法判断这架飞机现在能不能放飞
            if (info.currentFuel() > 0) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_fueled")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_fuel_short")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
            AircraftDefinition definition = AircraftDefinitionService.resolve(stack);
            if (definition != null && definition.requiresPayload()) {
                if (info.payloadLoaded()) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_payload_loaded")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_payload_empty")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        }
        ShipCoreCombat.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
