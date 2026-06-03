package com.piranport.client.input;

import com.piranport.aviation.ClientReconData;
import com.piranport.client.AmmoSelectOverlay;
import com.piranport.client.ModKeyMappings;
import com.piranport.network.ReconControlPayload;
import com.piranport.network.ReconExitPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 侦察模式输入处理（WASD/空格/潜行），将玩家鼠标旋转镜像到侦察实体。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 * <p><b>生命周期</b>: 状态在 {@link ClientReconData} 中管理，
 *   在 {@link com.piranport.client.ClientGameEvents#onClientDisconnect} 中清理。
 */
public class ReconInputHandler {

    private ReconInputHandler() {}

    /** 每 tick 处理侦察模式输入。返回 true 表示当前处于侦察模式。 */
    public static boolean tick(Minecraft mc) {
        if (mc.player == null) return false;
        boolean inReconMode = ClientReconData.isInReconMode();
        if (!inReconMode) return false;

        // 强制关闭弹药轮盘，避免与侦察模式的摄像机控制冲突
        if (AmmoSelectOverlay.isOpen()) {
            AmmoSelectOverlay.close();
        }

        if (mc.level != null) {
            Entity reconEntity = mc.level.getEntity(ClientReconData.getReconEntityId());
            if (reconEntity != null) {
                if (mc.getCameraEntity() != reconEntity) {
                    mc.setCameraEntity(reconEntity);
                }
                reconEntity.setXRot(mc.player.getXRot());
                reconEntity.setYRot(mc.player.getYRot());
                reconEntity.xRotO = mc.player.xRotO;
                reconEntity.yRotO = mc.player.yRotO;
            }
        }

        // V 键退出侦察模式
        while (ModKeyMappings.RECON_EXIT.consumeClick()) {
            PacketDistributor.sendToServer(new ReconExitPayload());
        }

        if (mc.player.tickCount % 2 == 0) {
            handleReconInput(mc);
        }

        return true;
    }

    /**
     * 将 WASD/空格/潜行输入发送到服务端，用于侦察机操控。
     * 移动方向相对于玩家当前视线方向。
     */
    private static void handleReconInput(Minecraft mc) {
        if (mc.player == null) return;

        if (!ClientReconData.isInReconMode()) return;

        int entityId = ClientReconData.getReconEntityId();
        if (mc.level != null) {
            Entity reconEntity = mc.level.getEntity(entityId);
            if (reconEntity == null) {
                com.piranport.PiranPort.LOGGER.warn("ClientReconInput ENTITY_MISSING | entityId={}", entityId);
                return;
            }
        }

        Options opts = mc.options;
        boolean anyKey = opts.keyUp.isDown() || opts.keyDown.isDown()
                || opts.keyLeft.isDown() || opts.keyRight.isDown()
                || opts.keyJump.isDown() || opts.keyShift.isDown();
        if (!anyKey) return;

        Vec3 look = mc.player.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        double fwdLen = fwd.length();
        if (fwdLen < 0.001) fwd = new Vec3(0, 0, 1);
        else fwd = fwd.scale(1.0 / fwdLen);
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);

        float dx = 0, dy = 0, dz = 0;
        if (opts.keyUp.isDown())      { dx += (float) fwd.x;   dz += (float) fwd.z; }
        if (opts.keyDown.isDown())    { dx -= (float) fwd.x;   dz -= (float) fwd.z; }
        if (opts.keyLeft.isDown())    { dx -= (float) right.x; dz -= (float) right.z; }
        if (opts.keyRight.isDown())   { dx += (float) right.x; dz += (float) right.z; }
        if (opts.keyJump.isDown())    dy += 1f;
        if (opts.keyShift.isDown())   dy -= 1f;

        float hLen = (float) Math.sqrt(dx * dx + dz * dz);
        if (hLen > 1f) { dx /= hLen; dz /= hLen; }

        PacketDistributor.sendToServer(new ReconControlPayload(dx, dy, dz));
    }
}
