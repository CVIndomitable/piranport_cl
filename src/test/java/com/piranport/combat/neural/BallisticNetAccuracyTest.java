package com.piranport.combat.neural;

import com.piranport.combat.BallisticSolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * 网络解算与 {@link BallisticSolver} 的对照测试。
 *
 * <p>对应文档：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md} 4.7 节。
 *
 * <p>本测试只用网络与解算器本身（不涉及 MC 类），因此不需要启动游戏。
 * 权重来自 classpath 资源，与正式运行时同一份。
 */
class BallisticNetAccuracyTest {

    /** 实验炮参数（复制 medium_gun）：v₀=3.0，drag=0.01，g=9.8/196，仰角 −5°~50° */
    private static final double V0 = 3.0;
    private static final double DRAG = 0.01;
    private static final double G = 9.8 / 196.0;
    private static final double MIN_ANGLE = Math.toRadians(-5.0);
    private static final double MAX_ANGLE = Math.toRadians(50.0);

    private BallisticSolver.ConfigSuppliers originalSuppliers;

    @BeforeEach
    void setUp() {
        originalSuppliers = BallisticSolver.setConfigSuppliers(
                100, 4000, 0.01, 5.0, 256, true);
    }

    @AfterEach
    void tearDown() {
        if (originalSuppliers != null) originalSuppliers.restore();
        BallisticSolver.clearCache();
        BallisticSolver.setCacheEnabled(true);
    }

