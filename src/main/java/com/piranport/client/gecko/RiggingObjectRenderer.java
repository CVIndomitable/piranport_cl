package com.piranport.client.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import java.util.Set;

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

    /** 默认不隐藏任何骨骼；第一人称层会按需设置。 */
    private Set<String> hiddenBoneCubes = Set.of();

    public RiggingObjectRenderer(RiggingRenderDef def) {
        super(new RiggingGeoModel(def));
    }

    /**
     * 隐藏指定骨骼<b>自己的方块</b>，但保留它的子骨骼。
     *
     * <p>WHY 不直接用 {@link GeoBone#setHidden(boolean)}：烘焙模型是全局共享的，
     * 改 hidden 标志会影响第三人称和其他玩家。这里只在绘制时跳过，不动共享状态。
     * 只跳过方块而不跳过子骨骼，是为了在第一人称下「去掉躯干/烟囱但保留双臂」。
     */
    public void setHiddenBoneCubes(Set<String> boneNames) {
        this.hiddenBoneCubes = boneNames;
    }

    @Override
    public void preRender(PoseStack poseStack, RiggingAnimatable animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        scaleModelForRender(this.scaleWidth, this.scaleHeight, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
    }

    /**
     * 与 {@code GeoRenderer} 的默认实现一致，额外支持按骨骼名跳过方块。
     * 子骨骼仍由父类默认的 {@code renderChildBones} 递归绘制。
     */
    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay, int colour) {
        if (!hiddenBoneCubes.isEmpty() && hiddenBoneCubes.contains(bone.getName())) return;
        if (bone.isHidden()) return;
        for (GeoCube cube : bone.getCubes()) {
            poseStack.pushPose();
            renderCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
            poseStack.popPose();
        }
    }
}
