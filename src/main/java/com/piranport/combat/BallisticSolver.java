package com.piranport.combat;

import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModEquipmentConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 弹道解算引擎：给定初速度、阻力、重力、目标距离，计算最佳发射仰角。
 * 使用三分法和牛顿迭代法同时计算，取最精确结果。
 * 阻力/重力模型与 {@link com.piranport.entity.CannonProjectileEntity} 一致。
 *
 * <p>参数来源（按优先级）：
 * <ul>
 *   <li>max_iterations / max_steps / accuracy / cache_size → {@link ModEquipmentConfig}</li>
 *   <li>no_solution_threshold / cache_solutions → {@link ModArtilleryConfig}</li>
 * </ul>
 */
public final class BallisticSolver {

    /** 默认重力值（blocks/tick²），匹配 CannonProjectileEntity.getDefaultGravity() */
    public static final double DEFAULT_GRAVITY = 0.05;

    private static final Map<SolutionKey, Result> cache = createLRUCache();
    private static boolean cacheEnabled = true;

    /** 创建 LRU 缓存 */
    private static Map<SolutionKey, Result> createLRUCache() {
        return new LinkedHashMap<SolutionKey, Result>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<SolutionKey, Result> eldest) {
                return size() > ModEquipmentConfig.BALLISTIC_CACHE_SIZE.get();
            }
        };
    }

    /** 线程安全的缓存读（弹道解算可能从多个渲染线程调用） */
    private static Result cacheGet(SolutionKey key) {
        synchronized (cache) { return cache.get(key); }
    }

    /** 线程安全的缓存写 */
    private static void cachePut(SolutionKey key, Result value) {
        synchronized (cache) { cache.put(key, value); }
    }

    /** 缓存键量化步长（格）。0 = 不量化，绝对精确。 */
    private static final double DISTANCE_QUANTUM = 0.0;

    private BallisticSolver() {}

    /** 弹道解算结果。 */
    public record Result(double angle, boolean outOfRange) {}

    /** 内部求解结果（含迭代次数）。 */
    private record SolveResult(double angle, int iterations) {}

    /**
     * 弹道解算入口。从 ModEquipmentConfig / ModArtilleryConfig 读取参数。
     * @param vz0 垂直于射击平面的初速度分量（用于精确计算 3D 阻力），通常为 0
     */
    public static Result solve(double initialSpeed, double dragCoeff, double gravity,
                                double horizontalDist, double verticalDist, double vz0) {
        if (initialSpeed <= 0 || horizontalDist <= 0)
            return new Result(45.0 * Math.PI / 180.0, true);

        // 量化距离以提高缓存命中率
        double qHDist = quantize(horizontalDist);
        double qVDist = quantize(verticalDist);

        if (isCacheEnabled()) {
            SolutionKey key = new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist);
            Result cached = cacheGet(key);
            if (cached != null) return cached;
        }

        int maxIters = ModEquipmentConfig.BALLISTIC_MAX_ITERATIONS.get();
        double accuracyThreshold = ModEquipmentConfig.BALLISTIC_ACCURACY.get();
        double noSolutionThreshold = ModArtilleryConfig.BALLISTIC_NO_SOLUTION_THRESHOLD.get();

        BallisticSolverStats stats = BallisticSolverStats.getInstance();

        // ===== 三分法求解 =====
        long ternaryStart = System.nanoTime();
        SolveResult ternaryResult = solveTernary(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist,
                maxIters, accuracyThreshold, noSolutionThreshold, vz0);
        long ternaryElapsed = System.nanoTime() - ternaryStart;
        double ternaryAngle = ternaryResult.angle();

        // 计算三分法精度（误差）
        double ternaryError = Math.abs(simulate(initialSpeed, ternaryAngle, dragCoeff, gravity, horizontalDist, vz0) - verticalDist);
        stats.recordTernary(ternaryElapsed, ternaryError);
        stats.setLastTernaryIters(ternaryResult.iterations());

        // ===== 牛顿迭代法求解 =====
        long newtonStart = System.nanoTime();
        SolveResult newtonResult = solveNewton(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist,
                maxIters, accuracyThreshold, noSolutionThreshold, vz0);
        long newtonElapsed = System.nanoTime() - newtonStart;
        double newtonAngle = newtonResult.angle();

        // 计算牛顿法精度（误差）
        double newtonError = Math.abs(simulate(initialSpeed, newtonAngle, dragCoeff, gravity, horizontalDist, vz0) - verticalDist);
        stats.recordNewton(newtonElapsed, newtonError);
        stats.setLastNewtonIters(newtonResult.iterations());

        // ===== 取最精确的结果 =====
        double bestAngle;
        double bestError;
        BallisticSolverStats.Algorithm chosen;

        if (newtonError < ternaryError) {
            bestAngle = newtonAngle;
            bestError = newtonError;
            chosen = BallisticSolverStats.Algorithm.NEWTON;
        } else {
            bestAngle = ternaryAngle;
            bestError = ternaryError;
            chosen = BallisticSolverStats.Algorithm.TERNARY;
        }

        stats.recordCombined(bestError, chosen);

        // 无解（打不到目标）时返回最大射程角 + 超出射程标记
        boolean outOfRange = bestError > noSolutionThreshold;
        if (outOfRange) {
            bestAngle = calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity);
        }

        Result result = new Result(bestAngle, outOfRange);
        if (isCacheEnabled()) {
            cachePut(new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist), result);
        }
        return result;
    }

    /**
     * 三分法求解：网格搜索 + 三分法精细搜索
     */
    private static SolveResult solveTernary(double initialSpeed, double dragCoeff, double gravity,
                                        double horizontalDist, double verticalDist,
                                        int maxIters, double accuracyThreshold, double noSolutionThreshold,
                                        double vz0) {
        double searchLow = -Math.PI / 4;   // -45°：允许俯射
        double searchHigh = Math.PI / 4;   //  45°：低弹道上界
        double bestAngle = 45.0 * Math.PI / 180.0;
        double minError = Double.MAX_VALUE;
        int totalIters = 0;

        // 粗网格扫描（64 步），找到全局最优区域
        int gridSteps = 64;
        double gridStep = (searchHigh - searchLow) / gridSteps;
        double bestGridAngle = searchLow;
        for (int g = 0; g <= gridSteps; g++) {
            double a = searchLow + g * gridStep;
            double err = Math.abs(simulate(initialSpeed, a, dragCoeff, gravity, horizontalDist, vz0) - verticalDist);
            if (err < minError) {
                minError = err;
                bestAngle = a;
                bestGridAngle = a;
            }
        }
        totalIters = gridSteps + 1;

        if (minError > accuracyThreshold) {
            // 以网格最优点为中心，三分法精细搜索
            double refineRadius = gridStep * 2;
            double lowAngle = Math.max(searchLow, bestGridAngle - refineRadius);
            double highAngle = Math.min(searchHigh, bestGridAngle + refineRadius);

            for (int iter = 0; iter < maxIters; iter++) {
                if (highAngle - lowAngle < 1e-7) break;

                double mid1 = lowAngle + (highAngle - lowAngle) / 3;
                double mid2 = highAngle - (highAngle - lowAngle) / 3;

                double err1 = Math.abs(simulate(initialSpeed, mid1, dragCoeff, gravity, horizontalDist, vz0) - verticalDist);
                double err2 = Math.abs(simulate(initialSpeed, mid2, dragCoeff, gravity, horizontalDist, vz0) - verticalDist);

                if (err1 < minError) { minError = err1; bestAngle = mid1; }
                if (err2 < minError) { minError = err2; bestAngle = mid2; }

                totalIters += 2;

                if (err1 < accuracyThreshold || err2 < accuracyThreshold) break;

                if (err1 > err2) {
                    lowAngle = mid1;
                } else {
                    highAngle = mid2;
                }
            }
        }

        return new SolveResult(bestAngle, totalIters);
    }

    /**
     * 牛顿迭代法求解：使用数值导数进行迭代
     */
    private static SolveResult solveNewton(double initialSpeed, double dragCoeff, double gravity,
                                       double horizontalDist, double verticalDist,
                                       int maxIters, double accuracyThreshold, double noSolutionThreshold,
                                       double vz0) {
        // 初始猜测：使用最大射程角的一半
        double angle = 22.5 * Math.PI / 180.0;
        double delta = 1e-5; // 数值导数的微小增量
        int iters = 0;

        for (int iter = 0; iter < maxIters; iter++) {
            iters++;
            double f = simulate(initialSpeed, angle, dragCoeff, gravity, horizontalDist, vz0) - verticalDist;

            // 检查是否满足精度要求
            if (Math.abs(f) < accuracyThreshold) {
                return new SolveResult(angle, iters);
            }

            // 数值导数：f'(x) ≈ (f(x+δ) - f(x-δ)) / (2δ)
            double fPlus = simulate(initialSpeed, angle + delta, dragCoeff, gravity, horizontalDist, vz0) - verticalDist;
            double fMinus = simulate(initialSpeed, angle - delta, dragCoeff, gravity, horizontalDist, vz0) - verticalDist;
            double derivative = (fPlus - fMinus) / (2 * delta);

            // 避免除以零
            if (Math.abs(derivative) < 1e-10) {
                break;
            }

            // 牛顿迭代：x_{n+1} = x_n - f(x_n) / f'(x_n)
            double newAngle = angle - f / derivative;

            // 限制角度范围 [-45°, 45°]
            newAngle = Math.max(-Math.PI / 4, Math.min(Math.PI / 4, newAngle));

            // 检查收敛
            if (Math.abs(newAngle - angle) < 1e-10) {
                return new SolveResult(newAngle, iters);
            }

            angle = newAngle;
        }

        // 牛顿法未收敛，返回当前最佳估计
        return new SolveResult(angle, iters);
    }

    /**
     * 数值弹道模拟，模型与 CannonProjectileEntity.tick() 严格一致。
     * 最大步数从 ModEquipmentConfig.BALLISTIC_MAX_STEPS 读取。
     *
     * 物理模型：
     * 1. 阻力：与当前速度矢量反向，大小恒定 = dragCoeff
     * 2. 重力：每 tick vy 减去 gravity（可配置）
     * 3. 位置更新
     *
     * 时间步长固定为 1.0 tick，与实体 tick 周期一致。
     */
    private static double simulate(double v0, double angle, double dragCoeff, double gravity,
                                   double targetX, double vz0) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double vz = vz0;
        double x = 0, y = 0;
        int maxSteps = ModEquipmentConfig.BALLISTIC_MAX_STEPS.get();

        for (int step = 0; step < maxSteps; step++) {
            // 1. 应用阻力：使用 3D 速度计算（与 CannonProjectileEntity.tick() 一致）
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (speed > 1e-9) {
                vx -= dragCoeff * (vx / speed);
                vy -= dragCoeff * (vy / speed);
                vz -= dragCoeff * (vz / speed);
            }

            // 2. 应用重力
            vy -= gravity;

            // 3. 位置更新（只跟踪水平和垂直分量）
            x += vx;
            y += vy;

            // 到达目标水平距离
            if (x >= targetX) return y;
            // 落地（远低于发射点，放宽以支持大高低差场景）
            if (y < -300) break;
        }
        return y;
    }

    /** 计算最大射程发射角（有阻力时略高于 45°） */
    public static double calculateMaxRangeAngle(double v0, double dragCoeff, double gravity) {
        if (dragCoeff <= 0) return 45.0 * Math.PI / 180.0;
        double offset = Math.min(10, dragCoeff * 100);
        return (45.0 + offset) * Math.PI / 180.0;
    }

    /** 将距离量化到 DISTANCE_QUANTUM 精度。0 = 不量化。 */
    private static double quantize(double value) {
        if (DISTANCE_QUANTUM <= 0) return value;
        return Math.round(value / DISTANCE_QUANTUM) * DISTANCE_QUANTUM;
    }

    /** 从 ModArtilleryConfig 读取缓存开关。 */
    private static boolean isCacheEnabled() {
        return cacheEnabled && ModArtilleryConfig.PERF_CACHE_SOLUTIONS.get();
    }

    /** 强制开关缓存（用于调试/性能测试）。 */
    public static void setCacheEnabled(boolean enabled) {
        cacheEnabled = enabled;
        if (!enabled) cache.clear();
    }

    public static void clearCache() {
        cache.clear();
    }

    /** 缓存键：包含所有影响弹道的参数，距离已量化 */
    private record SolutionKey(double speed, double drag, double gravity, double hDist, double vDist) {}
}
