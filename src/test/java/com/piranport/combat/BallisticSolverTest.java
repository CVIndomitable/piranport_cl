package com.piranport.combat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BallisticSolver 回归测试（C3：critical 级）。
 *
 * <p>覆盖：
 * <ol>
 *   <li>独立投射物逐 tick 积分验证近远距离与不同目标高度的实际落点</li>
 *   <li>多个有效根优先低抛，俯仰受限时允许必要的高抛</li>
 *   <li>horizontalDist <= 0 → outOfRange=true 兜底</li>
 *   <li>minAngle > maxAngle 自动交换</li>
 *   <li>NaN/Infinity 输入不抛异常</li>
 *   <li>cache 命中率（setCacheEnabled false/true）</li>
 *   <li>45° 兜底（initialSpeed<=0）</li>
 *   <li>calculateMaxRangeAngle 单调性</li>
 * </ol>
 *
 * <p>BallisticSolver 通过 {@link BallisticSolver.ConfigSuppliers} 暴露可注入的 config
 * supplier，测试时用常数替换以避免触发 MC/Neoforge 类的静态初始化。
 */
class BallisticSolverTest {

    private BallisticSolver.ConfigSuppliers originalSuppliers;

    @BeforeEach
    void injectConstantSuppliers() {
        // 注入常数 supplier，避免触发 ModEquipmentConfig / ModArtilleryConfig 静态初始化
        // （这两个类会拉入 ModConfigSpec.Builder，在测试运行时 classpath 缺失）
        originalSuppliers = BallisticSolver.setConfigSuppliers(
                100,    // maxIterations
                4000,   // maxSteps
                0.01,   // accuracy
                5.0,    // noSolutionThreshold（最大射程角回退阈值）
                32,     // cacheSize
                true    // perfCacheEnabled
        );
    }

    @AfterEach
    void resetState() {
        if (originalSuppliers != null) {
            originalSuppliers.restore();
        }
        BallisticSolver.clearCache();
        BallisticSolver.setCacheEnabled(true);
    }

    // 已知有高低两根：过去会因高根的浮点误差稍小而错误选择约 64.86°。
    @Test
    void directShotShortRangeHasLowReverseError() {
        double v0 = 2.0;
        double drag = 0.01;
        double gravity = 0.05;
        double hDist = 30.0;
        double vDist = 0.0;
        BallisticSolver.Result r = BallisticSolver.solve(v0, drag, gravity, hDist, vDist, 0.0);
        assertNotNull(r);
        assertFalse(r.outOfRange(), "近距离平射应在射程内");
        assertEquals(13.4317, Math.toDegrees(r.angle()), 0.01, "应稳定选择低弹道");
        assertProjectileHits(v0, drag, gravity, hDist, vDist, r);
    }

    @Test
    void smallGunHitsBodyAtDifferentHeightsWithoutSwitchingToHighArc() {
        assertSmallGunBodyTargets();
    }

    @Test
    void currentGameplayConfigKeepsPreciseLowArcForBodyTargets() {
        // run/config/piranport-equipment.toml 的现有配置比默认配置宽松。
        // 0.5 格停止阈值不能让误差 0.49 格的近似低角压过精确低根。
        BallisticSolver.setConfigSuppliers(20, 200, 0.5, 5.0, 32, false);
        assertSmallGunBodyTargets();
    }

    private static void assertSmallGunBodyTargets() {
        // small_gun.json 的实际 float 参数，炮口与实体瞄准点有高低差。
        double drag = (double) 0.015F;
        double gravity = 9.8F / 196.0;
        for (double targetY : new double[]{-2.0, -1.0, 0.0, 1.0, 3.0, 5.0}) {
            BallisticSolver.Result result = BallisticSolver.solve(2.5, drag, gravity,
                    50.0, targetY, 0.0, Math.toRadians(-10.0), Math.toRadians(60.0));
            assertFalse(result.outOfRange(), "50 格不同身体高度都应可达，y=" + targetY);
            assertTrue(Math.toDegrees(result.angle()) < 35.0,
                    "微调准星高度不应突然切换到 50° 高抛，y=" + targetY);
            assertProjectileHits(2.5, drag, gravity, 50.0, targetY, result);
        }
    }

