package com.piranport;

import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.block.entity.CuttingBoardBlockEntity;
import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.block.entity.YubariWaterBucketBlockEntity;
import com.piranport.entity.FloatingTargetEntity;
import com.piranport.entity.LowTierDestroyerEntity;
import com.piranport.npc.deepocean.DeepOceanBattleCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanBattleshipEntity;
import com.piranport.npc.deepocean.DeepOceanCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanDestroyerEntity;
import com.piranport.npc.deepocean.DeepOceanHeavyCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanLightCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanLightCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
import com.piranport.npc.deepocean.DeepOceanSupplyEntity;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModEntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
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
        event.put(ModEntityTypes.LOW_TIER_DESTROYER.get(),
                LowTierDestroyerEntity.createAttributes().build());
        // Deep Ocean NPC entities
        event.put(ModEntityTypes.DEEP_OCEAN_SUPPLY.get(),
                DeepOceanSupplyEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_DESTROYER.get(),
                DeepOceanDestroyerEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER.get(),
                DeepOceanLightCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER.get(),
                DeepOceanHeavyCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER.get(),
                DeepOceanBattleCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_BATTLESHIP.get(),
                DeepOceanBattleshipEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER.get(),
                DeepOceanLightCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_CARRIER.get(),
                DeepOceanCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_SUBMARINE.get(),
                DeepOceanSubmarineEntity.createAttributes().build());
        // Ship Girl NPC
        event.put(ModEntityTypes.SHIP_GIRL.get(),
                ShipGirlEntity.createAttributes().build());
    }
}