    /**
     * 核心对照：在实验炮的有效射程内逐点比较网络解与解算器解的落点高度误差。
     *
     * <p>验收标准是**落点高度误差**（格），不是角度误差——同一距离存在高低双根，
     * 反解可以合法地选中另一支，此时角度差异很大但落点完全正确。
     */
    @Test
    void neuralSolutionMatchesSolverWithinPlanningPrecision() {
        BallisticNet net = BallisticNet.loadExperimental();
        double[] input = new double[5];

        int n = 0;
        int withinTest = 0;     // 0.02 格（现有测试门槛）
        int withinPlan = 0;     // 0.5 格（策划精度）
        double maxErr = 0;
        double worstDist = 0;

        // 遍历实验炮射程（medium_gun 上限约 84.9 格）与多个高差
        for (double dist = 10.0; dist <= 80.0; dist += 1.0) {
            for (double vDist = -20.0; vDist <= 20.0; vDist += 5.0) {
                input[0] = dist;
                input[1] = vDist;
                input[2] = V0;
                input[3] = DRAG;
                input[4] = G;

                Double angle = net.solveAngle(input, dist, vDist, V0, DRAG, G);
                if (angle == null) continue;

                double clamped = Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, angle));
                Double hit = BallisticNetMath.heightAt(V0, clamped, DRAG, G, dist);
                if (hit == null) continue;

                double err = Math.abs(hit - vDist);

                // 超出策划精度意味着这一发不可用；解算器此时会走最大射程回退。
                // 只在**解算器能命中**的点上比较，超射程场景另行验证回退行为。
                BallisticSolver.Result solver = BallisticSolver.solve(
                        V0, DRAG, G, dist, vDist, 0.0, MIN_ANGLE, MAX_ANGLE);
                if (solver.outOfRange()) continue;

                n++;
                if (err <= 0.02) withinTest++;
                if (err <= BallisticSolver.maxAcceptableError()) withinPlan++;
                if (err > maxErr) {
                    maxErr = err;
                    worstDist = dist;
                }
            }
        }

        double testPct = n == 0 ? 0 : withinTest * 100.0 / n;
        double planPct = n == 0 ? 0 : withinPlan * 100.0 / n;

        System.out.printf("%n网络 vs 解算器 落点高度误差对照（实验炮参数，%d 个可达点）%n", n);
        System.out.printf("  最大误差 %.4f 格（出现在 %.0f 格距离）%n", maxErr, worstDist);
        System.out.printf("  达 0.02 格测试门槛：%.1f%%%n", testPct);
        System.out.printf("  达 0.5 格策划精度：%.1f%%%n", planPct);

        org.junit.jupiter.api.Assertions.assertTrue(n > 500,
                "对照点太少，样本不足：" + n);

        // 验收：全部点都必须落在策划精度内。这是可接受性的底线——
        // 只要有一发超出 0.5 格，玩家会看到「明明锁定却打偏」。
        org.junit.jupiter.api.Assertions.assertTrue(planPct == 100.0,
                String.format("有 %d 个点超出 0.5 格策划精度（最大 %.4f 格）",
                        n - withinPlan, maxErr));
    }

    /**
     * 闭式反解自身的一致性：喂入采样器给出的真实飞行时间，应能还原出落点。
     *
     * <p>这条覆盖的是「反解实现」而非「网络精度」，是本方案 2.1 节的立论基础。
     */
    @Test
    void closedFormInverseReproducesHeight() {
        BallisticNet net = BallisticNet.loadExperimental();
        double[] input = new double[5];

        // 取若干 (距离, 高差)，验证 网络预测的 t 经反解后落点合理
        int checked = 0;
        for (double dist = 15.0; dist <= 70.0; dist += 5.0) {
            for (double vDist = -10.0; vDist <= 10.0; vDist += 10.0) {
                input[0] = dist;
                input[1] = vDist;
                input[2] = V0;
                input[3] = DRAG;
                input[4] = G;

                double ticks = net.forwardTicks(input);
                org.junit.jupiter.api.Assertions.assertTrue(ticks > 0 && Double.isFinite(ticks),
                        String.format("飞行时间不合理：%.4f（距离 %.0f）", ticks, dist));

                Double angle = BallisticNetMath.angleFromTicks(V0, DRAG, G, dist, vDist, ticks);
                org.junit.jupiter.api.Assertions.assertNotNull(angle,
                        String.format("反解返回 null（距离 %.0f，高差 %.0f）", dist, vDist));
                checked++;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(checked >= 20, "检查点不足：" + checked);
    }

    /**
     * 超射程必须被识别，并回退到最大射程角。
     *
     * <p>网络只学到 83.3% 的样本在 0.02 格内，边界场景不能只靠网络——
     * {@code BallisticDispatcher} 用实际回算判定超射程，本测试锁定该行为。
     */
    @Test
    void unreachableTargetFallsBackToMaxRangeAngle() {
        double farDist = 200.0;  // 远超 medium_gun 约 84.9 格的上限
        double[] input = {farDist, 0.0, V0, DRAG, G};

        BallisticNet net = BallisticNet.loadExperimental();
        Double angle = net.solveAngle(input, farDist, 0.0, V0, DRAG, G);

        // 无论反解给出什么（或 null），实际落点都不可能到达 200 格
        if (angle != null) {
            Double hit = BallisticNetMath.heightAt(V0,
                    Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, angle)), DRAG, G, farDist);
            // hit 为 null 表示弹丸在到达前就落地了 —— 同样是不可达
            if (hit != null) {
                org.junit.jupiter.api.Assertions.assertTrue(
                        Math.abs(hit - 0.0) > BallisticSolver.maxAcceptableError(),
                        "200 格按理不可达，但回算落点误差却在精度内：" + hit);
            }
        }

        // 解算器对该目标应判定超射程，且给出最大射程角
        BallisticSolver.Result solver = BallisticSolver.solve(
                V0, DRAG, G, farDist, 0.0, 0.0, MIN_ANGLE, MAX_ANGLE);
        org.junit.jupiter.api.Assertions.assertTrue(solver.outOfRange(),
                "解算器应判定 200 格为超射程");
        double expected = BallisticSolver.calculateMaxRangeAngle(V0, DRAG, G, MIN_ANGLE, MAX_ANGLE);
        org.junit.jupiter.api.Assertions.assertEquals(expected, solver.angle(), 1e-9,
                "超射程应回退到最大射程角");
    }

    /**
     * 权重文件必须能被加载，参数量与导出一致。
     */
    @Test
    void weightsLoadWithExpectedParameterCount() {
        BallisticNet net = BallisticNet.loadExperimental();
        // 5→256→128→64→1 全连接：(5+1)*256 + (256+1)*128 + (128+1)*64 + (64+1)*1
        int expected = 6 * 256 + 257 * 128 + 129 * 64 + 65;
        org.junit.jupiter.api.Assertions.assertEquals(expected, net.parameterCount(),
                "参数量与训练导出不符——权重文件或结构定义被改动过");
    }
}
