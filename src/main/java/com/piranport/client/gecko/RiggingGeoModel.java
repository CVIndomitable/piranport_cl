package com.piranport.client.gecko;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * 按 {@link RiggingModelDef} 返回资源路径的 GeckoLib 模型。
 *
 * <p>资源路径必须带完整后缀：GeckoLib 4.9.3 在资源重载时按
 * {@code assets/<ns>/geo/.../*.geo.json} 与 {@code assets/<ns>/animations/.../*.animation.json}
 * 的完整 ResourceLocation 建缓存，{@code getModelResource} 返回不带后缀的路径会找不到模型。
 *
 * <p>4.9.3 把一参的 {@code getModelResource/getTextureResource} 标了 {@code @Deprecated}
 * 并改为由带 {@link GeoRenderer} 的重载转发；一参版本仍是抽象方法，必须一并实现。
 */
@SuppressWarnings("deprecation")
public class RiggingGeoModel extends GeoModel<RiggingAnimatable> {

    private final RiggingModelDef def;

    public RiggingGeoModel(RiggingModelDef def) {
        this.def = def;
    }

    @Override
    public ResourceLocation getModelResource(RiggingAnimatable animatable,
                                             GeoRenderer<RiggingAnimatable> renderer) {
        return def.geoModel();
    }

    @Override
    public ResourceLocation getModelResource(RiggingAnimatable animatable) {
        return def.geoModel();
    }

    @Override
    public ResourceLocation getTextureResource(RiggingAnimatable animatable,
                                               GeoRenderer<RiggingAnimatable> renderer) {
        return def.texture();
    }

    @Override
    public ResourceLocation getTextureResource(RiggingAnimatable animatable) {
        return def.texture();
    }

    @Override
    public ResourceLocation getAnimationResource(RiggingAnimatable animatable) {
        return def.animation();
    }
}
