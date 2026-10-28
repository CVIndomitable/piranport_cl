package com.piranport.combat;

import net.minecraft.world.phys.Vec3;

/**
 * 非空投鱼雷（玩家手持/舰船、NPC、随从发射）空中段的回退弹道（纯函数）。
 *
 * <p><b>用户口径（2026-10-28）</b>：手持鱼雷发射器打出的鱼雷要「平着飞出去再落水」。
 * 旧实现每 tick 把水平速度乘 0.70、垂直再加 0.25 格/tick²，三四 tick 就把出膛水平速度
 * 吃光（全程水平位移不到 1 格），实测入水角 82°~90°，看起来就是垂直扎进水里。
 *
 * <p>现在的空中段 = 保留出膛水平速度 + 接近原版投掷物的重力（{@code getDefaultGravity()} 同为 0.03），
 * 并给下坠速度封顶：无论从多高的地方打出去，鱼雷都以不超过「垂直:水平 = 上限:航速」的角度入水
 * （默认 0.3 : 0.3，即 45°，与航空鱼雷的固定入水角同一档）。
 *
 * <p>航空鱼雷的空投段走固定入水角，见 {@link TorpedoAirDropBallistics}，不经过这里；
 * 水下贴水面巡航（{@code TorpedoEntity#tickSurfaceAI} 的解算分支）也不经过这里。
 */
public final class TorpedoAirFallBallistics {

    private TorpedoAirFallBallistics() {}

    /**
     * 下一 tick 的垂直速度：匀加速下落，但不快于 {@code terminalFallSpeed}。
     *
     * <p>线导路径只换水平分量（水平始终是玩家朝向 × 航速），垂直共用本方法，
     * 因此线导鱼雷在空中段的手感与无人鱼雷一致。
     */
    public static double nextVerticalSpeed(double vy, double verticalAccel, double terminalFallSpeed) {
        // Math.max 取更「不那么往下」的那个：初始向上的速度不受影响，只有下坠被限速。
        return Math.max(vy - verticalAccel, -Math.abs(terminalFallSpeed));
    }

    /**
     * 无人巡航路径的下一 tick 速度：水平按当前方向乘保留比例（默认 1.0 = 不衰减），
     * 垂直按 {@link #nextVerticalSpeed}。
     */
    public static Vec3 nextMotion(Vec3 motion, double horizontalDecay,
                                  double verticalAccel, double terminalFallSpeed) {
        double decay = Math.max(0.0, Math.min(1.0, horizontalDecay));
        return new Vec3(
                motion.x * decay,
                nextVerticalSpeed(motion.y, verticalAccel, terminalFallSpeed),
                motion.z * decay);
    }
}