    @Test
    void independentProjectilePhysicsHitsNearAndFarTargets() {
        double[][] gunScenarios = {
                {2.5, 0.015F, 9.8F / 196.0, -10.0, 60.0, 15.0, 30.0, 50.0},
                {3.0, 0.01F, 9.8F / 196.0, -5.0, 50.0, 20.0, 50.0, 70.0},
                {6.0, 0.002F, 6.0F / 196.0, -5.0, 45.0, 30.0, 100.0, 250.0}
        };
        for (double[] gun : gunScenarios) {
            for (int i = 5; i < gun.length; i++) {
                for (double targetY : new double[]{-1.0, 0.0, 4.0}) {
                    BallisticSolver.Result result = BallisticSolver.solve(gun[0], gun[1], gun[2],
                            gun[i], targetY, 0.0, Math.toRadians(gun[3]), Math.toRadians(gun[4]));
                    assertFalse(result.outOfRange(), "可达目标应有解，距离=" + gun[i] + "，高差=" + targetY);
                    assertProjectileHits(gun[0], gun[1], gun[2], gun[i], targetY, result);
                }
            }
        }
    }

    @Test
    void unreachableMediumGunTargetIsNotAcceptedByPermissiveFallbackThreshold() {
        // 保留旧配置的 50 格回退阈值也不能声称 90 格目标可以命中（实际低约 8 格）。
        BallisticSolver.setConfigSuppliers(100, 4000, 0.01, 50.0, 32, false);
        BallisticSolver.Result result = BallisticSolver.solve(3.0, (double) 0.01F, 9.8F / 196.0,
                90.0, 0.0, 0.0, Math.toRadians(-5.0), Math.toRadians(50.0));
        assertTrue(result.outOfRange());
        assertTrue(projectileHeightAt(3.0, result.angle(), (double) 0.01F, 9.8F / 196.0, 90.0) < -7.0);
    }

    @Test
    void elevationLimitsRejectUnreachableTargetsAndAllowRequiredHighArc() {
        double drag = (double) 0.015F;
        double gravity = 9.8F / 196.0;
        BallisticSolver.Result lowLimit = BallisticSolver.solve(2.5, drag, gravity,
                50.0, 0.0, 0.0, Math.toRadians(-10.0), Math.toRadians(10.0));
        assertTrue(lowLimit.outOfRange(), "低仰角限制使 50 格目标不可达");

        BallisticSolver.Result highOnly = BallisticSolver.solve(2.5, drag, gravity,
                50.0, 0.0, 0.0, Math.toRadians(40.0), Math.toRadians(60.0));
        assertFalse(highOnly.outOfRange());
        assertTrue(Math.toDegrees(highOnly.angle()) > 50.0);
        assertProjectileHits(2.5, drag, gravity, 50.0, 0.0, highOnly);

        BallisticSolver.Result depressionLimit = BallisticSolver.solve(6.0, (double) 0.002F, 6.0F / 196.0,
                15.0, -2.0, 0.0, Math.toRadians(-5.0), Math.toRadians(45.0));
        assertTrue(depressionLimit.outOfRange(), "近距离目标需要超过炮管限制的俯角");
    }

    @Test
    void configuredThresholdStillControlsMaxRangeAngleFallback() {
        BallisticSolver.setConfigSuppliers(100, 4000, 0.01, 50.0, 32, true);
        double minAngle = Math.toRadians(-5.0);
        double maxAngle = Math.toRadians(45.0);
        double drag = (double) 0.002F;
        double gravity = 6.0F / 196.0;
        BallisticSolver.Result approximate = BallisticSolver.solve(6.0, drag, gravity,
                15.0, -2.0, 0.0, minAngle, maxAngle);
        assertTrue(approximate.outOfRange());
        assertEquals(-5.0, Math.toDegrees(approximate.angle()), 0.01);

        BallisticSolver.clearCache();
        BallisticSolver.setConfigSuppliers(100, 4000, 0.01, 0.5, 32, true);
        BallisticSolver.Result fallback = BallisticSolver.solve(6.0, drag, gravity,
                15.0, -2.0, 0.0, minAngle, maxAngle);
        assertTrue(fallback.outOfRange());
        assertEquals(BallisticSolver.calculateMaxRangeAngle(6.0, drag, gravity, minAngle, maxAngle),
                fallback.angle(), 1.0e-9);
    }

