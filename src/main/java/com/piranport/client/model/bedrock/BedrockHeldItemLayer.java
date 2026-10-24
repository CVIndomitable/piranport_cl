package com.piranport.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 手持物层：把主手/副手物品挂到 YSM 模型的手部定位骨骼上。
 *
 * <p>变换顺序与原版 {@code ItemInHandLayer} 一致（X -90、Y 180），
 * 但去掉了原版“从肩部再偏到手掌”的固定偏移——这里直接用的是
 * {@code RightHandLocator}/{@code LeftHandLocator}，已经是手腕位置。</p>
 */
public class BedrockHeldItemLayer<T extends LivingEntity> extends RenderLayer<T, BedrockEntityModel<T>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public BedrockHeldItemLayer(RenderLayerParent<T, BedrockEntityModel<T>> parent,
                                ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        boolean mainIsRight = entity.getMainArm() == HumanoidArm.RIGHT;
        renderArm(entity,
                mainIsRight ? entity.getMainHandItem() : entity.getOffhandItem(),
                mainIsRight ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                mainIsRight ? HumanoidArm.RIGHT : HumanoidArm.LEFT,
                poseStack, buffer, packedLight);
        renderArm(entity,
                mainIsRight ? entity.getOffhandItem() : entity.getMainHandItem(),
                mainIsRight ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                mainIsRight ? HumanoidArm.LEFT : HumanoidArm.RIGHT,
                poseStack, buffer, packedLight);
    }

    private void renderArm(T entity, ItemStack stack, ItemDisplayContext displayContext, HumanoidArm arm,
                           PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        this.getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // 定位骨骼在手背/手腕处，往下挪一像素让握点落在掌心
        poseStack.translate(0.0F, 0.03125F, 0.0F);
        this.itemInHandRenderer.renderItem(entity, stack, displayContext,
                arm == HumanoidArm.LEFT, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
