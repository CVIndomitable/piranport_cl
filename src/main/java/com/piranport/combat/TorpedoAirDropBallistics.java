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

    /**
     * 空投段最长持续 tick 数（5 秒）。兜底用：鱼雷如果投到陆地/建筑或不含水柱的区域，
     * 不能永远停在空投段（空投段会跳过 {@code tickSurfaceAI}，且 {@link #entryVelocity}
     * 不含重力，速度恒定）。0.14 格/tick × 100 ≈ 14 格的垂直下落，足够等到正常水面，
     * 超过就交还给空中回退坠落。
     */
    public static final int MAX_AIR_DROP_TICKS = 100;

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

    /**
     * 空投段是否该结束。
     *
     * <p>正常情况只有“脚部所在方块真正是水”（{@code inWater}）才结束——这样最后一步扎进水里的
     * 速度方向仍是配置角度（默认 45°）。旧实现用“脚下方块是水就提前结束”，会提前约 1 格切到
     * {@code tickSurfaceAI} 的空中回退（当时是 0.70 衰减 + 0.25 加速度），视觉上接近垂直入水。
     *
     * <p>兜底：脚部方块是不可穿过的固体（投到陆地/建筑）或超过 {@link #MAX_AIR_DROP_TICKS}
     * 仍未入水（虚空、无水柱等）时也必须结束，否则空投段会永久接管运动。
     *
     * @param inWater           脚部所在方块的水流体状态是水
     * @param feetBlocked       脚部所在方块是不可穿过的固体（isSolid）
     * @param airDropTicks      本次空投已经过的 tick 数（从 1 开始）
     */
    public static boolean shouldEndAirDrop(boolean inWater, boolean feetBlocked, int airDropTicks) {
        return inWater || feetBlocked || airDropTicks > MAX_AIR_DROP_TICKS;
    }
}
