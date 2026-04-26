package com.piranport;

import com.piranport.client.AircraftRenderer;
import com.piranport.client.PlaceableFoodRenderer;
import com.piranport.client.ReloadBarDecorator;
import com.piranport.client.WeaponReloadDecorator;
import com.piranport.component.AircraftInfo;
import com.piranport.menu.ReloadFacilityScreen;
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
        event.register(ModMenuTypes.RELOAD_FACILITY_MENU.get(), ReloadFacilityScreen::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.CYCLE_WEAPON);
        event.register(ModKeyMappings.FIRE_CONTROL_LOCK);
        event.register(ModKeyMappings.FIRE_CONTROL_ADD);
        event.register(ModKeyMappings.FIRE_CONTROL_CANCEL);
        event.register(ModKeyMappings.HIGHLIGHT_ENTITIES);
        event.register(ModKeyMappings.DEBUG_TOGGLE);
        event.register(ModKeyMappings.MANUAL_RELOAD);
        event.register(ModKeyMappings.TORPEDO_STEER_LEFT);
        event.register(ModKeyMappings.TORPEDO_STEER_RIGHT);
        event.register(ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE);
        event.register(ModKeyMappings.HIT_DISPLAY_TOGGLE);
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
        // Missile launchers — all types get cooldown bar
        event.register(ModItems.TERRIER_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SEA_DART_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SEACAT_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SY1_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.MK14_HARPOON_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SHIP_ROCKET_LAUNCHER.get(), weaponDecorator);
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
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PLACEABLE_FOOD.get(),
                PlaceableFoodRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MODEL_DEBUG.get(),
                com.piranport.client.ModelDebugBlockEntityRenderer::new);
        // Gungnir
        event.registerEntityRenderer(ModEntityTypes.GUNGNIR.get(),
                ThrownItemRenderer::new);
    }
}
