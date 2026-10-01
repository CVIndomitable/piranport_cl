package com.piranport.component;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 装备稀有度四档（策划决策/数值/06、07、08）。
 *
 * <p>火炮：档位只改装填，{@link #reloadMultiplier()} 乘在基础装填上，不改面板与负重。
 * 鱼雷发射器：档位只做分类展示，负重由口径与联装公式决定。
 */
public enum EquipmentTier {
    INITIAL("initial", 1.0, ChatFormatting.WHITE),
    STANDARD("standard", 0.8, ChatFormatting.GREEN),
    IMPROVED("improved", 0.65, ChatFormatting.AQUA),
    ADVANCED("advanced", 0.5, ChatFormatting.LIGHT_PURPLE);

    private final String id;
    private final double reloadMultiplier;
    private final ChatFormatting color;

    EquipmentTier(String id, double reloadMultiplier, ChatFormatting color) {
        this.id = id;
        this.reloadMultiplier = reloadMultiplier;
        this.color = color;
    }

    public String id() { return id; }
    public double reloadMultiplier() { return reloadMultiplier; }

    /** tooltip 行：「稀有度：标准型」。 */
    public Component tooltip() {
        return Component.translatable("tooltip.piranport.equipment_tier",
                Component.translatable("tooltip.piranport.equipment_tier." + id)).withStyle(color);
    }

    public static EquipmentTier byId(String id) {
        for (EquipmentTier t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        throw new IllegalArgumentException("Unknown equipment tier: " + id);
    }
}
