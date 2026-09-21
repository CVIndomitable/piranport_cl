package com.piranport.combat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * BallisticSolver 性能实测（神经网络拟合实验的前置判据）。
 *
 * <p>对应文档：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md}
 *
 * <p>目的：量化"用神经网络替换解算器"的收益上限。
 * 若 cache-miss 的单次耗时占一个游戏 tick 预算的比例极低，
 * 则该方案不值得承担可调试性的代价。
 *
 * <p>本测试不断言性能（避免 CI 机器差异导致误报），只输出分布供人工判读，
 * 唯一的断言是"解算不应慢到阻塞 tick"这一宽松上界。
 */
class BallisticSolverPerfTest {

    /** 各炮实际参数（WeaponItems 注册值，gravity 已 /196 归一化） */
    private static final double[][] GUNS = {
            {2.5, 0.015,  9.8 / 196.0, -10.0, 60.0},  // small_gun
            {3.0, 0.010,  9.8 / 196.0,  -5.0, 50.0},  // medium_gun
            {3.5, 0.008,  9.8 / 196.0,  -5.0, 45.0},  // large_gun 系
    };

    private static final int N = 20000;

    private BallisticSolver.ConfigSuppliers originalSuppliers;

    @BeforeEach
    void setUp() {
        // 注入常数 supplier，避免触发 ModEquipmentConfig / ModArtilleryConfig 静态初始化
        originalSuppliers = BallisticSolver.setConfigSuppliers(
                100, 4000, 0.01, 5.0, 256, true);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (originalSuppliers != null) originalSuppliers.restore();
        BallisticSolver.clearCache();
        BallisticSolver.setCacheEnabled(true);
    }

    @Test
    void reportSolverCostDistribution() {
        warmUpJit();
        BallisticSolver.clearCache();

        double[] missNs = measureCacheMiss();
        double[] hitNs = measureCacheHit();

        printReport(missNs, hitNs);

        // 宽松上界：单次 cache-miss 解算不应超过一个 tick 预算的 5%
        double p99Ms = percentile(missNs, 99) / 1e6;
        double tickMs = 1000.0 / 20.0;
        org.junit.jupiter.api.Assertions.assertTrue(
                p99Ms < tickMs * 0.05,
                String.format("单次解算 P99 = %.3f ms，超过单 tick 预算的 5%%（%.3f ms）",
                        p99Ms, tickMs * 0.05));
    }

    /** 预热 JIT：不做这一步测到的是解释器/C1 版本，会严重高估耗时。 */
    private static void warmUpJit() {
        for (int w = 0; w < 20000; w++) {
            BallisticSolver.solve(3.0, 0.01, 9.8 / 196.0, 1.0 + (w % 100), 0.0, 0.0,
                    Math.toRadians(-5), Math.toRadians(50));
        }
    }

    /** cache-miss：每次使用全新的距离键，强制走完整解算。 */
    private static double[] measureCacheMiss() {
        double[] out = new double[N];
        for (int i = 0; i < N; i++) {
            double[] g = GUNS[i % GUNS.length];
            // 加微小偏移确保 SolutionKey 唯一
            double d = 20.0 + (i % 60) + (i * 1e-6);
            long t0 = System.nanoTime();
            BallisticSolver.solve(g[0], g[1], g[2], d, 0.0, 0.0,
                    Math.toRadians(g[3]), Math.toRadians(g[4]));
            out[i] = System.nanoTime() - t0;
        }
        return out;
    }

    /** cache-hit：同一 key 重复，模拟玩家准星稳定时的瞄准路径。 */
    private static double[] measureCacheHit() {
        double[] g = GUNS[1];
        BallisticSolver.solve(g[0], g[1], g[2], 60.0, 0.0, 0.0,
                Math.toRadians(g[3]), Math.toRadians(g[4]));
        double[] out = new double[N];
        for (int i = 0; i < N; i++) {
            long t0 = System.nanoTime();
            BallisticSolver.solve(g[0], g[1], g[2], 60.0, 0.0, 0.0,
                    Math.toRadians(g[3]), Math.toRadians(g[4]));
            out[i] = System.nanoTime() - t0;
        }
        return out;
    }

    private static void printReport(double[] missNs, double[] hitNs) {
        double tickMs = 1000.0 / 20.0;  // 一个游戏 tick 的墙钟预算
        double netNs = 704 * 2.0;       // 网络估算：704 MAC x 2ns

        System.out.println();
        System.out.println("================================================================");
        System.out.println("BallisticSolver 解算耗时分布（已预热 JIT，N=" + N + "）");
        System.out.println("================================================================");
        line("cache-MISS（冷启动一次完整解算）", missNs);
        line("cache-HIT （LRU 命中，瞄准稳态）", hitNs);

        double p50 = percentile(missNs, 50);
        double p95 = percentile(missNs, 95);
        double p99 = percentile(missNs, 99);

        System.out.println();
        System.out.printf("单个游戏 tick 预算 = %.1f ms%n", tickMs);
        System.out.printf("  cache-miss P50 占 %.4f%%%n", p50 / 1e6 / tickMs * 100);
        System.out.printf("  cache-miss P95 占 %.4f%%%n", p95 / 1e6 / tickMs * 100);
        System.out.printf("  cache-miss P99 占 %.4f%%%n", p99 / 1e6 / tickMs * 100);

        System.out.println();
        System.out.println("极端情况：一个 tick 内 20 门炮全部 cache-miss");
        System.out.printf("  合计 %.4f ms，占单 tick 预算 %.4f%%%n",
                p50 * 20 / 1e6, p50 * 20 / 1e6 / tickMs * 100);

        System.out.println();
        System.out.println("神经网络推理估算（704 MAC x 2ns，保守）");
        System.out.printf("  约 %.0f ns = %.3f us%n", netNs, netNs / 1000);
        System.out.printf("  相对 cache-miss P50 加速 %.0f 倍%n", p50 / netNs);
        System.out.printf("  相对 cache-hit  P50 加速 %.2f 倍（<1 表示网络反而更慢）%n",
                percentile(hitNs, 50) / netNs);
        System.out.println("================================================================");
        System.out.println();
    }

    private static void line(String label, double[] a) {
        System.out.printf("%s%n", label);
        System.out.printf("  P50=%9.3f us   P90=%9.3f us   P95=%9.3f us%n",
                percentile(a, 50) / 1000, percentile(a, 90) / 1000, percentile(a, 95) / 1000);
        System.out.printf("  P99=%9.3f us   max=%9.3f us   mean=%8.3f us%n",
                percentile(a, 99) / 1000, percentile(a, 100) / 1000, mean(a) / 1000);
    }

    private static double percentile(double[] a, int p) {
        double[] c = a.clone();
        Arrays.sort(c);
        int i = (int) Math.min(c.length - 1, Math.round((p / 100.0) * (c.length - 1)));
        return c[i];
    }

    private static double mean(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s / a.length;
    }
}
