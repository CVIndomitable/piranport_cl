package com.piranport.client;

/**
 * 准星吸附的纯函数决策核心 —— 不依赖 Minecraft 运行时，可在单测里直接跑。
 *
 * <p><b>为什么单独拆出来</b>：吸附逻辑里最容易出错的不是「怎么转视角」，而是
 * 「这一 tick 到底吸不吸、吸谁」。把它抽成无副作用纯函数后，可以用 JUnit 把
 * 「不入阈值不吸」「墙上目标不吸」「已锁定不被抢走」「物理不可达不吸」这些
 * 分支逐个钉住 —— 这些正是原实现（FireControlRadarSnapHandler 首版）翻车的地方。
 *
 * <p>同一理由，{@link #yawTo} / {@link #pitchTo} / {@link #turnStep} 这三个「视角解算」
 * 的纯数学也被搬到这里：转向时最容易犯的错恰恰是纯数学的错（yaw 与 pitch 的轴传反、
 * 俯仰越界不钳制、限速与死区互相打架），而 {@code FireControlRadarSnapHandler#aimAt}
 * 夹着 {@code mc}/实体/网络包，没法直接单测。搬进来之后这些分支就能被钉死。
 *
 * <p><b>坐标口径</b>：{@link Candidate#direction()} 是
 * 「眼睛 → 瞄准点」的长度为 1 的方向向量，{@link Candidate#distance()} 是眼睛到瞄准点的距离。
 * 调用方负责把 {@code getPosition(1.0f)} 的插值位置、实体包围盒、遮挡判定先算好。
 *
 * <p><b>线程模型</b>：纯函数，无线程假设。
 */
final class SnapDecision {

    private SnapDecision() {}

    /**
     * 一个可吸附候选（已通过敌对判定与可达性判定的实体）。
     *
     * @param id          实体唯一标识（{@link net.minecraft.world.entity.Entity#getId()}）
     * @param direction   眼睛 → 瞄准点的单位方向向量
     * @param distance    眼睛 → 瞄准点的距离（格）
     * @param angleDeg    该方向与视线方向的夹角（度）
     * @param hasLineOfSight 眼睛与瞄准点之间是否无方块遮挡
     * @param projectileReachable 瞄准点是否可以被炮弹打到（客户端能做的最强判定：
     *        目标所在区块已加载，见 {@code FireControlRadarSnapHandler#collectCandidates}）。
     *        真正的弹道可达域只有开火那一刻服务端才知道 —— 锁定后不重算它，是为了不把
     *        「本 tick 客户端无法验证」误判成「不可达」而中途松锁。
     */
    record Candidate(long id,
                     double directionX, double directionY, double directionZ,
                     double distance,
                     double angleDeg,
                     boolean hasLineOfSight,
                     boolean projectileReachable) {}

    /** 候选名单里没有可吸对象。 */
    static final Candidate NONE = new Candidate(-1, 0, 0, 0, 0, 0, false, false);

    /** 是否处于吸附状态。 */
    static boolean isSnapping() {
        return snapLocked && lockedTarget.id() >= 0;
    }

    /** 当前锁定的目标。未吸附时为 {@link #NONE}。 */
    static Candidate lockedTarget() {
        return lockedTarget;
    }

    /**
     * 清空吸附状态。
     *
     * <p>调用时机有两类：断开连接（{@code ClientInputCoordinator#resetClientState}）、
     * 退出开镜（{@code FireControlRadarSnapHandler#tick} 的开镜门）。
     *
     * <p><b>0 键关火控这条路径不清状态</b>：{@code FireControlRadarSnapHandler#tick} 在读到
     * {@code SHIP_FC_RADAR_ON != true} 时只做 {@code return}，并不调本方法，所以关掉雷达后
     * 「上一轮锁定的实体 id」会留在静态字段里。重新按 0 开启雷达时，只要仍开着镜，
     * {@code decide()} 就会因该 id 还在而走「保持原锁定」分支 —— 而该分支刻意不看遮挡、
     * 阈值放宽到退出半径（8°），于是准星会从最远 8° 外被猛拉回旧目标，绕过 4° 的进入阈值。
     * 要修就在 {@code tick()} 读到开关不为 true 的那个分支里补一次 {@code SnapDecision.reset()}。
     */
    static void reset() {
        snapLocked = false;
        lockedTarget = NONE;
    }

    private static boolean snapLocked = false;
    private static Candidate lockedTarget = NONE;

