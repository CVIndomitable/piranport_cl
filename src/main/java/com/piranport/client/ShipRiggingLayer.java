package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.client.gecko.RiggingModelRegistry;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 玩家舰装渲染层：只负责「还没有 GeckoLib 模型」的武器占位。
 *
 * <p>船体、飞行甲板、腰部鱼雷架已改由 {@link com.piranport.client.gecko.RiggingHullLayer}
 * 用 GeckoLib 模型绘制（2026-11-01），这里不再画任何程序化几何。
 * 已登记 GeckoLib 模型的武器也由 {@code RiggingAttachedLayer} 绘制，本层跳过，
 * 避免同一挂点出现双份。
 *
 * <p><b>数据来源</b>：挂点分配由服务端算好放进
 * {@link com.piranport.registry.ModAttachmentTypes#RIGGING_LOADOUT}，
 * 所有看得见该玩家的客户端都只读这份快照 —— 远程玩家的背包客户端根本拿不到。
 * 只有本地玩家在快照到达之前（进服第一帧 / 单人模式）才用
 * {@link com.piranport.rigging.RiggingLoadoutResolver} 现场兜底。
 */
public class ShipRiggingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public ShipRiggingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        if (!RiggingClientState.isRenderedAsTransformed(player)) return;

        RiggingLoadout loadout = RiggingClientState.resolveLoadout(player);
        float launchPose = AircraftLaunchPoseClientState.intensity(player.getId(), partialTick);

        for (RiggingMount mount : loadout.mounts()) {
            // 已有 GeckoLib 模型的装备由 RiggingAttachedLayer 绘制，这里不能再画一份物品模型。
            if (RiggingModelRegistry.isRegistered(mount.stack())) continue;
            renderMount(poseStack, bufferSource, packedLight, player, mount, launchPose);
        }
    }

    /**
     * 按挂点摆一个物品模型。
     *
     * <p>挂点参考系统一由 {@link RiggingSocketTransforms} 提供，和 GeckoLib 层共用，
     * 避免两套坐标漂移。
     */
    private void renderMount(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                             AbstractClientPlayer player, RiggingMount mount, float launchPose) {
        poseStack.pushPose();
        RiggingSocketTransforms.applyBase(poseStack, this.getParentModel(), mount.socket(), launchPose);

        RiggingSocketTransforms.MountPose pose =
                RiggingSocketTransforms.mountPose(mount.socket(), launchPose);
        RiggingSocketTransforms.applyMountFrame(poseStack, pose);
        poseStack.scale(pose.scale(), pose.scale(), pose.scale());

        Minecraft.getInstance().getItemRenderer().renderStatic(
                mount.stack(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, player.level(), player.getId() * 31 + mount.socket().ordinal());
        poseStack.popPose();
    }
}
