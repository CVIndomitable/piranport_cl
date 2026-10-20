package com.piranport.npc.ai;

import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;

/**
 * 编队间距用的船体尺寸（格）。长 = 前后向占位，宽 = 左右向占位。
 * <p>
 * 碰撞盒宽度即船身宽；船长没有独立数据，按宽的 2 倍估计。
 * 这是编造的长宽比，待策划确认后替换为逐舰种登记值。
 */
public record FleetHull(double length, double beam) {

    private static final double LENGTH_PER_BEAM = 2.0;

    /** 未知舰种回退：宽 1、长 1，避免间距为 0 导致队形叠在一起 */
    public static final FleetHull DEFAULT = new FleetHull(1.0, 1.0);

    public static FleetHull of(@Nullable EntityType<?> type) {
        if (type == null) {
            return DEFAULT;
        }
        double beam = Math.max(type.getWidth(), 0.5);
        return new FleetHull(beam * LENGTH_PER_BEAM, beam);
    }
}
