package com.piranport.client.gecko;

import net.minecraft.resources.ResourceLocation;

/**
 * 船体部件的 GeckoLib 模型定义。
 *
 * <p>与 {@link RiggingModelDef} 的区别只有一点：船体不是物品，没有 itemId，
 * 因此不参与「ItemStack → 模型」查表，只由 {@link RiggingHullModelRegistry} 引用。
 *
 * @param geoModel    geo 文件完整 ResourceLocation（含 .geo.json）
 * @param texture     贴图完整 ResourceLocation（含 .png）
 * @param animation   动画文件完整 ResourceLocation（含 .animation.json）
 * @param yawOffset   模型自身前向与玩家模型前向的差值；船体模型 -Z 朝前，填 0
 * @param scale       模型整体缩放；船体模型按原画 1:1 建模，填 1.0
 * @param idleAnimation 待机动画名
 * @param fireAnimation 船体没有开火动画，保留统一接口
 */
public record RiggingHullModelDef(
        ResourceLocation geoModel,
        ResourceLocation texture,
        ResourceLocation animation,
        float yawOffset,
        float scale,
        String idleAnimation,
        String fireAnimation) implements RiggingRenderDef {
}
