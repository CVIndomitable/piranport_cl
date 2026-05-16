package com.piranport.client;

import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModEquipmentConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 弹道解算引擎：给定初速度、阻力、重力、目标距离，计算最佳发射仰角。
 * 使用三分法搜索 [0°, 45°]（低弹道），每次迭代模拟数值弹道。
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

    private static final Map<SolutionKey, Double> cache = createLRUCache();
    private static boolean cacheEnabled = true;

    /** 创建 LRU 缓存，避免缓存满时直接清空导致性能抖动 */
    private static Map<SolutionKey, Double> createLRUCache() {
        return new LinkedHashMap<SolutionKey, Double>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<SolutionKey, Double> eldest) {
                return size() > ModEquipmentConfig.BALLISTIC_CACHE_SIZE.get();
            }
        };
    }

    /** 缓存键量化步长（格）。0.1 格精度足够瞄准使用，可大幅提高缓存命中率。 */
    private static final double DISTANCE_QUANTUM = 0.1;

    private BallisticSolver() {}

    /** 弹道解算入口。从 ModEquipmentConfig / ModArtilleryConfig 读取参数。 */
    public static double solve(double initialSpeed, double dragCoeff, double gravity,
                                double horizontalDist, double verticalDist) {
        if (initialSpeed <= 0 || horizontalDist <= 0) return 45.0 * Math.PI / 180.0;

        // 量化距离以提高缓存命中率
        double qHDist = quantize(horizontalDist);
        double qVDist = quantize(verticalDist);

        if (isCacheEnabled()) {
            SolutionKey key = new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist);
            Double cached = cache.get(key);
            if (cached != null) return cached;
        }

        int maxIters = ModEquipmentConfig.BALLISTIC_MAX_ITERATIONS.get();
        double accuracyThreshold = ModEquipmentConfig.BALLISTIC_ACCURACY.get();
        double noSolutionThreshold = ModArtilleryConfig.BALLISTIC_NO_SOLUTION_THRESHOLD.get();

        // 三分法搜索误差绝对值最小的仰角（避免二分法在非单调函数上的精度问题）
        double lowAngle = 0;
        double highAngle = Math.PI / 4; // 45°：只搜低弹道
        double bestAngle = 45.0 * Math.PI / 180.0;
        double minError = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIters; iter++) {
            if (highAngle - lowAngle < 1e-6) break;

            double mid1 = lowAngle + (highAngle - lowAngle) / 3;
            double mid2 = highAngle - (highAngle - lowAngle) / 3;

            double err1 = Math.abs(simulate(initialSpeed, mid1, dragCoeff, gravity, horizontalDist) - verticalDist);
            double err2 = Math.abs(simulate(initialSpeed, mid2, dragCoeff, gravity, horizontalDist) - verticalDist);

            if (err1 < minError) { minError = err1; bestAngle = mid1; }
            if (err2 < minError) { minError = err2; bestAngle = mid2; }

            if (err1 < accuracyThreshold || err2 < accuracyThreshold) break;

            // 三分法缩小区间：淘汰误差更大的一侧
            if (err1 > err2) {
                lowAngle = mid1;
            } else {
                highAngle = mid2;
            }
        }

        // 无解（打不到目标）时返回最大射程角
        if (minError > noSolutionThreshold) {
            bestAngle = calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity);
        }

        if (isCacheEnabled()) {
            synchronized (cache) {
                cache.put(new SolutionKey(initialSpeed, dragCoeff, gravity, qHDist, qVDist), bestAngle);
            }
        }
        return bestAngle;
    }

    /**
     * 数值弹道模拟，模型与 CannonProjectileEntity.tick() 严格一致。
     * 最大步数从 ModEquipmentConfig.BALLISTIC_MAX_STEPS 读取。
     *
     * 物理模型与 CannonProjectileEntity.tick() 严格一致：
     * 1. 先应用阻力到速度（vx 和 vy 分别缩放）
     * 2. 再应用重力到 vy
     * 3. 最后更新位置
     *
     * 时间步长固定为 1.0 tick，与实体 tick 周期一致。
     */
    private static double simulate(double v0, double angle, double dragCoeff, double gravity, double targetX) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double x = 0, y = 0;
        double dragFactor = Math.max(0.1, 1.0 - dragCoeff);
        int maxSteps = ModEquipmentConfig.BALLISTIC_MAX_STEPS.get();

        for (int step = 0; step < maxSteps; step++) {
            // 1. 应用阻力（与 CannonProjectileEntity.tick() 第130行一致）
            vx *= dragFactor;
            vy *= dragFactor;

            // 2. 应用重力（在阻力之后，与 super.tick() 顺序一致）
            vy -= gravity;

            // 3. 位置更新
            x += vx;
            y += vy;

            // 到达目标水平距离
            if (x >= targetX) return y;
            // 落地（远低于发射点）
            if (y < -10) break;
        }
        return y;
    }

    /** 计算最大射程发射角（有阻力时略高于 45°） */
    private static double calculateMaxRangeAngle(double v0, double dragCoeff, double gravity) {
        if (dragCoeff <= 0) return 45.0 * Math.PI / 180.0;
        double offset = Math.min(10, dragCoeff * 100);
        return (45.0 + offset) * Math.PI / 180.0;
    }

    /** 将距离量化到 DISTANCE_QUANTUM 精度，提高缓存命中率。 */
    private static double quantize(double value) {
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
