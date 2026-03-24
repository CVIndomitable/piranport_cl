package com.piranport;

import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.CycleWeaponPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientTickHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (ClientEvents.CYCLE_WEAPON_KEY.consumeClick()) {
            if (mc.player.getMainHandItem().getItem() instanceof ShipCoreItem
                    && TransformationManager.isTransformed(mc.player.getMainHandItem())) {
                PacketDistributor.sendToServer(new CycleWeaponPayload());
            }
        }
    }
}
