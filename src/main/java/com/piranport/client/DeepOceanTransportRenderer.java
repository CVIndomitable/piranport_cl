package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.npc.deepocean.DeepOceanTransportEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 深海运输船渲染器 — 用即时方块拼一艘更大的箱船（船体 + 甲板 + 集装箱 + 舰桥）。
 *
 * <p>WHY 不套 Alex 人形：运输船是《深海/01》§九的无武装货运单位，外观上应当是
 * 箱船而不是舰娘；同时现有深海舰渲染约定「后续补皮肤只改 AlexSlimMobRenderer」，
 * 运输船不参与那一套，所以单独一个渲染器最不容易被误改。</p>
 *
 * <p>画法参照 {@code com.piranport.dungeon.client.LootShipRenderer}：
 * 逐层叠加 cuboid，颜色用深蓝灰船体 + 木色甲板，集装箱用锈红/褪蓝/军绿区分。</p>
 */
public class DeepOceanTransportRenderer extends EntityRenderer<DeepOceanTransportEntity> {

    public DeepOceanTransportRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0f;
    }

    @Override
    public void render(DeepOceanTransportEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 与 LootShipRenderer 相同：实体正面为 -Z，渲染时整体转半圈对齐航向。
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

        VertexConsumer consumer = SimpleEntityRenderShapes.translucentConsumer(buffer);
        Matrix4f pose = poseStack.last().pose();

        // 船体（深蓝灰）：吃水部分 -> 舷侧 -> 木色甲板
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -2.40f, -0.45f, -0.85f,
                2.40f, 0.10f, 0.85f, 0.20f, 0.24f, 0.30f, 1.0f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -2.20f, 0.10f, -0.75f,
                2.20f, 0.45f, 0.75f, 0.24f, 0.29f, 0.36f, 1.0f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -2.15f, 0.45f, -0.72f,
                2.15f, 0.58f, 0.72f, 0.55f, 0.40f, 0.24f, 1.0f, packedLight);
        // 船艏收窄，避免整艘船看起来像一块方砖
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 2.15f, 0.02f, -0.45f,
                2.75f, 0.40f, 0.45f, 0.22f, 0.27f, 0.34f, 1.0f, packedLight);

        // 甲板集装箱：锈红 / 褪蓝 / 军绿
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -1.90f, 0.58f, -0.60f,
                -0.95f, 1.42f, -0.05f, 0.62f, 0.24f, 0.20f, 1.0f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -1.90f, 0.58f, 0.05f,
                -0.95f, 1.40f, 0.60f, 0.22f, 0.38f, 0.52f, 1.0f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, -0.75f, 0.58f, -0.50f,
                0.10f, 1.32f, 0.50f, 0.30f, 0.42f, 0.34f, 1.0f, packedLight);

        // 舰桥 / 上层建筑（船尾侧）
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 0.55f, 0.58f, -0.55f,
                1.55f, 1.60f, 0.55f, 0.26f, 0.31f, 0.39f, 1.0f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 0.70f, 1.60f, -0.42f,
                1.35f, 1.85f, 0.42f, 0.32f, 0.38f, 0.46f, 1.0f, packedLight);
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 1.05f, 1.85f, -0.12f,
                1.30f, 2.10f, 0.12f, 0.15f, 0.16f, 0.18f, 1.0f, packedLight);
        // 桅杆
        SimpleEntityRenderShapes.renderCuboid(consumer, pose, 1.65f, 0.58f, -0.04f,
                1.72f, 2.20f, 0.04f, 0.76f, 0.74f, 0.70f, 1.0f, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepOceanTransportEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png");
    }
}
