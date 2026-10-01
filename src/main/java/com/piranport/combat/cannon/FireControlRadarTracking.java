package com.piranport.combat.cannon;

import com.piranport.config.ModEquipmentConfig;
import net.minecraft.world.phys.Vec3;

/**
 * 火控雷达炮弹追踪（依据：策划决策/火控/05-火控雷达强化装备.md §2）。
 *
 * <p>装备火控雷达 + 火控锁定目标存在时，炮弹进入目标 {@code range} 格内后每 tick
 * 按转向系数向目标偏转；范围外弹道不受影响（初始轨迹与远距离弹道不变）。
 * 只改方向不改速率，重力/阻力照常作用。纯函数，便于单测。
 */
public final class FireControlRadarTracking {
    private FireControlRadarTracking() {}

    /** 单级追踪参数。 */
    public record Params(double range, double turnCoefficient) {
        public boolean active() {
            return range > 0 && turnCoefficient > 0
                    && Double.isFinite(range) && Double.isFinite(turnCoefficient);
        }
    }

    /** 按等级（1～3）读取终端参数；等级越界钳到 1～3。 */
    public static Params forLevel(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> new Params(ModEquipmentConfig.FC_RADAR_L1_RANGE.get(), ModEquipmentConfig.FC_RADAR_L1_TURN.get());
            case 2 -> new Params(ModEquipmentConfig.FC_RADAR_L2_RANGE.get(), ModEquipmentConfig.FC_RADAR_L2_TURN.get());
            default -> new Params(ModEquipmentConfig.FC_RADAR_L3_RANGE.get(), ModEquipmentConfig.FC_RADAR_L3_TURN.get());
        };
    }

    /**
     * 计算追踪修正后的速度。
     *
     * @param pos       炮弹位置
     * @param velocity  炮弹当前速度
     * @param aimPoint  目标瞄准点（目标中心）
     * @param params    追踪参数
     * @return 修正后的速度；未进入范围、目标已在身后或参数无效时原样返回
     */
    public static Vec3 steer(Vec3 pos, Vec3 velocity, Vec3 aimPoint, Params params) {
        if (params == null || !params.active()) return velocity;
        double speed = velocity.length();
        if (speed < 1.0e-6) return velocity;
        Vec3 toTarget = aimPoint.subtract(pos);
        double dist = toTarget.length();
        if (dist > params.range() || dist < 1.0e-6) return velocity;
        Vec3 dir = velocity.scale(1.0 / speed);
        Vec3 want = toTarget.scale(1.0 / dist);
        // 目标已被越过（在弹头后半球）时不回头追，避免炮弹绕圈
        if (dir.dot(want) <= 0) return velocity;
        double k = Math.min(1.0, params.turnCoefficient());
        Vec3 blended = dir.scale(1.0 - k).add(want.scale(k));
        double len = blended.length();
        if (len < 1.0e-9) return velocity;
        return blended.scale(speed / len);
    }
}
