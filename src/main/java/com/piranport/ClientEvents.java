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

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {



    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册 "fueled" 物品属性：有燃料时为 1.0，否则为 0.0。
        // 模型使用此谓词在装载和空载纹理之间切换。
        ResourceLocation fueled = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fueled");
        event.enqueueWork(() -> {
            // 为所有 AircraftItem 实例注册 "fueled" 属性（包括命名变体）
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
        event.register(ModMenuTypes.STONE_MILL_MENU.get(), StoneMillScreen::new);
        event.register(ModMenuTypes.COOKING_POT_MENU.get(), CookingPotScreen::new);
        event.register(ModMenuTypes.FLIGHT_GROUP_MENU.get(), FlightGroupScreen::new);
        event.register(ModMenuTypes.RELOAD_FACILITY_MENU.get(), ReloadFacilityScreen::new);
        // 弹药工作台
        event.register(ModMenuTypes.AMMO_WORKBENCH_MENU.get(), AmmoWorkbenchScreen::new);
        // 武器工作台
        event.register(ModMenuTypes.WEAPON_WORKBENCH_MENU.get(),
                com.piranport.menu.WeaponWorkbenchScreen::new);
        // 副本
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
        event.register(ModKeyMappings.SWITCH_AMMO);
        event.register(ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE);
        event.register(ModKeyMappings.HIT_DISPLAY_TOGGLE);
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        ReloadBarDecorator decorator = new ReloadBarDecorator();
        event.register(ModItems.SMALL_SHIP_CORE.get(), decorator);
        event.register(ModItems.MEDIUM_SHIP_CORE.get(), decorator);
        event.register(ModItems.LARGE_SHIP_CORE.get(), decorator);

        // 武器装填条（无GUI模式——武器物品上的耐久条样式）
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
        // 导弹发射器——所有类型都显示冷却条
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
        event.registerEntityRenderer(ModEntityTypes.SHELL_PROJECTILE.get(),
                ThrownItemRenderer::new);
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
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MODEL_DEBUG.get(),
                com.piranport.client.ModelDebugBlockEntityRenderer::new);
        // 副本敌人
        event.registerEntityRenderer(ModEntityTypes.LOW_TIER_DESTROYER.get(),
                com.piranport.client.LowTierDestroyerRenderer::new);
        // 副本运输机（炮击开场脚本）
        event.registerEntityRenderer(ModEntityTypes.DUNGEON_TRANSPORT_PLANE.get(),
                com.piranport.client.DungeonTransportPlaneRenderer::new);
        // 副本实体（目前使用 NoopRenderer——视觉效果通过 tick() 中的粒子实现）
        event.registerEntityRenderer(ModEntityTypes.DUNGEON_PORTAL.get(),
                com.piranport.dungeon.client.DungeonPortalRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.LOOT_SHIP.get(),
                com.piranport.dungeon.client.LootShipRenderer::new);
        // 冈格尼尔
        event.registerEntityRenderer(ModEntityTypes.GUNGNIR.get(),
                ThrownItemRenderer::new);
        // 深海投射物
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_PROJECTILE.get(),
                ThrownItemRenderer::new);
        // 深海 NPC 实体（带舰队状态粒子的占位渲染器）
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
        // 舰娘 NPC
        event.registerEntityRenderer(ModEntityTypes.SHIP_GIRL.get(),
                com.piranport.client.ShipGirlRenderer::new);
    }
}
