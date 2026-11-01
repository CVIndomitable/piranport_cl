package com.piranport.client.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * 舰装 GeckoLib 模型的对象渲染器。
 *
 * <p>去掉父类 {@code GeoObjectRenderer#preRender} 的 {@code translate(0.5, 0.51, 0.5)}：
 * 那是给「以方块中心为原点」的独立对象用的；舰装挂点原点已经是模型基座底面中心，
 * 照搬会让每个模型都偏移半格。其余（保存矩阵、应用 scaleWidth/Height）与父类一致。
 *
 * <p>武器层与船体层共用同一实现，避免两份 preRender 副本漂移。
 */
public final class RiggingObjectRenderer extends GeoObjectRenderer<RiggingAnimatable> {

    public RiggingObjectRenderer(RiggingRenderDef def) {
        super(new RiggingGeoModel(def));
    }

    @Override
    public void preRender(PoseStack poseStack, RiggingAnimatable animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        scaleModelForRender(this.scaleWidth, this.scaleHeight, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
    }
}
