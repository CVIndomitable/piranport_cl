package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.item.SkinCoreItem;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Custom renderer for SkinCoreItem that displays a 3D player head
 * with the corresponding skin texture in the inventory.
 */
public class SkinCoreItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final SkullModelBase headModel;

    public SkinCoreItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.headModel = new SkullModel(modelSet.bakeLayer(ModelLayers.PLAYER_HEAD));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof SkinCoreItem skinCore)) return;

        int skinId = skinCore.getSkinId();
        ResourceLocation skinTexture = ResourceLocation.fromNamespaceAndPath(
                PiranPort.MOD_ID, "textures/skin/skin_" + skinId + ".png");

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.25F, 0.5F);

        float scale = 0.625F;
        poseStack.scale(scale, -scale, -scale);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(skinTexture));
        this.headModel.renderToBuffer(poseStack, vc, packedLight, packedOverlay, -1);

        poseStack.popPose();
    }
}