    /**
     * 每 tick 决策一次：吸谁 / 换不换 / 松不松。
     *
     * <p>状态机只有两态，切换条件刻意做得不对称（这是「锁在上面」的手感来源）：
     * <ul>
     *   <li><b>未吸附</b>：候选里挑夹角最小、且夹角 ≤ {@code enterRadiusDeg}、且可达、
     *       且有视线的那个，吸上去。</li>
     *   <li><b>已吸附</b>：只要「原锁定目标」仍满足 {@code exitRadiusDeg} 阈值（该阈值更大，
     *       所以有回滞，不会在阈值边缘反复吸放），就继续锁它 —— <b>即使别的目标此刻夹角更小、
     *       甚至原目标已经被遮挡</b>。原目标丢失才重新挑新的候选。</li>
     * </ul>
     *
     * <p><b>为什么锁定后忽略遮挡</b>：目标舰在水面起伏/被浪花挡住时若松锁，准星会突然弹回，
     * 玩家主观感受是「吸附乱跳」。遮挡只在「选新目标」时作为过滤条件。     *
     * @param candidates 本 tick 的候选列表（未排序，允许为空）
     * @param enterRadiusDeg 未吸附时的进入阈值（度）
     * @param exitRadiusDeg 已吸附时的脱离阈值（度）
     * @param currentlyLockedId 上一 tick 锁定的实体 id，未锁定传 -1
     * @return 本 tick 选中的候选；不该吸附时返回 {@code null}
     */
    static Candidate decide(Iterable<Candidate> candidates,
                            double enterRadiusDeg,
                            double exitRadiusDeg,
                            long currentlyLockedId) {
        // 第一优先：保持原锁定。阈值用更宽的退出半径，形成回滞。
        if (currentlyLockedId >= 0) {
            Candidate held = null;
            for (Candidate c : candidates) {
                if (c.id() == currentlyLockedId) {
                    held = c;
                    break;
                }
            }
            // 条件刻意不含 hasLineOfSight：遮挡不松锁（见方法注释）。
            if (held != null && held.projectileReachable()
                    && held.angleDeg() <= exitRadiusDeg) {
                snapLocked = true;
                lockedTarget = held;
                return held;
            }
        }

        // 第二优先：从未吸附状态里挑一个新的。
        Candidate best = null;
        for (Candidate c : candidates) {
            if (!c.hasLineOfSight() || !c.projectileReachable()) continue;
            if (c.angleDeg() > enterRadiusDeg) continue;
            if (best == null || c.angleDeg() < best.angleDeg()) best = c;
        }

        snapLocked = best != null;
        lockedTarget = best != null ? best : NONE;
        return best;
    }

    // ===== 视角解算（纯数学，从 FireControlRadarSnapHandler#aimAt 搬来以便单测）=====

    /** {@code Entity#turn} 的内部固定换算：入参乘 0.15 才变成角度。 */
    static final double TURN_INPUT_TO_DEGREES = 0.15;

    /**
     * 「眼睛 → 目标」的位移解算成 Minecraft 约定的绝对偏航角（度）。
     *
     * <p>约定：yaw=0 朝 +Z，yaw=90 朝 -X（即 +X 方向是 -90°）。所以对 -x 取 atan2。
     * 轴传反不会「转得别扭」，而是横竖各转错一根轴 —— 这正是首版准星乱甩的成因之一。
     */
    static double yawTo(double toTargetX, double toTargetZ) {
        return Math.toDegrees(Math.atan2(-toTargetX, toTargetZ));
    }

    /**
     * 「眼睛 → 目标」的位移解算成绝对俯仰角（度），与 {@code Entity#xRot} 同号。
     *
     * <p>返回的是 {@code Entity} 口径的 xRot 值，不是「自然语言里的抬头角度」。
     * 按 {@code aimAt} 的取数口径（{@code toTargetY} 已含上下方向、{@code horizontalDistance} 恒为非负），
     * 实测符号是：目标在<b>上方</b>（{@code toTargetY} 为负）→ <b>正</b>角；目标在<b>下方</b> → <b>负</b>角。
     * 这条符号约定由 {@code SnapDecisionTest} 用具体数值钉死 —— 写反的表现不是「转得别扭」，
     * 而是准星朝目标的反方向（上/下）转，并稳定停在关于水平面对称的位置上。
     *
     * <p>结果钳制在 ±90°：{@code Entity#turn} 内部只对 {@code xRot} 钳制、对 {@code xRotO}
     * 不钳制，喂进越界值会让两者分道扬镳，渲染插值甩出一个巨大假旋转。
     * {@code horizontalDistance} 为 0（目标在正上/正下方）时 atan2 依然给出有限值，不会 NaN。
     */
    static double pitchTo(double toTargetY, double horizontalDistance) {
        return Math.max(-90.0, Math.min(90.0,
                Math.toDegrees(-Math.atan2(toTargetY, horizontalDistance))));
    }

    /**
     * 本 tick 实际该转的角度差（度），已套用死区与限速。
     *
     * <p>死区（{@code epsilonDeg}）与限速（{@code maxTurnDeg}）必须成对使用：
     * 只限速不死区，准星会在目标中心左右反复过冲；只死区不限速，远距离目标会瞬间拉满。
     * 返回数组是 {@code [yawStep, pitchStep]}，单位是「度」——
     * 喂给 {@code Entity#turn} 前还要除以 {@link #TURN_INPUT_TO_DEGREES}。
     *
     * <p>调用方必须先自行把两个角度差各自 wrap/clamp 到 {@code [-90, 90]}：
     * 这里只做「死区 + 限速」，不做绕圈折叠（yaw 的 ±180 环绕语义不属于纯数学能决定的范畴）。
     */
    static float[] turnStep(double deltaYawDeg, double deltaPitchDeg,
                            double epsilonDeg, double maxTurnDeg) {
        if (Math.abs(deltaYawDeg) < epsilonDeg && Math.abs(deltaPitchDeg) < epsilonDeg) {
            return new float[]{0.0f, 0.0f};
        }
        return new float[]{
                (float) Math.max(-maxTurnDeg, Math.min(maxTurnDeg, deltaYawDeg)),
                (float) Math.max(-maxTurnDeg, Math.min(maxTurnDeg, deltaPitchDeg)),
        };
    }
}
