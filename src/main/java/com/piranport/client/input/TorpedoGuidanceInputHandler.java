package com.piranport.client.input;

import com.piranport.client.AmmoSelectOverlay;
import com.piranport.combat.ClientTorpedoGuidance;
import com.piranport.network.TorpedoGuidanceInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 鱼雷制导输入处理（WASD/空格/退出），将玩家旋转镜像到鱼雷并发送视线方向到服务端。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 * <p><b>生命周期</b>: 状态在 {@link ClientTorpedoGuidance} 中管理，
 *   在 {@link com.piranport.ClientGameEvents#onClientDisconnect} 中清理。
 */
public class TorpedoGuidanceInputHandler {

    private TorpedoGuidanceInputHandler() {}

    /** 每 tick 处理鱼雷制导输入。返回 true 表示当前处于制导模式。 */
    public static boolean tick(Minecraft mc) {
        boolean inTorpedoGuidance = ClientTorpedoGuidance.isActive();
        if (!inTorpedoGuidance) return false;

        // 强制关闭弹药轮盘，避免与制导的摄像机控制冲突
        if (AmmoSelectOverlay.isOpen()) {
            AmmoSelectOverlay.close();
        }

        if (mc.level != null) {
            Entity torpedoEntity = mc.level.getEntity(ClientTorpedoGuidance.getTorpedoEntityId());
            if (torpedoEntity != null) {
                if (mc.getCameraEntity() != torpedoEntity) {
                    mc.setCameraEntity(torpedoEntity);
                }
                torpedoEntity.setXRot(mc.player.getXRot());
                torpedoEntity.setYRot(mc.player.getYRot());
                torpedoEntity.xRotO = mc.player.xRotO;
                torpedoEntity.yRotO = mc.player.yRotO;
            } else {
                ClientTorpedoGuidance.handleEnd();
                inTorpedoGuidance = false;
            }
        }

        if (inTorpedoGuidance && mc.player.tickCount % 2 == 0) {
            Vec3 look = mc.player.getLookAngle();
            PacketDistributor.sendToServer(new TorpedoGuidanceInputPayload(
                    (float) look.x, (float) look.y, (float) look.z));
        }

        return inTorpedoGuidance;
    }
}
