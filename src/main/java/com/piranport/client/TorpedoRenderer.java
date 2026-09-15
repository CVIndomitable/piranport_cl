package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.client.model.TorpedoModel;
import com.piranport.entity.TorpedoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 航行中鱼雷的 3D 渲染器。
 *
 * 鱼雷的朝向不来自实体自身的 yRot/xRot（那组字段只在客户端线导时被玩家视角改写，
 * 平射鱼雷一直是 0），而是取当 tick 的速度矢量——速度就是雷轴指向，
 * 空投下落、水面巡航、线导转向都能自然贴合。
 */
public class TorpedoRenderer extends EntityRenderer<TorpedoEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/torpedo.png");

    /** 速度小于此值时认为雷轴无法由速度确定（例如刚出管或被卡住） */
    private static final double MIN_SPEED_SQR = 1.0E-6;

    private final TorpedoModel model;

    public TorpedoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15f;
        this.model = new TorpedoModel(context.bakeLayer(TorpedoModel.LAYER_LOCATION));
    }

    @Override
    public void render(TorpedoEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 线导时摄像机被绑在鱼雷自身上，第一人称下模型会糊满视野；
        // 第三人称（F5）仍照常渲染，方便看雷体姿态。
        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        float yaw;
        float xRot;
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > MIN_SPEED_SQR) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            // 模型雷头朝 -Z：yaw 用 MC 标准公式（yaw=0 朝 +Z），渲染时再转 180 度把雷头
            // 对到实体正前方；xRot 也按 MC 惯例（正值 = 低头）。
            yaw = (float) (Mth.atan2(-motion.x, motion.z) * (180.0 / Math.PI));
            xRot = (float) (Mth.atan2(-motion.y, horizontal) * (180.0 / Math.PI));
        } else {
            yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        // 原版实体模型靠 scale(-1,-1,1) 把 Y 轴翻过去，所以 xRot 直接当转角用；
        // 本模型没做翻转（自然 Y 轴朝上），转角要取反，与 AircraftRenderer 的 -pitch 同理
        poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));

        model.setupAnim(entity, 0.0f, 0.0f, entity.tickCount + partialTick, 0.0f, 0.0f);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TorpedoEntity entity) {
        return TEXTURE;
    }
}
