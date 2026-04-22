package com.piranport.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-side torpedo guidance state — camera is bound to a wire-guided torpedo. */
@OnlyIn(Dist.CLIENT)
public class ClientTorpedoGuidance {
    private static boolean active = false;
    private static int torpedoEntityId = -1;

    public static boolean isActive() { return active; }
    public static int getTorpedoEntityId() { return torpedoEntityId; }

    public static void handleStart(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(entityId);
        if (entity != null) {
            mc.setCameraEntity(entity);
            active = true;
            torpedoEntityId = entityId;
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable("message.piranport.torpedo_guidance_enter"), true);
        }
    }

    public static void handleEnd() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.displayClientMessage(
                    Component.translatable("message.piranport.torpedo_guidance_exit"), true);
        }
        active = false;
        torpedoEntityId = -1;
    }

    public static void resetClientState() {
        active = false;
        torpedoEntityId = -1;
    }
}