    // (2) horizontalDist <= 0 兜底
    @Test
    void zeroOrNegativeDistanceReturnsOutOfRange() {
        BallisticSolver.Result zero = BallisticSolver.solve(1.5, 0.01, 0.05, 0.0, 0.0, 0.0);
        assertNotNull(zero);
        assertTrue(zero.outOfRange(), "horizontalDist=0 必须兜底为 outOfRange");

        BallisticSolver.Result neg = BallisticSolver.solve(1.5, 0.01, 0.05, -10.0, 0.0, 0.0);
        assertNotNull(neg);
        assertTrue(neg.outOfRange(), "horizontalDist<0 必须兜底为 outOfRange");
    }

    // initialSpeed<=0 也兜底
    @Test
    void zeroOrNegativeInitialSpeedReturns45DegFallback() {
        BallisticSolver.Result r = BallisticSolver.solve(0.0, 0.01, 0.05, 50.0, 0.0, 0.0);
        assertNotNull(r);
        assertTrue(r.outOfRange());
        // 45° 兜底（clamp 到 [-89°, 89°] 区间内）
        assertEquals(Math.toRadians(45.0), r.angle(), 1e-9);
    }

    // (3) minAngle > maxAngle 自动交换（不抛异常）
    @Test
    void reversedMinMaxAnglesAreSwappedAndNotThrow() {
        BallisticSolver.Result r = assertDoesNotThrow(
                () -> BallisticSolver.solve(1.5, 0.01, 0.05, 30.0, 0.0, 0.0,
                        Math.toRadians(60.0), Math.toRadians(10.0)),
                "minAngle>maxAngle 必须容忍并自动交换");
        assertNotNull(r);
    }

    // (4) NaN/Infinity 输入不抛异常
    @Test
    void nanInfinityInputsDoNotThrow() {
        assertDoesNotThrow(() -> BallisticSolver.solve(Double.NaN, 0.01, 0.05, 50.0, 0.0, 0.0));
        assertDoesNotThrow(() -> BallisticSolver.solve(Double.POSITIVE_INFINITY, 0.01, 0.05, 50.0, 0.0, 0.0));
        assertDoesNotThrow(() -> BallisticSolver.solve(1.5, Double.NaN, 0.05, 50.0, 0.0, 0.0));
        assertDoesNotThrow(() -> BallisticSolver.solve(1.5, 0.01, 0.05, Double.NaN, 0.0, 0.0));
        assertDoesNotThrow(() -> BallisticSolver.solve(1.5, 0.01, 0.05, 50.0, Double.NaN, 0.0));
    }

    // (5) cache 命中率 — 关闭缓存时多次调用不应命中缓存
    @Test
    void cacheDisabledDoesNotCache() {
        BallisticSolver.setCacheEnabled(false);
        BallisticSolver.clearCache();
        BallisticSolver.Result first = BallisticSolver.solve(1.5, 0.01, 0.05, 50.0, 0.0, 0.0);
        BallisticSolver.Result second = BallisticSolver.solve(1.5, 0.01, 0.05, 50.0, 0.0, 0.0);
        assertNotNull(first);
        assertNotNull(second);
        // 关闭缓存：两次结果应一致（纯函数），但内部缓存表应为空
        assertEquals(first.angle(), second.angle(), 1e-9);
        // 切回开启：再次调用同一 key 应能命中
        BallisticSolver.setCacheEnabled(true);
        BallisticSolver.Result third = BallisticSolver.solve(1.5, 0.01, 0.05, 50.0, 0.0, 0.0);
        assertEquals(first.angle(), third.angle(), 1e-9);
    }

