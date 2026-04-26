package com.piranport;

import com.piranport.registry.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

@EventBusSubscriber(modid = PiranPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModFuelHandler {
    private static final int BURN_TIME = 16000;

    private ModFuelHandler() {}

    @SubscribeEvent
    public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
        Item item = event.getItemStack().getItem();
        if (item == ModItems.FUEL.get() || item == ModItems.AVIATION_FUEL.get()) {
            event.setBurnTime(BURN_TIME);
        }
    }
}
