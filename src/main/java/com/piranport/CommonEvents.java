package com.piranport;

import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.entity.FloatingTargetEntity;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModEntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = PiranPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.STONE_MILL.get(),
                StoneMillBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.COOKING_POT.get(),
                CookingPotBlockEntity::getItemHandler
        );
    }

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.FLOATING_TARGET.get(),
                FloatingTargetEntity.createAttributes().build());
    }
}