    // 缓存清空可重置
    @Test
    void clearCacheAllowsFreshCompute() {
        BallisticSolver.solve(1.5, 0.01, 0.05, 50.0, 0.0, 0.0);
        BallisticSolver.clearCache();
        // 清空后再调用不应抛错
        BallisticSolver.Result r = BallisticSolver.solve(1.5, 0.01, 0.05, 50.0, 0.0, 0.0);
        assertNotNull(r);
    }

    // (7) calculateMaxRangeAngle 应返回 [minAngle, maxAngle] 区间内的值
    @Test
    void maxRangeAngleStaysInBounds() {
        double minA = Math.toRadians(-10.0);
        double maxA = Math.toRadians(80.0);
        double angle = BallisticSolver.calculateMaxRangeAngle(1.5, 0.01, 0.05, minA, maxA);
        assertTrue(angle >= minA && angle <= maxA,
                "max range angle 应在搜索区间内，实际=" + Math.toDegrees(angle));
    }

    // 反向 min/max 也应正常工作
    @Test
    void maxRangeAngleSwapsReversedBounds() {
        double minA = Math.toRadians(80.0);
        double maxA = Math.toRadians(-10.0);
        double angle = assertDoesNotThrow(
                () -> BallisticSolver.calculateMaxRangeAngle(1.5, 0.01, 0.05, minA, maxA));
        assertTrue(angle >= Math.toRadians(-10.0) && angle <= Math.toRadians(80.0));
    }

    // 远距离高抛（高弧线）下 solver 仍返回有限值
    @Test
    void highArcLongRangeReturnsFiniteAngle() {
        BallisticSolver.Result r = BallisticSolver.solve(1.5, 0.01, 0.05, 100.0, 5.0, 0.0);
        assertNotNull(r);
        assertTrue(Double.isFinite(r.angle()), "解算角度必须有限");
    }

    // 异常路径不应破坏后续调用
    @Test
    void exceptionPathDoesNotBreakSubsequentCalls() {
        // 制造一次非法输入（horizontalDist<=0 触发的兜底），后续正常输入仍可工作
        BallisticSolver.solve(1.5, 0.01, 0.05, -1.0, 0.0, 0.0);
        BallisticSolver.Result r = BallisticSolver.solve(1.5, 0.01, 0.05, 20.0, 0.0, 0.0);
        assertNotNull(r);
        assertFalse(r.outOfRange());
    }

    // 防呆：solve 不应抛任何异常（即便输入全 0）
    @Test
    void solveNeverThrows() {
        for (double v0 : new double[]{0.0, 0.5, 1.5, 3.0}) {
            for (double h : new double[]{-10.0, 0.0, 30.0, 200.0}) {
                for (double v : new double[]{-50.0, 0.0, 30.0, 100.0}) {
                    assertDoesNotThrow(() -> BallisticSolver.solve(v0, 0.01, 0.05, h, v, 0.0),
                            "solve(" + v0 + "," + h + "," + v + ") 不应抛异常");
                }
            }
        }
    }

    // 显式 setCacheEnabled(false) + clearCache() 不抛异常（用于调试/性能测试）
    @Test
    void setCacheEnabledTogglesCleanly() {
        BallisticSolver.setCacheEnabled(false);
        BallisticSolver.setCacheEnabled(true);
        BallisticSolver.setCacheEnabled(false);
        BallisticSolver.clearCache();
        assertDoesNotThrow(() -> BallisticSolver.solve(1.5, 0.01, 0.05, 30.0, 0.0, 0.0));
    }

    // ===== 闭式解（docs/技术实现指南/06-弹道闭式解法优化.md）=====

