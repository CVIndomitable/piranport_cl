package com.piranport.aviation;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-side recon mode state. */
@OnlyIn(Dist.CLIENT)
public class ClientReconData {
    private static boolean inReconMode = false;
    private static int reconEntityId = -1;

    public static void setReconActive(int entityId) {
        inReconMode = true;
        reconEntityId = entityId;
    }

    public static void clearRecon() {
        inReconMode = false;
        reconEntityId = -1;
    }

    public static boolean isInReconMode() { return inReconMode; }
    public static int getReconEntityId() { return reconEntityId; }

    public static void handleReconStart(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(entityId);
        if (entity != null) {
            mc.setCameraEntity(entity);
            setReconActive(entityId);
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable("message.piranport.recon_enter"), true);
        }
    }

    public static void handleReconEnd() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.displayClientMessage(
                    Component.translatable("message.piranport.recon_exit"), true);
        }
        clearRecon();
    }
}
