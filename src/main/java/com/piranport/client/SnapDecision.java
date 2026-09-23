package com.piranport.client;

/**
 * 准星吸附的纯函数决策核心 —— 不依赖 Minecraft 运行时，可在单测里直接跑。
 *
 * <p><b>为什么单独拆出来</b>：吸附逻辑里最容易出错的不是「怎么转视角」，而是
 * 「这一 tick 到底吸不吸、吸谁」。把它抽成无副作用纯函数后，可以用 JUnit 把
 * 「不入阈值不吸」「墙上目标不吸」「已锁定不被抢走」「物理不可达不吸」这些
 * 分支逐个钉住 —— 这些正是原实现（FireControlRadarSnapHandler 首版）翻车的地方。
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

    /** 断开连接/关闭火控时清空吸附状态。 */
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
}
