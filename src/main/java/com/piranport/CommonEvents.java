package com.piranport;

import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.block.entity.CuttingBoardBlockEntity;
import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.block.entity.YubariWaterBucketBlockEntity;
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
        // Phase 30: single-slot handler for hopper input; extract is always EMPTY (output drops to world)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.CUTTING_BOARD.get(),
                (be, direction) -> be.getItemHandler()
        );
        // Yubari Water Bucket: infinite water source for pipes/tanks
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntityTypes.YUBARI_WATER_BUCKET.get(),
                YubariWaterBucketBlockEntity::getFluidHandler
        );
    }

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.FLOATING_TARGET.get(),
                FloatingTargetEntity.createAttributes().build());
    }
}
