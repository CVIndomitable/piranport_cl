package com.piranport.aviation;

import java.util.Locale;
import java.util.Set;

/**
 * 服务端与客户端共享的视觉 ID 白名单。
 *
 * <p>数据加载阶段用它做硬校验：未知 {@code visual_id} 直接让定义加载失败，
 * 不再静默回退到 F4F。客户端 {@code AircraftVisualRegistry} 只负责把白名单里的
 * ID 映射到具体模型/贴图；新增模型时必须同时在这里和客户端注册表登记。
 */
public final class AircraftVisualIds {
    public static final String B25 = "b25";
    public static final String F4F = "f4f";
    /** 梭鱼（Fairey Barracuda）鱼雷机专属模型。 */
    public static final String BARRACUDA = "barracuda";
    /** 流星（横须贺 D4Y）鱼雷机专属模型。 */
    public static final String RYUSEI = "ryusei";
    /** 天河（双发陆攻）专属模型。 */
    public static final String TENGA = "tenga";
    /** ju87c 专属模型。 */
    public static final String JU87C = "ju87c";
    /** btd_destroyer 专属模型。 */
    public static final String BTD_DESTROYER = "btd_destroyer";
    /** wyvern 专属模型。 */
    public static final String WYVERN = "wyvern";
    /** skyraider 专属模型。 */
    public static final String SKYRAIDER = "skyraider";
    /** f4u7 专属模型。 */
    public static final String F4U7 = "f4u7";
    /** type96_fighter 专属模型。 */
    public static final String TYPE96_FIGHTER = "type96_fighter";
    /** f2a_buffalo 专属模型。 */
    public static final String F2A_BUFFALO = "f2a_buffalo";
    public static final String FIGHTER = "fighter";
    public static final String ROCKET_FIGHTER = "rocket_fighter";
    public static final String DIVE_BOMBER = "dive_bomber";
    public static final String LEVEL_BOMBER = "level_bomber";
    public static final String TORPEDO_BOMBER = "torpedo_bomber";
    public static final String ASW = "asw";
    public static final String RECON = "recon";
    /** 美术占位：模型借用 F4F，贴图借用 B-25，正式美术到位后替换。 */
    public static final String PLACEHOLDER = "placeholder";

    private static final Set<String> KNOWN = Set.of(
            B25, F4F, F2A_BUFFALO, TYPE96_FIGHTER, F4U7, SKYRAIDER, WYVERN, BTD_DESTROYER, JU87C, BARRACUDA, RYUSEI, TENGA, FIGHTER, ROCKET_FIGHTER, DIVE_BOMBER,
            LEVEL_BOMBER, TORPEDO_BOMBER, ASW, RECON, PLACEHOLDER);

    private AircraftVisualIds() {}

    /** 规范化：去空白、小写、去掉本模组命名空间前缀。 */
    public static String normalize(String visualId) {
        if (visualId == null || visualId.isBlank()) return "";
        String value = visualId.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("piranport:")) value = value.substring("piranport:".length());
        return value;
    }

    public static boolean isKnown(String visualId) {
        return KNOWN.contains(normalize(visualId));
    }

    public static Set<String> known() {
        return KNOWN;
    }
}
