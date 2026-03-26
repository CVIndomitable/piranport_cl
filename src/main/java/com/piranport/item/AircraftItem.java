package com.piranport.item;

import com.piranport.component.AircraftInfo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AircraftItem extends Item {

    public AircraftItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_damage",
                    String.format("%.1f", info.panelDamage())));
            tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_speed",
                    String.format("%.1f", info.panelSpeed())));
            tooltipComponents.add(Component.translatable("tooltip.piranport.weight", info.weight()));
            if (info.currentFuel() > 0) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.fuel",
                        info.currentFuel(), info.fuelCapacity()));
            }
        }
    }
}
