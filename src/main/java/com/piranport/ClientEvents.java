package com.piranport;

import com.piranport.client.AircraftRenderer;
import com.piranport.client.CuttingBoardRenderer;
import com.piranport.client.PlaceableFoodRenderer;
import com.piranport.client.ReloadBarDecorator;
import com.piranport.client.WeaponReloadDecorator;
import com.piranport.component.AircraftInfo;
import com.piranport.menu.AmmoWorkbenchScreen;
import com.piranport.menu.CookingPotScreen;
import com.piranport.menu.FlightGroupScreen;
import com.piranport.menu.ReloadFacilityScreen;
import com.piranport.menu.ShipCoreScreen;
import com.piranport.menu.StoneMillScreen;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModKeyMappings;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientEvents {



    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register "fueled" item property: 1.0 when aircraft has fuel, 0.0 otherwise.
        // Models use this predicate to switch between loaded and empty textures.
        ResourceLocation fueled = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fueled");
        event.enqueueWork(() -> {
            // Register "fueled" property for ALL AircraftItem instances (including named variants)
            for (var entry : ModItems.ITEMS.getEntries()) {
                if (entry.get() instanceof com.piranport.item.AircraftItem) {
                    ItemProperties.register(entry.get(), fueled, (stack, level, entity, seed) -> {
                        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
                        return (info != null && info.currentFuel() > 0) ? 1.0f : 0.0f;
                    });
                }
            }
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHIP_CORE_MENU.get(), ShipCoreScreen::new);
        event.register(ModMenuTypes.STONE_MILL_MENU.get(), StoneMillScreen::new);
        event.register(ModMenuTypes.COOKING_POT_MENU.get(), CookingPotScreen::new);
        event.register(ModMenuTypes.FLIGHT_GROUP_MENU.get(), FlightGroupScreen::new);
        event.register(ModMenuTypes.RELOAD_FACILITY_MENU.get(), ReloadFacilityScreen::new);
        // Ammo Workbench
        event.register(ModMenuTypes.AMMO_WORKBENCH_MENU.get(), AmmoWorkbenchScreen::new);
        // Weapon Workbench
        event.register(ModMenuTypes.WEAPON_WORKBENCH_MENU.get(),
                com.piranport.menu.WeaponWorkbenchScreen::new);
        // Dungeon
        event.register(ModMenuTypes.DUNGEON_BOOK_MENU.get(),
                com.piranport.dungeon.client.DungeonBookScreen::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.CYCLE_WEAPON);
        event.register(ModKeyMappings.FIRE_CONTROL_LOCK);
        event.register(ModKeyMappings.FIRE_CONTROL_ADD);
        event.register(ModKeyMappings.FIRE_CONTROL_CANCEL);
        event.register(ModKeyMappings.OPEN_FLIGHT_GROUP);
        event.register(ModKeyMappings.HIGHLIGHT_ENTITIES);
        event.register(ModKeyMappings.TOGGLE_AUTO_LAUNCH);
        event.register(ModKeyMappings.DEBUG_TOGGLE);
        event.register(ModKeyMappings.MANUAL_RELOAD);
        event.register(ModKeyMappings.TORPEDO_STEER_LEFT);
        event.register(ModKeyMappings.TORPEDO_STEER_RIGHT);
        event.register(ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE);
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        ReloadBarDecorator decorator = new ReloadBarDecorator();
        event.register(ModItems.SMALL_SHIP_CORE.get(), decorator);
        event.register(ModItems.MEDIUM_SHIP_CORE.get(), decorator);
        event.register(ModItems.LARGE_SHIP_CORE.get(), decorator);

        // Weapon reload bar (no-GUI mode — durability-style bar on weapon items)
        WeaponReloadDecorator weaponDecorator = new WeaponReloadDecorator();
        event.register(ModItems.SINGLE_SMALL_GUN.get(), weaponDecorator);
        event.register(ModItems.SMALL_GUN.get(), weaponDecorator);
        event.register(ModItems.MEDIUM_GUN.get(), weaponDecorator);
        event.register(ModItems.LARGE_GUN.get(), weaponDecorator);
        event.register(ModItems.TWIN_TORPEDO_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.QUAD_TORPEDO_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.DEPTH_CHARGE_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), weaponDecorator);
        event.register(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), weaponDecorator);
        // Missile launchers — all types get cooldown bar
        event.register(ModItems.TERRIER_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SEA_DART_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SEACAT_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SY1_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.MK14_HARPOON_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SHIP_ROCKET_LAUNCHER.get(), weaponDecorator);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skinModel : event.getSkins()) {
            var renderer = event.getSkin(skinModel);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(
                        new com.piranport.client.SkinOverlayLayer(playerRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.piranport.client.model.B25Model.LAYER_LOCATION,
                com.piranport.client.model.B25Model::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.F4FModel.LAYER_LOCATION,
                com.piranport.client.model.F4FModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CANNON_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TORPEDO_ENTITY.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.AIRCRAFT_ENTITY.get(),
                AircraftRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLOATING_TARGET.get(),
                net.minecraft.client.renderer.entity.ArmorStandRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.AERIAL_BOMB.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.BULLET.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SANSHIKI_PELLET.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEPTH_CHARGE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MISSILE_ENTITY.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLARE_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.RAILGUN_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CUTTING_BOARD.get(),
                CuttingBoardRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PLACEABLE_FOOD.get(),
                PlaceableFoodRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.B25_DEBUG.get(),
                com.piranport.client.B25DebugBlockEntityRenderer::new);
        // Dungeon enemies
        event.registerEntityRenderer(ModEntityTypes.LOW_TIER_DESTROYER.get(),
                com.piranport.client.LowTierDestroyerRenderer::new);
        // Dungeon transport plane (artillery intro script)
        event.registerEntityRenderer(ModEntityTypes.DUNGEON_TRANSPORT_PLANE.get(),
                com.piranport.client.DungeonTransportPlaneRenderer::new);
        // Dungeon entities (use NoopRenderer for now — visual effects are done via particles in tick())
        event.registerEntityRenderer(ModEntityTypes.DUNGEON_PORTAL.get(),
                com.piranport.dungeon.client.DungeonPortalRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.LOOT_SHIP.get(),
                com.piranport.dungeon.client.LootShipRenderer::new);
        // Gungnir
        event.registerEntityRenderer(ModEntityTypes.GUNGNIR.get(),
                ThrownItemRenderer::new);
        // Deep Ocean projectile
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_PROJECTILE.get(),
                ThrownItemRenderer::new);
        // Deep Ocean NPC entities (placeholder renderer with fleet state particles)
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_SUPPLY.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_DESTROYER.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_BATTLESHIP.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_CARRIER.get(),
                com.piranport.client.DeepOceanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_SUBMARINE.get(),
                com.piranport.client.DeepOceanRenderer::new);
        // Ship Girl NPC
        event.registerEntityRenderer(ModEntityTypes.SHIP_GIRL.get(),
                com.piranport.client.ShipGirlRenderer::new);
    }
}
