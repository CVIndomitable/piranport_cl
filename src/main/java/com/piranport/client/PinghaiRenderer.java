package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.client.model.bedrock.BedrockAnimation;
import com.piranport.client.model.bedrock.BedrockEntityModel;
import com.piranport.client.model.bedrock.BedrockGeometry;
import com.piranport.client.model.bedrock.BedrockGeometryLoader;
import com.piranport.client.model.bedrock.BedrockHeldItemLayer;
import com.piranport.client.model.bedrock.BedrockMesh;
import com.piranport.entity.PinghaiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** 平海：YSM 模型转成的 Bedrock 几何实体渲染器。 */
public class PinghaiRenderer extends MobRenderer<PinghaiEntity, BedrockEntityModel<PinghaiEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/pinghai.png");
    private static final ResourceLocation GEOMETRY =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "geo/pinghai.geo.json");
    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "animations/pinghai.animation.json");

    public PinghaiRenderer(EntityRendererProvider.Context context) {
        super(context, createModel(context), 0.5F);
        addLayer(new BedrockHeldItemLayer<>(this, context.getItemInHandRenderer()));
    }

    private static BedrockEntityModel<PinghaiEntity> createModel(EntityRendererProvider.Context context) {
        try {
            BedrockGeometry geometry = BedrockGeometryLoader.loadGeometry(context.getResourceManager(), GEOMETRY);
            Map<String, BedrockAnimation> animations =
                    BedrockGeometryLoader.loadAnimations(context.getResourceManager(), ANIMATIONS);
            return new BedrockEntityModel<>(geometry, animations, BedrockMesh.YSM_SCALE);
        } catch (Exception ex) {
            PiranPort.LOGGER.error("平海实体模型加载失败：{}", GEOMETRY, ex);
            return new BedrockEntityModel<>(BedrockGeometry.empty(), Map.of(), BedrockMesh.YSM_SCALE);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PinghaiEntity entity) {
        return TEXTURE;
    }
}
