package com.piranport.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 客户端线导鱼雷状态 — 摄像机绑定到线导鱼雷 */
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
        if (entity == null) {
            // 实体尚未下发到本客户端（生成包与制导包乱序）或已超出视距卸载：
            // 不能提示"进入制导"，否则玩家以为已进入视角、实际摄像机还挂在自身上，
            // 且本端 active=false 不会发送任何制导输入，鱼雷将直线跑偏。
            // 服务端会在导线切断/鱼雷消失时下发 active=false 收尾，这里静默即可。
            return;
        }
        mc.setCameraEntity(entity);
        active = true;
        torpedoEntityId = entityId;
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable("message.piranport.torpedo_guidance_enter"), true);
        }
    }

    public static void handleEnd() {
        restoreLocalCamera();
        active = false;
        torpedoEntityId = -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable("message.piranport.torpedo_guidance_exit"), true);
        }
    }

    /** 断线重置：不发送游戏消息，但必须把相机交还给本地玩家。 */
    public static void resetClientState() {
        restoreLocalCamera();
        active = false;
        torpedoEntityId = -1;
    }

    private static void restoreLocalCamera() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() != null) {
            mc.setCameraEntity(mc.player);
        }
    }
}
