package com.piranport.client;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.aviation.ClientReconData;
import com.piranport.client.CameraShakeHandler;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModClientConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.client.input.ClientInputCoordinator;
import com.piranport.PiranPort;
import com.piranport.network.RecallAllAircraftPayload;
import com.piranport.network.SkinRevertPayload;
import com.piranport.skin.ClientSkinData;
import net.minecraft.ChatFormatting;
import com.piranport.artillery.ArtilleryItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientGameEvents {

    /** Phase 5: 瞄准镜模式下缩放 FOV */
    @SubscribeEvent
    public static void onComputeFov(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov event) {
        if (com.piranport.client.ClientScopeHandler.isFullyScoped()) {
            float zoom = com.piranport.client.ClientScopeHandler.getZoomLevel();
            if (zoom > 0.1f) {
                event.setFOV(event.getFOV() / zoom);
            }
        }
    }

    // P1修复: 使用静态RandomSource实例，避免每帧创建导致震动不连续
    private static final RandomSource SHAKE_RANDOM = RandomSource.create();

    /** Phase 10: 屏幕震动 — 在相机角度计算后施加随机偏移 */
    @SubscribeEvent
    public static void onComputeCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (CameraShakeHandler.isShaking()) {
            float intensity = CameraShakeHandler.getShakeIntensity()
                    * ModClientConfig.SCREEN_SHAKE_MULTIPLIER.get().floatValue();
            if (intensity > 0) {
                event.setYaw(event.getYaw() + (SHAKE_RANDOM.nextFloat() - 0.5f) * intensity * 2);
                event.setPitch(event.getPitch() + (SHAKE_RANDOM.nextFloat() - 0.5f) * intensity * 2);
                event.setRoll(event.getRoll() + (SHAKE_RANDOM.nextFloat() - 0.5f) * intensity * 0.5f);
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!Screen.hasShiftDown()) return;
        int load = TransformationManager.getItemLoad(event.getItemStack());
        if (load == 0) return;
        event.getToolTip().add(
                Component.translatable("tooltip.piranport.weight", load)
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    /** 空手 + 蹲下 + 右键 → 恢复皮肤并返还核心；空手 + 右键（不蹲下、无GUI、已变身）→ 召回所有飞机 */
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        // 侦察模式下阻止所有空手操作以防止错误
        if (ClientReconData.isInReconMode()) return;

        if (event.getEntity().isShiftKeyDown()) {
            int currentSkin = ClientSkinData.getActiveSkin(event.getEntity().getUUID());
            if (currentSkin > 0) {
                PacketDistributor.sendToServer(new SkinRevertPayload());
            }
            return;
        }
        // 无GUI模式：空手右键 → 召回所有飞机
        if (!ModCommonConfig.isShipCoreGuiEnabled()
                && TransformationManager.isPlayerTransformed(event.getEntity())) {
            PacketDistributor.sendToServer(new RecallAllAircraftPayload());
        }
    }

    /** 侦察模式下隐藏玩家手/手臂 — 摄像机在飞机上 */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientReconData.isInReconMode()) {
            event.setCanceled(true);
        }
    }

    /** 烟雾隐身：观察者有隐身效果（身处烟雾）时不渲染其他实体 */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        var viewer = Minecraft.getInstance().player;
        if (viewer != null && viewer.hasEffect(MobEffects.INVISIBILITY) && event.getEntity() != viewer) {
            event.setCanceled(true);
        }
    }

    /**
     * 手持火炮时在输入端完全屏蔽左键挖掘。
     * 在 player.swing() 之前触发，能同时阻止手臂动画和进度条。
     * 若需调试可取消下方 LOGGER 注释。
     */
    @SubscribeEvent
    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() instanceof ArtilleryItem) {
            event.setCanceled(true);
            event.setSwingHand(false);
            // 调试日志（如需启用，取消注释即可）：
            // PiranPort.LOGGER.debug("Blocked mining attempt while holding artillery");
        }
    }

    /** Issue 6: 手持火炮时屏蔽左键挖掘，防止装填时持续挖掘 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        var player = event.getEntity();
        if (player == null) return;
        
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            event.setCanceled(true);
        }
    }

    /** 断开连接时清理所有客户端静态状态，防止跨服数据泄露 */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFireControlData.clear();
        ClientReconData.clearRecon();
        ClientInputCoordinator.resetClientState();
        ClientSkinData.clear();
        com.piranport.client.FireControlHudLayer.clearCache();
        com.piranport.client.ClientScopeHandler.clear();
        com.piranport.dungeon.client.DungeonHudLayer.clearDungeonState();
        com.piranport.dungeon.network.ClientDungeonData.clear();
        // P0修复: 清理配置缓存，防止跨服务器配置污染
        com.piranport.artillery.config.override.ClientConfigCache.clearCache();
    }
}
