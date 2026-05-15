package com.piranport;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.aviation.ClientReconData;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.network.RecallAllAircraftPayload;
import com.piranport.network.SkinRevertPayload;
import com.piranport.skin.ClientSkinData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientGameEvents {

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

    /** 断开连接时清理所有客户端静态状态，防止跨服数据泄露 */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFireControlData.clear();
        ClientReconData.clearRecon();
        ClientTickHandler.resetClientState();
        ClientSkinData.clear();
        com.piranport.client.FireControlHudLayer.clearCache();
        com.piranport.dungeon.client.DungeonHudLayer.clearDungeonState();
        com.piranport.dungeon.network.ClientDungeonData.clear();
    }
}