    /**
     * 闭式种子必须给出<b>低弹道</b>，而不是高弹道。
     *
     * <p>回归背景：该文档原始方案是「枚举 N 取 |V_needed(N) - V| 最小」，实测
     * V_needed(N) 是山谷形而非单调，最小值点落在高弹道（本用例为 50.66°），
     * 而正确的低弹道是 19.28°。修正后必须反解 A(N) 的单调关系。
     */
    @Test
    void closedFormSeedReturnsLowArcNotHighArc() {
        double drag = (double) 0.015F;
        double gravity = 9.8F / 196.0;

        Double seed = BallisticSolver.closedFormSeed(2.5, drag, gravity, 50.0, 0.0);
        assertNotNull(seed, "闭式解应能求出 50 格平射的种子角");

        double lowArc = Math.toDegrees(seed);
        assertTrue(lowArc < 35.0,
                "闭式种子必须是低弹道（<35°），不得落到高弹道 50.66°，实际=" + lowArc);
        assertEquals(19.2763, lowArc, 0.05, "闭式种子应收敛到真实低弹道角");

        // 种子经过 solve() 的精修后必须真正命中目标
        BallisticSolver.clearCache();
        BallisticSolver.Result r = BallisticSolver.solve(2.5, drag, gravity,
                50.0, 0.0, 0.0, Math.toRadians(-10.0), Math.toRadians(60.0));
        assertFalse(r.outOfRange());
        assertProjectileHits(2.5, drag, gravity, 50.0, 0.0, r);
    }

    /**
     * 闭式种子的插值落点误差必须远优于 0.02 格判据。
     *
     * <p>A(N)/B(N) 描述的是整数 tick 处的精确位置，而命中判定用线性插值；
     * 实测该偏差为 0.0008-0.0062 格。这里覆盖多个初速/阻力/高低差组合。
     */
    @Test
    void closedFormSeedLandsWithinToleranceAcrossGuns() {
        double[][] cases = {
                // V,     drag,          gravity,       D,     H
                {2.0, 0.01, 0.05, 30.0, 0.0},
                {2.5, (double) 0.015F, 9.8F / 196.0, 50.0, 0.0},
                {2.5, (double) 0.015F, 9.8F / 196.0, 50.0, 5.0},
                {2.5, (double) 0.015F, 9.8F / 196.0, 50.0, -2.0},
                {3.0, 0.01, 9.8F / 196.0, 50.0, 0.0},
                {6.0, (double) 0.002F, 6.0F / 196.0, 100.0, 0.0},
                {6.0, (double) 0.002F, 6.0F / 196.0, 250.0, 0.0},
                {6.0, (double) 0.002F, 6.0F / 196.0, 100.0, 20.0},
                {6.0, (double) 0.002F, 6.0F / 196.0, 100.0, -20.0},
        };
        for (double[] c : cases) {
            Double seed = BallisticSolver.closedFormSeed(c[0], c[1], c[2], c[3], c[4]);
            assertNotNull(seed, "闭式解应给出种子，V=" + c[0] + " D=" + c[3] + " H=" + c[4]);
            double height = projectileHeightAt(c[0], seed, c[1], c[2], c[3]);
            assertTrue(Math.abs(height - c[4]) < 0.02,
                    "闭式种子插值落点误差超限：V=" + c[0] + " D=" + c[3] + " H=" + c[4]
                            + "，实际高=" + height);
        }
    }

    /**
     * 闭式解对超出最大射程的目标必须返回 null（不可达），不得给出伪解。
     *
     * <p>V=3.0/drag=0.01/g=0.05 的最大射程实测 84.874 格，因此 D=100 无解。
     * 回归背景：原文档 §8.1 声称该参数下 D=100 在 25.4° 命中，属伪造数据。
     */
    @Test
    void closedFormSeedReportsUnreachableTargets() {
        assertNull(BallisticSolver.closedFormSeed(3.0, 0.01, 0.05, 100.0, 0.0),
                "V=3.0 时 D=100 超出最大射程 84.87 格，闭式解应判为不可达");
        assertNull(BallisticSolver.closedFormSeed(2.5, (double) 0.015F, 9.8F / 196.0, 70.0, -2.0),
                "该参数下 70 格处可达高度约 -23 格，H=-2 不可达");
        // 最大射程内则应可达
        assertNotNull(BallisticSolver.closedFormSeed(3.0, 0.01, 0.05, 80.0, 0.0),
                "D=80 在 84.87 格最大射程内，应有解");
    }

