package com.piranport.client.gecko;

import net.minecraft.resources.ResourceLocation;

/**
 * 舰装船体部件的 GeckoLib 模型清单。
 *
 * <p>四个部件的资源路径约定统一为：
 * <ul>
 *   <li>geo：{@code piranport:geo/rigging/hull/<name>.geo.json}</li>
 *   <li>贴图：{@code piranport:textures/rigging/hull/<texture>.png}</li>
 *   <li>动画：{@code piranport:animations/rigging/hull/<name>.animation.json}</li>
 * </ul>
 *
 * <p>航母舰体（{@code carrier_hull}）复用通用舰体的贴图，只换几何：右臂被去掉，
 * 右侧让给 {@link #CARRIER_DECK}。
 */
public final class RiggingHullModelRegistry {

    private static final String HULL_DIR = "hull";
    private static final float DEFAULT_YAW_OFFSET = 0.0f;
    private static final float DEFAULT_SCALE = 1.0f;

    /** 通用舰体：所有非航母形态共用。 */
    public static final RiggingHullModelDef GENERAL_HULL =
            hull("general_hull", "general_hull");
    /** 航母舰体：去掉右臂的变体。<b>暂不引用，待删除</b>——当前航母配置保留通用舰体双臂，
     *  飞行甲板直接叠在右舷；资产文件先保留，等甲板与右臂的最终关系确认后再清理。 */
    public static final RiggingHullModelDef CARRIER_HULL =
            hull("carrier_hull", "general_hull");
    /** 航母飞行甲板：只在快捷栏里有舰载机时叠加。 */
    public static final RiggingHullModelDef CARRIER_DECK =
            hull("carrier_deck", "carrier_deck");
    /** 腰部鱼雷架：有鱼雷发射器挂点时叠加，左右各一个挂点。 */
    public static final RiggingHullModelDef WAIST_TORPEDO_RACK =
            hull("waist_torpedo_rack", "waist_torpedo_rack");

    private RiggingHullModelRegistry() {}

    private static RiggingHullModelDef hull(String name, String texture) {
        String base = "rigging/" + HULL_DIR + "/" + name;
        String textureBase = "rigging/" + HULL_DIR + "/" + texture;
        return new RiggingHullModelDef(
                ResourceLocation.fromNamespaceAndPath("piranport", "geo/" + base + ".geo.json"),
                ResourceLocation.fromNamespaceAndPath("piranport", "textures/" + textureBase + ".png"),
                ResourceLocation.fromNamespaceAndPath("piranport",
                        "animations/" + base + ".animation.json"),
                DEFAULT_YAW_OFFSET,
                DEFAULT_SCALE,
                RiggingModelDef.DEFAULT_IDLE_ANIMATION,
                RiggingModelDef.DEFAULT_FIRE_ANIMATION);
    }
}
