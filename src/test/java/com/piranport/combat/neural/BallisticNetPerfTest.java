package com.piranport.combat.neural;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * 神经网络推理耗时实测。
 *
 * <p>对应文档：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md} 4.5 节。
 *
 * <p><b>本测试要回答的问题</b>：精度对照实验证明达 0.02 格门槛需要 42753 参数
 * （342 KB），而 2.2 节「0.5~1.5 μs」的估算建立在 5.6 KB 常驻 L1 的前提上。
 * 大网络还剩下多少性能收益？只能实测。
 *
 * <p>对照基线（来自 {@code BallisticSolverPerfTest}，同一台机器）：
 * <ul>
 *   <li>cache-miss P50 60.2 μs / P95 733.8 μs</li>
 *   <li>cache-hit P50 0.25 μs</li>
 * </ul>
 *
 * <p>不断言性能绝对值（避免 CI 机器差异导致误报），只输出分布并做宽松上界断言。
 */
class BallisticNetPerfTest {

    /** 一个瞄准 tick 的墙钟预算 */
    private static final double TICK_MS = 1000.0 / 20.0;

    private static final int WARMUP = 20000;
    private static final int N = 20000;

    /**
     * 输入分布取自真实采样数据的典型范围：
     * 射程 20~90 格，高差 ±40，v0 2.5~3.5，drag 0.008~0.015，g 归一化 0.05。
     */
    private static double[] sampleInput(Random rnd, double[] out) {
        out[0] = 20.0 + rnd.nextDouble() * 70.0;          // hDist
        out[1] = -40.0 + rnd.nextDouble() * 80.0;         // vDist
        out[2] = 2.5 + rnd.nextDouble() * 1.0;            // initialSpeed
        out[3] = 0.008 + rnd.nextDouble() * 0.007;        // dragCoeff
        out[4] = 9.8 / 196.0;                             // gravity（已归一化）
        return out;
    }

    @Test
    void reportInferenceCost() {
        BallisticNet net = BallisticNet.loadExperimental();
        System.out.printf("%n网络参数量：%d（double 存储约 %.0f KB）%n",
                net.parameterCount(), net.parameterCount() * 8.0 / 1024);

        // ===== 1. 纯前向传播 =====
        double[] in = new double[5];
        Random rnd = new Random(42);
        for (int i = 0; i < WARMUP; i++) {
            net.forwardTicks(sampleInput(rnd, in));
        }

        double[] nanos = new double[N];
        for (int i = 0; i < N; i++) {
            sampleInput(rnd, in);
            long t0 = System.nanoTime();
            net.forwardTicks(in);
            nanos[i] = System.nanoTime() - t0;
        }
        printDistribution("① 纯前向传播（network.forwardTicks）", nanos);

        // ===== 2. 前向 + 闭式反解（完整解算路径）=====
        BallisticNet net2 = BallisticNet.loadExperimental();
        for (int i = 0; i < WARMUP; i++) {
            sampleInput(rnd, in);
            net2.solveAngle(in, in[0], in[1], in[2], in[3], in[4]);
        }
        double[] nanos2 = new double[N];
        int nullCount = 0;
        for (int i = 0; i < N; i++) {
            sampleInput(rnd, in);
            long t0 = System.nanoTime();
            Double a = net2.solveAngle(in, in[0], in[1], in[2], in[3], in[4]);
            nanos2[i] = System.nanoTime() - t0;
            if (a == null) nullCount++;
        }
        printDistribution("② 完整解算（前向 + 闭式反解）", nanos2);
        System.out.printf("   反解返回 null 的比例：%.2f%%%n", nullCount * 100.0 / N);

        // ===== 3. 权重加载成本（启动期一次性）=====
        long t0 = System.nanoTime();
        BallisticNet loaded = BallisticNet.loadExperimental();
        long loadNs = System.nanoTime() - t0;
        System.out.printf("%n③ 权重加载（启动期一次）：%.2f ms，校验参数 %d%n",
                loadNs / 1e6, loaded.parameterCount());

        // ===== 4. 对照 =====
        double p50Forward = percentile(nanos, 50) / 1000.0;
        double p50Full = percentile(nanos2, 50) / 1000.0;
        System.out.println();
        System.out.println("================ 对照（文档 4.5 结论）================");
        System.out.printf("  cache-miss P50 = 60.2 us  →  大网络相对快 %.1f 倍%n", 60.2 / p50Full);
        System.out.printf("  cache-hit  P50 =  0.25 us →  大网络相对慢 %.1f 倍%n", p50Full / 0.25);
        System.out.println("  （<1 表示网络反而更慢，稳态瞄准下是负优化）");
        System.out.println("====================================================");
        System.out.println();

        // 宽松上界：完整解算不应超过一个 tick 预算的 5%
        double p99Ms = percentile(nanos2, 99) / 1e6;
        org.junit.jupiter.api.Assertions.assertTrue(
                p99Ms < TICK_MS * 0.05,
                String.format("网络解算 P99 = %.4f ms，超过单 tick 预算的 5%%（%.3f ms）",
                        p99Ms, TICK_MS * 0.05));
    }

    private static void printDistribution(String label, double[] a) {
        System.out.println();
        System.out.println("─────────────────────────────────────────────");
        System.out.println(label + "（已预热 JIT，N=" + a.length + "）");
        System.out.println("─────────────────────────────────────────────");
        System.out.printf("  P50 =%10.3f us   P90 =%10.3f us%n",
                percentile(a, 50) / 1000, percentile(a, 90) / 1000);
        System.out.printf("  P95 =%10.3f us   P99 =%10.3f us%n",
                percentile(a, 95) / 1000, percentile(a, 99) / 1000);
        System.out.printf("  max =%10.3f us   mean=%10.3f us%n",
                percentile(a, 100) / 1000, mean(a) / 1000);
        System.out.printf("  占单 tick 预算：P50 %.4f%%  P99 %.4f%%%n",
                percentile(a, 50) / 1e6 / TICK_MS * 100,
                percentile(a, 99) / 1e6 / TICK_MS * 100);
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