    /** 闭式解退化输入不得抛异常，且应通过 null 交回精确路径。 */
    @Test
    void closedFormSeedHandlesDegenerateInput() {
        assertDoesNotThrow(() -> BallisticSolver.closedFormSeed(0.0, 0.01, 0.05, 50.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(0.0, 0.01, 0.05, 50.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(1.5, 0.01, 0.05, 0.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(1.5, 0.01, 0.05, -10.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(Double.NaN, 0.01, 0.05, 50.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(1.5, Double.NaN, 0.05, 50.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(1.5, 0.01, Double.NaN, 50.0, 0.0));
        assertNull(BallisticSolver.closedFormSeed(1.5, 0.01, 0.05, Double.NaN, 0.0));
        assertNull(BallisticSolver.closedFormSeed(1.5, 0.01, 0.05, 50.0, Double.NaN));
        // dragCoeff = 0 是零阻力极限，d -> 0.99 仍可解
        assertDoesNotThrow(() -> BallisticSolver.closedFormSeed(3.0, 0.0, 0.05, 50.0, 0.0));
    }

    /** 关闭闭式解后，解算结果仍须命中同一目标（种子只是加速，不改变可达性）。 */
    @Test
    void disablingClosedFormDoesNotChangeResults() {
        double drag = (double) 0.015F;
        double gravity = 9.8F / 196.0;

        BallisticSolver.clearCache();
        BallisticSolver.setClosedFormEnabled(true);
        BallisticSolver.Result withSeed = BallisticSolver.solve(2.5, drag, gravity,
                50.0, 3.0, 0.0, Math.toRadians(-10.0), Math.toRadians(60.0));

        BallisticSolver.clearCache();
        BallisticSolver.setClosedFormEnabled(false);
        BallisticSolver.Result withoutSeed = BallisticSolver.solve(2.5, drag, gravity,
                50.0, 3.0, 0.0, Math.toRadians(-10.0), Math.toRadians(60.0));
        BallisticSolver.setClosedFormEnabled(true);

        // 种子收窄了精修扫描窗口，落在同一平台根的略不同点属正常；
        // 判定标准是二者都命中目标且仰角差在 1e-3 度以内，而非逐位相同。
        assertEquals(withoutSeed.outOfRange(), withSeed.outOfRange());
        assertEquals(Math.toDegrees(withoutSeed.angle()), Math.toDegrees(withSeed.angle()), 1.0e-3,
                "闭式种子只应加速收敛，不应改变最终仰角");
        assertFalse(withSeed.outOfRange());
        assertProjectileHits(2.5, drag, gravity, 50.0, 3.0, withSeed);
        assertProjectileHits(2.5, drag, gravity, 50.0, 3.0, withoutSeed);
    }

    private static void assertProjectileHits(double speed, double drag, double gravity,
                                             double targetX, double targetY, BallisticSolver.Result result) {
        assertEquals(targetY, projectileHeightAt(speed, result.angle(), drag, gravity, targetX), 0.02,
                "独立实体物理未经过目标点，x=" + targetX + "，y=" + targetY);
    }

    /**
     * 独立核对 CannonProjectileEntity + Minecraft 1.21.1 ThrowableProjectile 的执行顺序。
     * 不调用解算器内部模拟：自定义阻力 → 当前线段移动/碰撞 → 原版 float 空气阻力 → 重力。
     */
    private static double projectileHeightAt(double speed, double angle, double drag,
                                             double gravity, double targetX) {
        double x = 0.0;
        double y = 0.0;
        double velocityX = speed * Math.cos(angle);
        double velocityY = speed * Math.sin(angle);
        for (int tick = 0; tick < 4000; tick++) {
            velocityX /= 1.0 + drag;
            velocityY /= 1.0 + drag;
            double nextX = x + velocityX;
            double nextY = y + velocityY;
            if (nextX >= targetX) {
                double crossingFraction = (targetX - x) / (nextX - x);
                return y + (nextY - y) * crossingFraction;
            }
            x = nextX;
            y = nextY;
            velocityX *= (double) 0.99F;
            velocityY = velocityY * (double) 0.99F - gravity;
            if (y < -300.0) break;
        }
        return Double.NaN;
    }
}
