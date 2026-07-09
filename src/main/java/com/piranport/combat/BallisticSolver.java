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
    private static final double UNRESTRICTED_MIN_ANGLE = Math.toRadians(-89.0);
    private static final double UNRESTRICTED_MAX_ANGLE = Math.toRadians(89.0);
    private static final int PRECISE_SCAN_STEPS = 512;
    private static final int PRECISE_REFINE_ITERS = 80;
    private static final double PRECISE_VERTICAL_EPSILON = 1.0e-4;
    private static final double PRECISE_ANGLE_EPSILON = 1.0e-9;

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

    /** 误差指标，单位均为格。 */
    private record ErrorMetrics(double vertical, double horizontal) {
        double total() { return Math.hypot(vertical, horizontal); }
    }

    /** 解算器内部使用的 3D 速度。 */
    private record Velocity(double vx, double vy, double vz) {}

    /** 原版 ThrowableProjectile 在空气中的每 tick 速度保留比例。 */
    private static final double VANILLA_AIR_DRAG = 0.99;

    /**
     * 弹道解算入口。从 ModEquipmentConfig / ModArtilleryConfig 读取参数。
     * @param vz0 垂直于射击平面的初速度分量（用于精确计算 3D 阻力），通常为 0
     */
    public static Result solve(double initialSpeed, double dragCoeff, double gravity,
                                double horizontalDist, double verticalDist, double vz0) {
        return solve(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist, vz0,
                UNRESTRICTED_MIN_ANGLE, UNRESTRICTED_MAX_ANGLE);
    }

    /**
     * 带搜索角度范围的弹道解算入口。
     * @param minAngle 最小发射仰角（弧度，向上为正）
     * @param maxAngle 最大发射仰角（弧度，向上为正）
     */
    public static Result solve(double initialSpeed, double dragCoeff, double gravity,
                                double horizontalDist, double verticalDist, double vz0,
                                double minAngle, double maxAngle) {
        if (initialSpeed <= 0 || horizontalDist <= 0)
            return new Result(clampAngle(Math.toRadians(45.0), minAngle, maxAngle), true);

        if (maxAngle < minAngle) {
            double t = maxAngle;
            maxAngle = minAngle;
            minAngle = t;
        }

        // 量化距离以提高缓存命中率
        double qHDist = quantize(horizontalDist);
        double qVDist = quantize(verticalDist);

        if (isCacheEnabled()) {
            SolutionKey key = new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist, minAngle, maxAngle);
            Result cached = cacheGet(key);
            if (cached != null) return cached;
        }

        long solveStart = System.nanoTime();
        int maxIters = ModEquipmentConfig.BALLISTIC_MAX_ITERATIONS.get();
        double accuracyThreshold = ModEquipmentConfig.BALLISTIC_ACCURACY.get();
        if (accuracyThreshold <= 0.0) accuracyThreshold = 0.01;
        double noSolutionThreshold = ModArtilleryConfig.BALLISTIC_NO_SOLUTION_THRESHOLD.get();

        BallisticSolverStats stats = BallisticSolverStats.getInstance();

        // ===== 三分法求解 =====
        long ternaryStart = System.nanoTime();
        SolveResult ternaryResult = solveTernary(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist,
                maxIters, accuracyThreshold, noSolutionThreshold, vz0, minAngle, maxAngle);
        long ternaryElapsed = System.nanoTime() - ternaryStart;
        double ternaryAngle = ternaryResult.angle();

        // 计算三分法精度（误差）
        ErrorMetrics ternaryError = evaluateError(initialSpeed, ternaryAngle, dragCoeff, gravity,
                horizontalDist, verticalDist, vz0);
        stats.recordTernary(ternaryElapsed, ternaryError.vertical(), ternaryError.horizontal());
        stats.setLastTernaryIters(ternaryResult.iterations());

        // ===== 牛顿迭代法求解 =====
        long newtonStart = System.nanoTime();
        SolveResult newtonResult = solveNewton(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist,
                maxIters, accuracyThreshold, noSolutionThreshold, vz0, minAngle, maxAngle);
        long newtonElapsed = System.nanoTime() - newtonStart;
        double newtonAngle = newtonResult.angle();

        // 计算牛顿法精度（误差）
        ErrorMetrics newtonError = evaluateError(initialSpeed, newtonAngle, dragCoeff, gravity,
                horizontalDist, verticalDist, vz0);
        stats.recordNewton(newtonElapsed, newtonError.vertical(), newtonError.horizontal());
        stats.setLastNewtonIters(newtonResult.iterations());

        // ===== 取最精确的结果 =====
        double bestAngle;
        ErrorMetrics bestError;
        BallisticSolverStats.Algorithm chosen;

        if (newtonError.total() < ternaryError.total()) {
            bestAngle = newtonAngle;
            bestError = newtonError;
            chosen = BallisticSolverStats.Algorithm.NEWTON;
        } else {
            bestAngle = ternaryAngle;
            bestError = ternaryError;
            chosen = BallisticSolverStats.Algorithm.TERNARY;
        }

        // ===== 精确解算 =====
        // 三分法/牛顿法给出候选角后，再全范围找垂直根并以总误差收敛，避免高低弹道或局部解造成偏差。
        SolveResult preciseResult = solvePrecise(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist,
                maxIters, accuracyThreshold, vz0, minAngle, maxAngle);
        double preciseAngle = preciseResult.angle();
        ErrorMetrics preciseError = evaluateError(initialSpeed, preciseAngle, dragCoeff, gravity,
                horizontalDist, verticalDist, vz0);
        if (isBetter(preciseError, preciseAngle, bestError, bestAngle)) {
            bestAngle = preciseAngle;
            bestError = preciseError;
            chosen = BallisticSolverStats.Algorithm.COMBINED;
        }

        long totalElapsed = System.nanoTime() - solveStart;
        stats.recordCombined(bestError.vertical(), bestError.horizontal(), chosen, totalElapsed);

        // 无解（打不到目标）时返回最大射程角 + 超出射程标记
        boolean outOfRange = bestError.total() > noSolutionThreshold;
        if (outOfRange) {
            bestAngle = calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity, minAngle, maxAngle);
        }

        Result result = new Result(bestAngle, outOfRange);
        if (isCacheEnabled()) {
            cachePut(new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist, minAngle, maxAngle), result);
        }
        return result;
    }

    private static ErrorMetrics evaluateError(double v0, double angle, double dragCoeff, double gravity,
                                              double targetX, double targetY, double vz0) {
        double vertical = Math.abs(simulate(v0, angle, dragCoeff, gravity, targetX, vz0) - targetY);
        double horizontal = computeHorizontalError(v0, angle, dragCoeff, gravity, targetX, targetY, vz0);
        return new ErrorMetrics(vertical, horizontal);
    }

    private static SolveResult solvePrecise(double initialSpeed, double dragCoeff, double gravity,
                                            double horizontalDist, double verticalDist,
                                            int maxIters, double accuracyThreshold,
                                            double vz0, double minAngle, double maxAngle) {
        int totalIters = 0;
        int scanSteps = Math.max(32, PRECISE_SCAN_STEPS);
        double step = (maxAngle - minAngle) / scanSteps;
        if (step <= 0.0) return new SolveResult(minAngle, 0);

        double bestSampleAngle = minAngle;
        double bestSampleAbs = Double.MAX_VALUE;
        SolveResult bestRoot = null;
        ErrorMetrics bestRootError = null;

        double prevAngle = minAngle;
        double prevF = verticalErrorSigned(initialSpeed, prevAngle, dragCoeff, gravity, horizontalDist, verticalDist, vz0);
        totalIters++;
        if (Double.isFinite(prevF)) {
            bestSampleAbs = Math.abs(prevF);
        }

        for (int i = 1; i <= scanSteps; i++) {
            double angle = minAngle + step * i;
            double f = verticalErrorSigned(initialSpeed, angle, dragCoeff, gravity, horizontalDist, verticalDist, vz0);
            totalIters++;

            if (Double.isFinite(f) && Math.abs(f) < bestSampleAbs) {
                bestSampleAbs = Math.abs(f);
                bestSampleAngle = angle;
            }

            if (Double.isFinite(f) && Math.abs(f) <= PRECISE_VERTICAL_EPSILON) {
                ErrorMetrics error = evaluateError(initialSpeed, angle, dragCoeff, gravity,
                        horizontalDist, verticalDist, vz0);
                if (bestRoot == null || isBetter(error, angle, bestRootError, bestRoot.angle())) {
                    bestRoot = new SolveResult(angle, totalIters);
                    bestRootError = error;
                }
            }

            if (Double.isFinite(prevF) && Double.isFinite(f) && prevF * f < 0.0) {
                SolveResult root = solveVerticalRootBisection(initialSpeed, dragCoeff, gravity,
                        horizontalDist, verticalDist, vz0, prevAngle, angle, prevF, f,
                        Math.max(maxIters, PRECISE_REFINE_ITERS),
                        Math.min(accuracyThreshold, PRECISE_VERTICAL_EPSILON));
                totalIters += root.iterations();
                ErrorMetrics error = evaluateError(initialSpeed, root.angle(), dragCoeff, gravity,
                        horizontalDist, verticalDist, vz0);
                if (bestRoot == null || isBetter(error, root.angle(), bestRootError, bestRoot.angle())) {
                    bestRoot = root;
                    bestRootError = error;
                }
            }

            prevAngle = angle;
            prevF = f;
        }

        if (bestRoot != null) {
            return new SolveResult(bestRoot.angle(), totalIters);
        }

        SolveResult minimized = minimizeTotalError(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist,
                vz0, minAngle, maxAngle, bestSampleAngle);
        return new SolveResult(minimized.angle(), totalIters + minimized.iterations());
    }

    private static SolveResult solveVerticalRootBisection(double initialSpeed, double dragCoeff, double gravity,
                                                          double horizontalDist, double verticalDist, double vz0,
                                                          double lowAngle, double highAngle,
                                                          double lowF, double highF,
                                                          int maxIters, double verticalEpsilon) {
        double bestAngle = Math.abs(lowF) <= Math.abs(highF) ? lowAngle : highAngle;
        double bestAbs = Math.min(Math.abs(lowF), Math.abs(highF));
        int iters = 0;

        for (int iter = 0; iter < maxIters; iter++) {
            iters++;
            double mid = (lowAngle + highAngle) * 0.5;
            double f = verticalErrorSigned(initialSpeed, mid, dragCoeff, gravity, horizontalDist, verticalDist, vz0);
            if (Double.isFinite(f) && Math.abs(f) < bestAbs) {
                bestAbs = Math.abs(f);
                bestAngle = mid;
            }
            if (!Double.isFinite(f) || Math.abs(f) <= verticalEpsilon || highAngle - lowAngle <= PRECISE_ANGLE_EPSILON) {
                break;
            }
            if (lowF * f <= 0.0) {
                highAngle = mid;
                highF = f;
            } else {
                lowAngle = mid;
                lowF = f;
            }
        }

        return new SolveResult(bestAngle, iters);
    }

    private static SolveResult minimizeTotalError(double initialSpeed, double dragCoeff, double gravity,
                                                  double horizontalDist, double verticalDist, double vz0,
                                                  double minAngle, double maxAngle, double seedAngle) {
        int iters = 0;
        int gridSteps = Math.max(64, PRECISE_SCAN_STEPS / 2);
        double gridStep = (maxAngle - minAngle) / gridSteps;
        double bestAngle = clampAngle(seedAngle, minAngle, maxAngle);
        ErrorMetrics bestError = evaluateError(initialSpeed, bestAngle, dragCoeff, gravity,
                horizontalDist, verticalDist, vz0);
        iters++;

        for (int i = 0; i <= gridSteps; i++) {
            double angle = minAngle + gridStep * i;
            ErrorMetrics error = evaluateError(initialSpeed, angle, dragCoeff, gravity,
                    horizontalDist, verticalDist, vz0);
            iters++;
            if (isBetter(error, angle, bestError, bestAngle)) {
                bestAngle = angle;
                bestError = error;
            }
        }

        double low = Math.max(minAngle, bestAngle - gridStep * 2.0);
        double high = Math.min(maxAngle, bestAngle + gridStep * 2.0);
        for (int iter = 0; iter < PRECISE_REFINE_ITERS && high - low > PRECISE_ANGLE_EPSILON; iter++) {
            iters += 2;
            double mid1 = low + (high - low) / 3.0;
            double mid2 = high - (high - low) / 3.0;
            ErrorMetrics e1 = evaluateError(initialSpeed, mid1, dragCoeff, gravity, horizontalDist, verticalDist, vz0);
            ErrorMetrics e2 = evaluateError(initialSpeed, mid2, dragCoeff, gravity, horizontalDist, verticalDist, vz0);

            if (isBetter(e1, mid1, bestError, bestAngle)) {
                bestAngle = mid1;
                bestError = e1;
            }
            if (isBetter(e2, mid2, bestError, bestAngle)) {
                bestAngle = mid2;
                bestError = e2;
            }

            if (e1.total() > e2.total()) {
                low = mid1;
            } else {
                high = mid2;
            }
        }

        return new SolveResult(bestAngle, iters);
    }

    private static double verticalErrorSigned(double v0, double angle, double dragCoeff, double gravity,
                                              double targetX, double targetY, double vz0) {
        return simulate(v0, angle, dragCoeff, gravity, targetX, vz0) - targetY;
    }

    private static boolean isBetter(ErrorMetrics candidate, double candidateAngle,
                                    ErrorMetrics current, double currentAngle) {
        if (current == null) return true;
        double candidateTotal = candidate.total();
        double currentTotal = current.total();
        if (candidateTotal < currentTotal - 1.0e-6) return true;
        if (Math.abs(candidateTotal - currentTotal) <= 1.0e-6) {
            return Math.abs(candidateAngle) < Math.abs(currentAngle);
        }
        return false;
    }

    /**
     * 三分法求解：网格搜索 + 三分法精细搜索
     */
    private static SolveResult solveTernary(double initialSpeed, double dragCoeff, double gravity,
                                        double horizontalDist, double verticalDist,
                                        int maxIters, double accuracyThreshold, double noSolutionThreshold,
                                        double vz0, double minAngle, double maxAngle) {
        double searchLow = minAngle;
        double searchHigh = maxAngle;
        double bestAngle = clampAngle(Math.toRadians(45.0), searchLow, searchHigh);
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
                                       double vz0, double minAngle, double maxAngle) {
        // 初始猜测：使用俯仰范围中点
        double angle = (minAngle + maxAngle) * 0.5;
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

            // 限制角度范围
            newAngle = clampAngle(newAngle, minAngle, maxAngle);

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
     * 1. 自定义阻力：每 tick 按 dragCoeff 做比例阻尼
     * 2. 位置更新与碰撞检测使用阻力后的当前速度
     * 3. 原版空气阻力 0.99 与重力在位置更新后影响下一 tick 速度
     *
     * 时间步长固定为 1.0 tick，与实体 tick 周期一致。
     */
    private static double simulate(double v0, double angle, double dragCoeff, double gravity,
                                   double targetX, double vz0) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double vz = vz0;
        double x = 0, y = 0;
        int maxSteps = simulationStepLimit(v0, targetX);

        for (int step = 0; step < maxSteps; step++) {
            double prevX = x;
            double prevY = y;

            // 1. 应用自定义阻力：使用 3D 速度计算（与 CannonProjectileEntity.tick() 一致）
            Velocity dragged = applyLinearDrag(vx, vy, vz, dragCoeff);
            vx = dragged.vx();
            vy = dragged.vy();
            vz = dragged.vz();

            // 2. 位置更新（实体在 super.tick() 中先移动，随后才应用原版阻力/重力）
            x += vx;
            y += vy;

            // 到达目标水平距离：用线性插值估算跨越目标点那一刻的高度，避免远距离整 tick 量化误差
            if (x >= targetX) {
                double dx = x - prevX;
                if (Math.abs(dx) < 1e-9) return y;
                double t = (targetX - prevX) / dx;
                return prevY + (y - prevY) * t;
            }

            // 3. 原版空气阻力 + 重力影响下一 tick 速度
            vx *= VANILLA_AIR_DRAG;
            vy *= VANILLA_AIR_DRAG;
            vz *= VANILLA_AIR_DRAG;
            vy -= gravity;

            // 落地（远低于发射点，放宽以支持大高低差场景）
            if (y < -300) break;
        }
        return y;
    }

    private static double computeHorizontalError(double v0, double angle, double dragCoeff, double gravity,
                                                 double targetX, double targetY, double vz0) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double vz = vz0;
        double x = 0.0;
        double y = 0.0;
        double bestCrossingError = Double.MAX_VALUE;
        double bestFallbackX = 0.0;
        double bestFallbackVertical = Math.abs(targetY);
        int maxSteps = simulationStepLimit(v0, targetX);

        for (int step = 0; step < maxSteps; step++) {
            double prevX = x;
            double prevY = y;

            Velocity dragged = applyLinearDrag(vx, vy, vz, dragCoeff);
            vx = dragged.vx();
            vy = dragged.vy();
            vz = dragged.vz();

            x += vx;
            y += vy;

            double fallbackVertical = Math.abs(y - targetY);
            if (fallbackVertical < bestFallbackVertical) {
                bestFallbackVertical = fallbackVertical;
                bestFallbackX = x;
            }

            double prevRel = prevY - targetY;
            double currRel = y - targetY;
            if (prevRel == 0.0) {
                bestCrossingError = Math.min(bestCrossingError, Math.abs(prevX - targetX));
            }
            if (prevRel * currRel <= 0.0) {
                double dy = y - prevY;
                double crossX = Math.abs(dy) < 1e-9
                        ? x
                        : prevX + (x - prevX) * ((targetY - prevY) / dy);
                bestCrossingError = Math.min(bestCrossingError, Math.abs(crossX - targetX));
            }

            vx *= VANILLA_AIR_DRAG;
            vy *= VANILLA_AIR_DRAG;
            vz *= VANILLA_AIR_DRAG;
            vy -= gravity;

            if (y < -300) break;
        }

        return bestCrossingError == Double.MAX_VALUE
                ? Math.abs(bestFallbackX - targetX)
                : bestCrossingError;
    }

    /** 计算最大射程发射角（有阻力时略高于 45°） */
    public static double calculateMaxRangeAngle(double v0, double dragCoeff, double gravity) {
        return calculateMaxRangeAngle(v0, dragCoeff, gravity, UNRESTRICTED_MIN_ANGLE, UNRESTRICTED_MAX_ANGLE);
    }

    /** 计算给定搜索角度范围内的最大平射距离发射角。 */
    public static double calculateMaxRangeAngle(double v0, double dragCoeff, double gravity,
                                                double minAngle, double maxAngle) {
        if (maxAngle < minAngle) {
            double t = maxAngle;
            maxAngle = minAngle;
            minAngle = t;
        }
        if (v0 <= 0) return clampAngle(Math.toRadians(45.0), minAngle, maxAngle);
        if (Math.abs(maxAngle - minAngle) < 1e-9) return minAngle;

        double bestAngle = minAngle;
        double bestRange = -1.0;
        int gridSteps = 96;
        double gridStep = (maxAngle - minAngle) / gridSteps;
        for (int g = 0; g <= gridSteps; g++) {
            double angle = minAngle + g * gridStep;
            double range = simulateHorizontalRange(v0, angle, dragCoeff, gravity, 0.0);
            if (range > bestRange) {
                bestRange = range;
                bestAngle = angle;
            }
        }

        double low = Math.max(minAngle, bestAngle - gridStep * 2);
        double high = Math.min(maxAngle, bestAngle + gridStep * 2);
        for (int iter = 0; iter < 48 && high - low > 1e-7; iter++) {
            double mid1 = low + (high - low) / 3.0;
            double mid2 = high - (high - low) / 3.0;
            double r1 = simulateHorizontalRange(v0, mid1, dragCoeff, gravity, 0.0);
            double r2 = simulateHorizontalRange(v0, mid2, dragCoeff, gravity, 0.0);
            if (r1 < r2) {
                low = mid1;
                if (r2 > bestRange) {
                    bestRange = r2;
                    bestAngle = mid2;
                }
            } else {
                high = mid2;
                if (r1 > bestRange) {
                    bestRange = r1;
                    bestAngle = mid1;
                }
            }
        }
        return bestAngle;
    }

    private static double simulateHorizontalRange(double v0, double angle, double dragCoeff,
                                                  double gravity, double vz0) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double vz = vz0;
        double x = 0.0;
        double y = 0.0;
        int maxSteps = Math.max(ModEquipmentConfig.BALLISTIC_MAX_STEPS.get(), 1000);

        for (int step = 0; step < maxSteps; step++) {
            double prevX = x;
            double prevY = y;

            Velocity dragged = applyLinearDrag(vx, vy, vz, dragCoeff);
            vx = dragged.vx();
            vy = dragged.vy();
            vz = dragged.vz();

            x += vx;
            y += vy;

            if (step > 0 && y <= 0.0) {
                double dy = y - prevY;
                if (Math.abs(dy) < 1e-9) return Math.max(0.0, x);
                double t = (0.0 - prevY) / dy;
                return Math.max(0.0, prevX + (x - prevX) * t);
            }

            vx *= VANILLA_AIR_DRAG;
            vy *= VANILLA_AIR_DRAG;
            vz *= VANILLA_AIR_DRAG;
            vy -= gravity;
        }
        return Math.max(0.0, x);
    }

    private static Velocity applyLinearDrag(double vx, double vy, double vz, double dragCoeff) {
        if (!Double.isFinite(dragCoeff) || dragCoeff <= 0.0) return new Velocity(vx, vy, vz);
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed <= 1.0e-9) return new Velocity(0.0, 0.0, 0.0);
        double scale = 1.0 / (1.0 + Math.max(0.0, dragCoeff));
        return new Velocity(vx * scale, vy * scale, vz * scale);
    }

    private static int simulationStepLimit(double initialSpeed, double targetX) {
        int configured = ModEquipmentConfig.BALLISTIC_MAX_STEPS.get();
        if (targetX <= 0.0 || initialSpeed <= 0.0) return configured;

        double conservativeHorizontalSpeed = Math.max(0.05, initialSpeed * 0.15);
        int distanceDriven = (int) Math.ceil(targetX / conservativeHorizontalSpeed) + 200;
        return Math.min(4000, Math.max(configured, distanceDriven));
    }

    private static double clampAngle(double angle, double minAngle, double maxAngle) {
        if (maxAngle < minAngle) {
            double t = maxAngle;
            maxAngle = minAngle;
            minAngle = t;
        }
        return Math.max(minAngle, Math.min(maxAngle, angle));
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
    private record SolutionKey(double speed, double drag, double gravity, double hDist, double vDist,
                               double minAngle, double maxAngle) {}
}
