package com.piranport.combat;

/**
 * 水平轰炸机投弹提前量解算 — 纯函数，无 Minecraft 依赖。
 *
 * <p><b>WHY（为什么要提前投）</b>：航弹出膛时保留前抛——它继承载机水平速度的一部分
 * （乘数见调试终端参数 {@code global.level_bomber.horizontal_velocity_multiplier}）。
 * 因此航弹从投弹高度落到目标高度的这段时间里，会沿航线自己往前飞出一段距离。
 * 若仍像旧实现那样飞到目标近旁（&lt;3 格）才投，弹着点必然被这段前抛量甩到目标前方
 * （实测偏前 7~9 格）。正确做法是反推：当载机到目标的水平距离刚好等于「航弹落地期间
 * 自身的水平前进量」时就投弹，让落点回到目标上。
 *
 * <p><b>为什么要与实体物理逐行一致</b>：本解算必须复刻
 * {@code ThrowableProjectile#tick}（{@code AerialBombEntity} 实际跑的物理）的执行顺序，
 * 否则提前量会系统性偏差。该顺序为每 tick：
 * <ol>
 *   <li>先用「阻尼前」的速度位移：{@code pos += v}；</li>
 *   <li>再阻尼：{@code v *= 0.99}（空气中）；</li>
 *   <li>最后加重力：{@code v.y -= gravity}。</li>
 * </ol>
 * 客户端黄色落点标记（{@code ProjectileLandingMarkerRenderer}）用的是同一顺序，
 * 所以本函数与标记、实体的弹道三者同源，标记指向哪、弹就落哪。
 */
public final class LevelBombLead {

    private LevelBombLead() {}

    /** 空气阻尼，须与 {@code ThrowableProjectile#tick} 的 0.99 一致。 */
    private static final double AIR_DRAG = 0.99;

    /** 迭代上限，防止异常参数（如重力为 0）导致死循环；正常投弹约 30 tick 落地。 */
    private static final int MAX_FALL_TICKS = 600;

    /**
     * 求投弹提前距离：航弹从投弹高度落到目标高度期间，自身沿水平方向前进的距离。
     *
     * <p>本函数按 {@code ThrowableProjectile#tick} 的逐行顺序数值积分：每 tick 先累加
     * 位移（用阻尼前的速度），再阻尼、再加重力，直到累计下落量达到 {@code dropHeight}。
     * 返回值为该瞬间累计的水平前进量。
     *
     * @param dropHeight      投弹点与目标的高度差（格，正数；= 投弹时 craft.getY() - 0.5 - target.getY()）
     * @param gravity         航弹每 tick 的垂直加速度（格/tick²，读实体的 {@code getDefaultGravity()}）
     * @param horizontalSpeed 航弹的初始水平速度（格/tick，= 载机水平速度 × 终端倍数）
     * @param verticalSpeed   航弹的初始垂直速度（格/tick，负数表示下坠）
     * @return 提前距离（格）；参数退化（下落高度非正、重力非正、非有限值）时返回 0
     */
    public static double releaseDistance(double dropHeight, double gravity,
                                         double horizontalSpeed, double verticalSpeed) {
        if (!(dropHeight > 0.0) || !(gravity > 0.0)
                || !Double.isFinite(dropHeight) || !Double.isFinite(gravity)
                || !Double.isFinite(horizontalSpeed) || !Double.isFinite(verticalSpeed)) {
            return 0.0;
        }

        double vx = horizontalSpeed;
        double vy = verticalSpeed;
        double fallen = 0.0;
        double forward = 0.0;

        for (int tick = 0; tick < MAX_FALL_TICKS; tick++) {
            // 顺序 1：先位移，且用「阻尼前」的速度 —— 与实体 tick 里的 pos += v 对齐。
            fallen += -vy;   // vy 为负，-vy 即本 tick 的下落量
            forward += vx;
            // 顺序 2：阻尼。
            vx *= AIR_DRAG;
            // 顺序 3：重力。
            vy = vy * AIR_DRAG - gravity;
            if (fallen >= dropHeight) {
                return forward;
            }
        }
        return forward;
    }
}
