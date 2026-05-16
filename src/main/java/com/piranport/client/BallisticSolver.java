package com.piranport.client;

import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModEquipmentConfig;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * 弹道解算引擎：给定初速度、阻力、重力、目标距离，计算最佳发射仰角。
 * 使用二分法搜索 [0°, 89°]，每次迭代模拟数值弹道。
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

    private static final Object2DoubleOpenHashMap<SolutionKey> cache = new Object2DoubleOpenHashMap<>();
    private static boolean cacheEnabled = true;

    private BallisticSolver() {}

    /** 弹道解算入口。从 ModEquipmentConfig / ModArtilleryConfig 读取参数。 */
    public static double solve(double initialSpeed, double dragCoeff, double gravity,
                                double horizontalDist, double verticalDist) {
        if (initialSpeed <= 0 || horizontalDist <= 0) return 45.0 * Math.PI / 180.0;

        if (isCacheEnabled()) {
            SolutionKey key = new SolutionKey(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist);
            double cached = cache.getOrDefault(key, Double.NaN);
            if (!Double.isNaN(cached)) return cached;
        }

        int maxIters = ModEquipmentConfig.BALLISTIC_MAX_ITERATIONS.get();
        double accuracyThreshold = ModEquipmentConfig.BALLISTIC_ACCURACY.get();
        double noSolutionThreshold = ModArtilleryConfig.BALLISTIC_NO_SOLUTION_THRESHOLD.get();

        double minAngle = 0;
        double maxAngle = Math.PI / 2 - 0.01; // ~89°
        double bestAngle = 45.0 * Math.PI / 180.0;
        double minError = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIters; iter++) {
            double angle = (minAngle + maxAngle) / 2;
            double finalY = simulate(initialSpeed, angle, dragCoeff, gravity, horizontalDist);
            double error = finalY - verticalDist;

            double absError = Math.abs(error);
            if (absError < minError) {
                minError = absError;
                bestAngle = angle;
            }

            if (error < 0) {
                minAngle = angle;
            } else {
                maxAngle = angle;
            }

            if (absError < accuracyThreshold) break;
        }

        // 无解（打不到目标）时返回最大射程角 ≈ 45°（受阻力影响往稍高处偏移）
        if (minError > noSolutionThreshold) {
            bestAngle = calculateMaxRangeAngle(initialSpeed, dragCoeff, gravity);
        }

        if (isCacheEnabled()) {
            int cacheSize = ModEquipmentConfig.BALLISTIC_CACHE_SIZE.get();
            synchronized (cache) {
                if (cache.size() >= cacheSize) cache.clear();
                cache.put(new SolutionKey(initialSpeed, dragCoeff, gravity, horizontalDist, verticalDist), bestAngle);
            }
        }
        return bestAngle;
    }

    /**
     * 数值弹道模拟，步长 = 1 MC tick，模型匹配 CannonProjectileEntity。
     * 最大步数从 ModEquipmentConfig.BALLISTIC_MAX_STEPS 读取。
     */
    private static double simulate(double v0, double angle, double dragCoeff, double gravity, double targetX) {
        double vx = v0 * Math.cos(angle);
        double vy = v0 * Math.sin(angle);
        double x = 0, y = 0;
        double factor = Math.max(0.1, 1.0 - dragCoeff);
        int maxSteps = ModEquipmentConfig.BALLISTIC_MAX_STEPS.get();

        for (int step = 0; step < maxSteps; step++) {
            // 阻力（与 CannonProjectileEntity.tick() 一致）
            vx *= factor;
            vy *= factor;
            // 重力
            vy -= gravity;
            // 位置更新
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

    /** 缓存键：包含所有影响弹道的参数 */
    private record SolutionKey(double speed, double drag, double gravity, double hDist, double vDist) {}
}
