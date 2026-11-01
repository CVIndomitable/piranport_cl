package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.client.gecko.RiggingModelRegistry;
import com.piranport.client.gecko.RiggingRenderContext;
import com.piranport.rigging.RiggingCategory;
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
     * 按挂点摆一个 3D 物品模型（BEWLR 武器炮塔 / 鱼雷发射器）。
     *
     * <p>挂点参考系统一由 {@link RiggingSocketTransforms} 提供，和 GeckoLib 层共用，
     * 避免两套坐标漂移。
     *
     * <p>用 {@link ItemDisplayContext#FIXED}：它的 display 只有 180° Y 旋转、scale 1、
     * 无平移，最适合「模型底部原点直接落在挂点」的场景。BEWLR 只看上下文无法区分
     * 「展示框的 FIXED」（要图标）与「舰装挂点的 FIXED」（要 3D），所以这里额外用
     * {@link RiggingRenderContext} 标记「本次是舰装渲染」，BEWLR 只在
     * {@code FIXED && isRiggingPass()} 时走 3D 分支。
     */
    private void renderMount(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                             AbstractClientPlayer player, RiggingMount mount, float launchPose) {
        // 甲板飞机复用飞行实体的 3D 模型；定义缺失时返回 false，回退到下面的物品图标。
        if (renderDeckAircraft(poseStack, bufferSource, packedLight, mount, launchPose)) return;

        poseStack.pushPose();
        RiggingSocketTransforms.applyBase(poseStack, this.getParentModel(), mount.socket(), launchPose);

        RiggingSocketTransforms.MountPose pose =
                RiggingSocketTransforms.mountPose(mount.socket(), launchPose);
        RiggingSocketTransforms.applyMountFrame(poseStack, pose);

        // 物品模型坐标系 +Y 朝上、炮口朝 +Z；玩家模型坐标系 +Y 朝下、+Z 朝前。
        // 绕 X 翻 180° 把模型的「上」翻成玩家模型的「下」（底座落在平台上），
        // 同时 +Z 仍朝前（炮口朝前）。FIXED 自带的 180° Y 与本翻转组合等价于
        // 绕 Z 转 180°：模型立起来且不左右调头。
        // 必须在挂点 yaw/pitch/roll 之后、缩放之前，与 GeckoLib 层的 Axis.XP(180) 语义一致。
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        poseStack.scale(pose.scale(), pose.scale(), pose.scale());

        // BEWLR 无法区分「展示框的 FIXED」和「舰装挂点的 FIXED」，用标记告诉它这次要 3D。
        // try/finally 防止异常时把标记留在 true，导致后续展示框/手持变成 3D。
        RiggingRenderContext.setRiggingPass(true);
        try {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    mount.stack(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, player.level(),
                    player.getId() * 31 + mount.socket().ordinal());
        } finally {
            RiggingRenderContext.setRiggingPass(false);
        }
        poseStack.popPose();
    }

    /**
     * 甲板飞机：复用飞行实体的 3D 模型，平放在甲板顶面、机头朝船首。
     *
     * <p>不能套 {@link RiggingSocketTransforms#mountPose} 的 DECK pitch=-90：那是给平面
     * 物品图标「躺平」用的。这里只借挂点的 x/z，进入与 {@code RiggingHullLayer}
     * 相同的 +Y 朝上模型坐标系后，按甲板顶面高度落点。
     *
     * @return true 表示已按 3D 模型画出；false 表示不是飞机或没有定义，调用方走物品图标。
     */
    private boolean renderDeckAircraft(PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, RiggingMount mount, float launchPose) {
        if (mount.category() != RiggingCategory.AIRCRAFT) return false;

        poseStack.pushPose();
        RiggingSocketTransforms.applyBase(poseStack, this.getParentModel(), mount.socket(), launchPose);
        // 与 RiggingHullLayer 一致：玩家模型 +Y 朝下 → 舰装模型 +Y 朝上（甲板/飞机的坐标系）。
        poseStack.scale(1.0f, -1.0f, 1.0f);
        AircraftRiggingModelRenderer.translateToDeck(poseStack, mount.socket());
        boolean rendered = AircraftRiggingModelRenderer.render(
                mount.stack(), poseStack, bufferSource, packedLight,
                AircraftRiggingModelRenderer.DECK_SCALE);
        poseStack.popPose();
        return rendered;
    }
}
