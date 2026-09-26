package com.piranport.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    private static final String CATEGORY = "key.categories.piranport";

    /**
     * 火控选择目标（鼠标中键）。
     *
     * <p>三种语义由「是否蹲下 × 准心是否有实体」派生：
     * 不蹲下=加选（追加到火控列表，上限 4）；蹲下=单选（替换整个列表）；
     * 蹲下且准心无实体=清空列表。
     *
     * <p>必须用带 {@link InputConstants.Type} 的构造重载：用 (String,int,String)
     * 重载会把鼠标键码当成键盘 keysym，永远匹配不上鼠标。
     */
    public static final KeyMapping FIRE_CONTROL_SELECT =
            new KeyMapping("key.piranport.fire_control_select",
                    InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, CATEGORY);

    public static final KeyMapping TOGGLE_FIGHTER_GROUND_ATTACK =
            new KeyMapping("key.piranport.toggle_fighter_ground_attack", GLFW.GLFW_KEY_U, CATEGORY);

    public static final KeyMapping HIGHLIGHT_ENTITIES =
            new KeyMapping("key.piranport.highlight_entities", GLFW.GLFW_KEY_Y, CATEGORY);

    public static final KeyMapping TOGGLE_AUTO_LAUNCH =
            new KeyMapping("key.piranport.toggle_auto_mode", GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping MANUAL_RELOAD =
            new KeyMapping("key.piranport.manual_reload", GLFW.GLFW_KEY_R, CATEGORY);

    /** Phase 4: 切换弹种（Tab） */
    public static final KeyMapping SWITCH_AMMO =
            new KeyMapping("key.piranport.switch_ammo", GLFW.GLFW_KEY_TAB, CATEGORY);

    /** 退出侦察模式（V） */
    public static final KeyMapping RECON_EXIT =
            new KeyMapping("key.piranport.recon_exit", GLFW.GLFW_KEY_V, CATEGORY);

    /**
     * 火控雷达开关（0）。
     *
     * <p>WHY 用主键盘数字 0 而不是小键盘：策划口径就是「按 0 键」，主键盘 0 与
     * 快捷栏 1–9 相邻，玩家按完 9 顺手按 0 不会跳到小键盘。原版并未占用主键盘 0。
     */
    public static final KeyMapping TOGGLE_FC_RADAR =
            new KeyMapping("key.piranport.toggle_fire_control_radar", GLFW.GLFW_KEY_0, CATEGORY);
}
