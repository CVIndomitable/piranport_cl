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

    /** Empty hand + shift + right click → revert skin and return core. */
    /** Empty hand + right click (no shift, no-GUI, transformed) → recall all aircraft. */
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        // Block all empty-hand actions while in recon mode to prevent errors
        if (ClientReconData.isInReconMode()) return;

        if (event.getEntity().isShiftKeyDown()) {
            int currentSkin = ClientSkinData.getActiveSkin(event.getEntity().getUUID());
            if (currentSkin > 0) {
                PacketDistributor.sendToServer(new SkinRevertPayload());
            }
            return;
        }
        // No-GUI mode: empty hand right-click → recall all aircraft
        if (!ModCommonConfig.isShipCoreGuiEnabled()
                && TransformationManager.isPlayerTransformed(event.getEntity())) {
            PacketDistributor.sendToServer(new RecallAllAircraftPayload());
        }
    }

    /** Hide player hands/arms while in recon mode — camera is on the aircraft. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientReconData.isInReconMode()) {
            event.setCanceled(true);
        }
    }

    /** Clean up all client-side static state on disconnect to prevent cross-server leaks. */
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
