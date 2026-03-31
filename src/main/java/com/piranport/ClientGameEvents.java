package com.piranport;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.aviation.ClientReconData;
import com.piranport.combat.TransformationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

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

    /** Clean up all client-side static state on disconnect to prevent cross-server leaks. */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFireControlData.clear();
        ClientReconData.clearRecon();
        ClientTickHandler.resetClientState();
    }
}
