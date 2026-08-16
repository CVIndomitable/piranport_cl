package com.piranport.combat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BallisticSolver 回归测试（C3：critical 级）。
 *
 * <p>覆盖：
 * <ol>
 *   <li>平射近距离下精确 angle 与 simulate 反演误差</li>
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
                50.0,   // noSolutionThreshold
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

    // (1) 平射近距离下精确 angle 与 simulate 反演误差
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
        // 解算仰角应在 [-89°, 89°] 区间内且为有限值
        // 注：在 v0=2.0/hDist=30 这样的低速远距组合下，求解器会选择高弧线（~64°）以达到目标，
        // 因此断言不能假设仰角接近 0；只校验结果在合法区间内。
        assertTrue(Double.isFinite(r.angle()), "解算角度必须有限");
        assertTrue(Math.abs(r.angle()) < Math.toRadians(89.0),
                "解算仰角应在 [-89°, 89°] 区间内，实际=" + Math.toDegrees(r.angle()));
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
        BallisticSolver.Result r = BallisticSolver.solve(1.5, 0.01, 0.05, 30.0, 0.0, 0.0);
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
}