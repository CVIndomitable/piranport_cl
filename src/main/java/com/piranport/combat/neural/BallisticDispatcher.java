package com.piranport.combat.neural;

import com.piranport.combat.BallisticSolver;
import com.piranport.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 弹道解算分发入口。
 *
 * <p>按**火炮注册 ID**决定走神经网络还是走 {@link BallisticSolver}。
 * 只有实验炮 {@code neural_ballistic_test_gun} 走网络，其余全部保持原路径。
 *
 * <h2>为什么必须按注册 ID 路由，不能按物理参数</h2>
 * {@code ConfigOverrideManager} / {@code UpdateConfigOverridePayload} 允许运行时改动
 * 任意火炮的 {@code dragCoeff} / {@code initialSpeed} / {@code gravity}。若按参数
 * 判断（例如「v0=3.0 且 drag=0.01 就是实验炮」），玩家一改覆盖值路由就会误判，
 * 把普通炮导到网络上，或反之。注册 ID 不受覆盖配置影响。
 *
 * <p>依据：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md} 4.2 节。
 *
 * <h2>失败回退</h2>
 * 网络权重加载失败或推理异常时，一律回退到 {@link BallisticSolver#solve}，
 * 保证任何情况下都有可用解。回退只在首次触发时打一次日志，避免刷屏。
 */
public final class BallisticDispatcher {

    private BallisticDispatcher() {}

    /** 网络单例，懒加载。加载失败时保持 null 并永久走回退路径。 */
    private static volatile BallisticNet net;

    /** 是否已经尝试过加载。避免每次调用都重复尝试失败的加载。 */
    private static volatile boolean loadAttempted = false;

    /** 回退是否已记录日志（只记一次，避免每 tick 刷屏）。 */
    private static volatile boolean fallbackLogged = false;

    /**
     * 判断给定武器是否走网络解算路径。
     *
     * <p>用 {@code is()} 比较注册项而非比较实例，以正确支持实验炮的任意 ItemStack。
     */
    public static boolean usesNeural(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return false;
        return weapon.is(ModItems.NEURAL_BALLISTIC_TEST_GUN.get());
    }

    /**
     * 解算。按武器自动分发；调用方无需关心走的是哪条路径。
     *
     * @return 与 {@link BallisticSolver#solve} 同义的解算结果
     */
    public static BallisticSolver.Result solve(ItemStack weapon,
                                               double initialSpeed, double dragCoeff, double gravity,
                                               double horizontalDist, double verticalDist, double vz0,
                                               double minAngle, double maxAngle) {
        if (!usesNeural(weapon)) {
            return BallisticSolver.solve(initialSpeed, dragCoeff, gravity,
                    horizontalDist, verticalDist, vz0, minAngle, maxAngle);
        }

        BallisticNet n = obtainNet();
        if (n == null) {
            return BallisticSolver.solve(initialSpeed, dragCoeff, gravity,
                    horizontalDist, verticalDist, vz0, minAngle, maxAngle);
        }

        try {
            return solveNeural(n, initialSpeed, dragCoeff, gravity,
                    horizontalDist, verticalDist, minAngle, maxAngle);
        } catch (RuntimeException ex) {
            // 任何推理异常都回退，保证玩家一定有可用解
            logFallbackOnce(ex);
            return BallisticSolver.solve(initialSpeed, dragCoeff, gravity,
                    horizontalDist, verticalDist, vz0, minAngle, maxAngle);
        }
    }

    /**
     * 网络解算：推理飞行时间 → 闭式反解仰角 → 按解算器同样规则钳制与判超射程。
     *
     * <p>输出语义刻意对齐 {@link BallisticSolver#solve}：
     * 角度被钳制在 {@code [minAngle, maxAngle]} 内；反解失败或落点误差超
     * {@link BallisticSolver#maxAcceptableError()} 时，回退到最大射程角并标记超射程。
     */
    private static BallisticSolver.Result solveNeural(BallisticNet n,
                                                      double initialSpeed, double dragCoeff, double gravity,
                                                      double horizontalDist, double verticalDist,
                                                      double minAngle, double maxAngle) {
        if (initialSpeed <= 0 || horizontalDist <= 0) {
            return BallisticSolver.solve(initialSpeed, dragCoeff, gravity,
                    horizontalDist, verticalDist, 0.0, minAngle, maxAngle);
        }

        // 与训练时的特征顺序一致：[hDist, vDist, initialSpeed, dragCoeff, gravity]
        double[] input = {horizontalDist, verticalDist, initialSpeed, dragCoeff, gravity};
        Double angle = n.solveAngle(input, horizontalDist, verticalDist,
                initialSpeed, dragCoeff, gravity);

        if (angle == null || !Double.isFinite(angle)) {
            return outOfRange(initialSpeed, dragCoeff, gravity, minAngle, maxAngle);
        }

        double clamped = clampAngle(angle, minAngle, maxAngle);

        // 用与解算器相同的物理复刻回算落点误差，作为可达性判据。
        // 不能只信网络——网络只学到 83.3% 的样本在 0.02 格内，
        // 超射程等边界场景必须靠实际回算兜住。
        Double hit = BallisticNetMath.heightAt(initialSpeed, clamped, dragCoeff, gravity, horizontalDist);
        if (hit == null) {
            return outOfRange(initialSpeed, dragCoeff, gravity, minAngle, maxAngle);
        }

        double verticalError = Math.abs(hit - verticalDist);
        boolean outOfRange = verticalError > BallisticSolver.maxAcceptableError();
        if (outOfRange) {
            return new BallisticSolver.Result(
                    BallisticSolver.calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity, minAngle, maxAngle),
                    true);
        }
        return new BallisticSolver.Result(clamped, false);
    }

    private static BallisticSolver.Result outOfRange(double initialSpeed, double dragCoeff, double gravity,
                                                     double minAngle, double maxAngle) {
        return new BallisticSolver.Result(
                BallisticSolver.calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity, minAngle, maxAngle),
                true);
    }

    private static double clampAngle(double angle, double minAngle, double maxAngle) {
        if (angle < minAngle) return minAngle;
        if (angle > maxAngle) return maxAngle;
        return angle;
    }

    /** 懒加载网络单例；失败则返回 null 并永久走回退。 */
    private static BallisticNet obtainNet() {
        BallisticNet local = net;
        if (local != null) return local;
        synchronized (BallisticDispatcher.class) {
            local = net;
            if (local != null) return local;
            if (loadAttempted) return null;
            loadAttempted = true;
            try {
                local = BallisticNet.loadExperimental();
                preheat(local);
                net = local;
            } catch (RuntimeException ex) {
                logFallbackOnce(ex);
                return null;
            }
            return local;
        }
    }

    /**
     * JIT 预热。
     *
     * <p>关键性依赖：JIT 需约 1 万次调用才升到 C2，而客户端解算频率是
     * {@code SOLVE_INTERVAL = 5} tick（4 次/秒），服务端开火路径更低（数秒一次）。
     * 不预热的话实际跑的是解释器/C1 版本，实测耗时会是预热后的数倍。
     *
     * <p>依据：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md} 2.3 节。
     */
    private static void preheat(BallisticNet n) {
        double[] input = new double[5];
        for (int i = 0; i < PREHEAT_ITERATIONS; i++) {
            input[0] = 20.0 + (i % 70);
            input[1] = -20.0 + (i % 40);
            input[2] = 3.0;
            input[3] = 0.01;
            input[4] = 9.8 / 196.0;
            n.forwardTicks(input);
        }
    }

    /** 预热次数。取 JIT C2 阈值（约 1 万）的两倍，留出余量。 */
    private static final int PREHEAT_ITERATIONS = 20000;

    private static void logFallbackOnce(RuntimeException ex) {
        if (fallbackLogged) return;
        fallbackLogged = true;
        org.slf4j.LoggerFactory.getLogger(BallisticDispatcher.class)
                .warn("神经网络弹道解算不可用，实验炮将回退到 BallisticSolver。后续相同错误不再记录。", ex);
    }

    /** 供测试与调试查询当前是否已成功加载网络。 */
    public static boolean isNetAvailable() {
        return net != null;
    }

    /** 供测试重置内部状态。 */
    static void resetForTest() {
        synchronized (BallisticDispatcher.class) {
            net = null;
            loadAttempted = false;
            fallbackLogged = false;
        }
    }
}
