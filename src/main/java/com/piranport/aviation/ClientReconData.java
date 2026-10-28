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

    /** 断线/换维度/重生重置：不发送游戏消息，但必须把相机交还给本地玩家。 */
    public static void resetClientState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() != null) {
            mc.setCameraEntity(mc.player);
        }
        // 换维度/重生发生在同一个客户端会话里时，区块缓存会复用，必须把临时放大的半径收回去。
        restoreChunkCacheRadius();
        clearRecon();
    }

    public static boolean isInReconMode() { return inReconMode; }
    public static int getReconEntityId() { return reconEntityId; }

    /**
     * 侦察期间放宽客户端区块缓存的接收半径。
     *
     * <p>WHY：ClientChunkCache.Storage 只接收「与视图中心（玩家所在区块）水平距离 ≤
     * 视距 + 3」的区块，超出的手工区块包会被直接丢弃并打 warn。侦察机比玩家飞得远，
     * 服务器强加载并推来的远处区块必须能被接收，因此侦察期间把缓存半径临时放大到
     * 覆盖「玩家 → 飞机最大航程 + 飞机周围强加载环」；退出时恢复原值。
     */
    private static final int RECON_CHUNK_CACHE_RADIUS = 64;

    /** 每 tick 兜底放大一次：服务器下发 SetChunkCacheRadius 会把半径改回原值，重放一次即可恢复。 */
    public static void keepChunkCacheExpanded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.getChunkSource().updateViewRadius(RECON_CHUNK_CACHE_RADIUS);
    }

    private static void restoreChunkCacheRadius() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // 原版缓存半径 = 服务器下发的区块半径；getEffectiveRenderDistance 会取它与
        // 客户端渲染距离的较小值，足够覆盖原版的区块追踪范围。
        mc.level.getChunkSource().updateViewRadius(mc.options.getEffectiveRenderDistance());
    }

    public static void handleReconStart(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        // 即使实体这一刻还没同步到客户端也要进入侦察模式：实体稍后出现时
        // ReconInputHandler 会补挂摄像机；否则会出现服务端已在侦察、客户端不认的死锁。
        setReconActive(entityId);
        if (mc.level != null) {
            keepChunkCacheExpanded();
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null) {
                mc.setCameraEntity(entity);
            }
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
        restoreChunkCacheRadius();
        clearRecon();
    }
}
