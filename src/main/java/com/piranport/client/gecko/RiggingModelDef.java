package com.piranport.client.gecko;

import net.minecraft.resources.ResourceLocation;

/**
 * 一件舰装武器对应的 GeckoLib 模型定义。
 *
 * <p>只描述资源与摆放参数，不持有任何动画状态；动画状态在
 * {@link RiggingAnimatable} 里（每个玩家一份）。
 *
 * <p><b>路径约定</b>（GeckoLib 4.9.3 的缓存键带完整后缀）：
 * <ul>
 *   <li>geo：{@code piranport:geo/rigging/missile/xxx.geo.json}</li>
 *   <li>贴图：{@code piranport:textures/rigging/missile/xxx.png}</li>
 *   <li>动画：{@code piranport:animations/rigging/missile/xxx.animation.json}</li>
 * </ul>
 *
 * @param itemId      物品注册 ID，用来把挂点上的 ItemStack 映射到本模型
 * @param geoModel    geo 文件完整 ResourceLocation（含 .geo.json）
 * @param texture     贴图完整 ResourceLocation（含 .png）
 * @param animation   动画文件完整 ResourceLocation（含 .animation.json）
 * @param yawOffset   模型自身前向与挂点前向的差值：局部 +Z 朝前的模型填 180，-Z 朝前的填 0
 * @param scale       模型整体缩放；按各模型包围盒调到和玩家比例相符
 * @param idleAnimation 待机动画名；模型没有同名动画时 {@link RiggingAnimatable} 会回退到第一个动画
 * @param fireAnimation 开火动画名；触发源（开火事件）下一阶段再接
 */
public record RiggingModelDef(
        ResourceLocation itemId,
        ResourceLocation geoModel,
        ResourceLocation texture,
        ResourceLocation animation,
        float yawOffset,
        float scale,
        String idleAnimation,
        String fireAnimation) implements RiggingRenderDef {

    /** 四个导弹模型的动画名一致；保留常量方便注册表与默认值引用。 */
    public static final String DEFAULT_IDLE_ANIMATION = "idle";
    public static final String DEFAULT_FIRE_ANIMATION = "fire";
}
