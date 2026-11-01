package com.piranport.client.gecko;

import net.minecraft.resources.ResourceLocation;

/**
 * 一件 GeckoLib 舰装资源的最小描述：模型 / 贴图 / 动画 / 摆放参数。
 *
 * <p>武器（{@link RiggingModelDef}）与船体部件（{@link RiggingHullModelDef}）都实现它，
 * 这样 {@link RiggingAnimatable}、{@link RiggingGeoModel}、{@link RiggingObjectRenderer}
 * 不需要区分「是不是挂在物品上」。
 */
public interface RiggingRenderDef {

    /** geo 文件完整 ResourceLocation（含 .geo.json）。 */
    ResourceLocation geoModel();

    /** 贴图完整 ResourceLocation（含 .png）。 */
    ResourceLocation texture();

    /** 动画文件完整 ResourceLocation（含 .animation.json）。 */
    ResourceLocation animation();

    /** 模型自身前向与挂点前向的差值：局部 +Z 朝前的模型填 180，-Z 朝前的填 0。 */
    float yawOffset();

    /** 模型整体缩放；按各模型包围盒调到和玩家比例相符。 */
    float scale();

    /** 待机动画名；模型没有同名动画时回退到第一个动画。 */
    String idleAnimation();

    /** 开火动画名；船体模型没有开火动作，传常量名即可，触发时 GeckoLib 会静默忽略。 */
    String fireAnimation();
}
