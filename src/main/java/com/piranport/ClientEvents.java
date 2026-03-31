package com.piranport;

import com.piranport.client.AircraftRenderer;
import com.piranport.client.CuttingBoardRenderer;
import com.piranport.client.PlaceableFoodRenderer;
import com.piranport.client.ReloadBarDecorator;
import com.piranport.component.AircraftInfo;
import com.piranport.menu.CookingPotScreen;
import com.piranport.menu.FlightGroupScreen;
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
import net.minecraft.client.KeyMapping;
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

    public static final KeyMapping CYCLE_WEAPON_KEY = new KeyMapping(
            "key.piranport.cycle_weapon", GLFW.GLFW_KEY_V, "key.categories.piranport");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register "fueled" item property: 1.0 when aircraft has fuel, 0.0 otherwise.
        // Models use this predicate to switch between loaded and empty textures.
        ResourceLocation fueled = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fueled");
        event.enqueueWork(() -> {
            for (var item : new net.minecraft.world.item.Item[]{
                    ModItems.FIGHTER_SQUADRON.get(),
                    ModItems.DIVE_BOMBER_SQUADRON.get(),
                    ModItems.TORPEDO_BOMBER_SQUADRON.get(),
                    ModItems.LEVEL_BOMBER_SQUADRON.get(),
                    ModItems.RECON_SQUADRON.get()
            }) {
                ItemProperties.register(item, fueled, (stack, level, entity, seed) -> {
                    AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
                    return (info != null && info.currentFuel() > 0) ? 1.0f : 0.0f;
                });
            }
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHIP_CORE_MENU.get(), ShipCoreScreen::new);
        event.register(ModMenuTypes.STONE_MILL_MENU.get(), StoneMillScreen::new);
        event.register(ModMenuTypes.COOKING_POT_MENU.get(), CookingPotScreen::new);
        event.register(ModMenuTypes.FLIGHT_GROUP_MENU.get(), FlightGroupScreen::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_WEAPON_KEY);
        event.register(ModKeyMappings.FIRE_CONTROL_LOCK);
        event.register(ModKeyMappings.FIRE_CONTROL_ADD);
        event.register(ModKeyMappings.FIRE_CONTROL_CANCEL);
        event.register(ModKeyMappings.OPEN_FLIGHT_GROUP);
        event.register(ModKeyMappings.HIGHLIGHT_ENTITIES);
        event.register(ModKeyMappings.DEBUG_TOGGLE);
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        ReloadBarDecorator decorator = new ReloadBarDecorator();
        event.register(ModItems.SMALL_SHIP_CORE.get(), decorator);
        event.register(ModItems.MEDIUM_SHIP_CORE.get(), decorator);
        event.register(ModItems.LARGE_SHIP_CORE.get(), decorator);
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
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CUTTING_BOARD.get(),
                CuttingBoardRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PLACEABLE_FOOD.get(),
                PlaceableFoodRenderer::new);
    }
}
