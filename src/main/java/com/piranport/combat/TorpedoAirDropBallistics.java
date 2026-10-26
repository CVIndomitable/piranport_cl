package com.piranport.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 航空鱼雷空投段的弹道（纯函数）。
 *
 * <p>用户口径（2026-10-26）：鱼雷出生与入水都要保持固定 45° 角，不能越落越陡。
 * 因此空投段速度方向直接按入水角反解（{@code vy = -水平速度 × tan θ}），
 * 下落过程中不衰减水平速度、也不累加垂直加速度。抽成纯函数既方便实体与投放策略共用，
 * 也便于像 {@link LevelBombLead} 一样做不启动注册表的单测。
 */
public final class TorpedoAirDropBallistics {
    /** 入水角上限（度）：90° 时 tan 发散，速度会变成无穷大。 */
    public static final double MAX_ENTRY_ANGLE = 89.0;

    private TorpedoAirDropBallistics() {}

    /**
     * 空投下落阶段的速度矢量。
     *
     * @param horizontalDirection 归一化水平方向（只有 x/z 参与计算）
     * @param horizontalSpeed     鱼雷水平航速（格/tick），也是入水后的巡航速度
     * @param entryAngleDegrees   入水角（度）：0 = 水平，90 = 垂直，默认 45
     */
    public static Vec3 entryVelocity(Vec3 horizontalDirection, double horizontalSpeed,
                                     double entryAngleDegrees) {
        double radians = Math.toRadians(Mth.clamp(entryAngleDegrees, 0.0, MAX_ENTRY_ANGLE));
        return new Vec3(horizontalDirection.x * horizontalSpeed,
                -horizontalSpeed * Math.tan(radians),
                horizontalDirection.z * horizontalSpeed);
    }

    /** 入水后的巡航速度：纯水平贴水面，不保留空投段的俯角。 */
    public static Vec3 cruiseVelocity(Vec3 horizontalDirection, double horizontalSpeed) {
        return new Vec3(horizontalDirection.x * horizontalSpeed, 0,
                horizontalDirection.z * horizontalSpeed);
    }
}
