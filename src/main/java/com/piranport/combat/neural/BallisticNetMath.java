package com.piranport.combat.neural;

/**
 * 神经网络弹道解算所需的闭式数学。
 *
 * <p>对应文档：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md} 2.1 节。
 *
 * <p>推导严格按 {@code CannonProjectileEntity.tick()} 的执行顺序：
 * <b>自定义阻力 → 位置更新 → 原版空气阻力 → 重力</b>。
 * 位置更新用的是「已乘阻力、未乘 0.99」的速度，所以两个缩放的指数相差 1。
 *
 * <pre>
 * A = 1/(1+drag)         自定义线性阻力对速度的缩放
 * R = 0.99               原版空气阻力（float 0.99F），直接用，不是它的平方根
 * D = A·R                每 tick 速度的净保留比
 * S(n) = Σ_{j=0..n−1} D^j
 *
 * 水平：x(n)  = vx₀ · A · S(n)
 * 竖直：Δy(n) = A · [ vy₀·S(n) − g·(S(n)−n)/(D−1) ]
 * </pre>
 *
 * <p><b>踩坑记录</b>：早期 Python 实现把 R 写成 {@code √0.99}，闭式反解用自造用例
 * 能验到 1e-15 看着自洽，但跑真实采样数据时角度误差随 flightTicks 线性增长
 * （t&lt;10 时 2.3e-2°，t&gt;80 时 3.7°）。自造用例只能验证推导的形式，
 * 不能替代对真实数据的回归。这里 R 必须与 {@code BallisticSolver.VANILLA_AIR_DRAG} 一致。
 */
public final class BallisticNetMath {

    private BallisticNetMath() {}

    /** 与 {@code BallisticSolver.VANILLA_AIR_DRAG} 保持一致：直接是 0.99，不是它的平方根。 */
    private static final double VANILLA_AIR_DRAG = 0.99;

    /**
     * Σ_{k=0..n−1} a^k，对 a→1 数值稳定。
     *
     * <p>直接用等比数列求和公式在 a 接近 1 时会因浮点相消丢精度，故单独退化处理。
     */
    private static double sumGeom(double a, int n) {
        if (n <= 0) return 0.0;
        if (Math.abs(a - 1.0) < 1e-12) return n;
        return (Math.pow(a, n) - 1.0) / (a - 1.0);
    }

    /**
     * 由飞行时间闭式反解发射仰角。
     *
     * <p>flightTicks 含小数：跨越点落在某 tick 内部，采样器按水平比例线性插值。
     * 因此拆成整数部分 n 与小数部分 f，小数部分用第 n+1 tick 的位移增量线性加权。
     *
     * @return 仰角（弧度），与 {@code Math.atan2(vy0, vx0)} 一致；参数非法时返回 {@code null}
     */
    public static Double angleFromTicks(double initialSpeed, double dragCoeff, double gravity,
                                        double hDist, double vDist, double ticks) {
        if (ticks <= 0.0 || dragCoeff < 0.0 || hDist <= 0.0) return null;

        double a = 1.0 / (1.0 + Math.max(0.0, dragCoeff));
        double d = a * VANILLA_AIR_DRAG;

        int n = (int) Math.floor(ticks);
        double f = ticks - n;

        double sN = sumGeom(d, n);
        double geomRatio = Math.pow(d, n);

        // ---- 水平：x(n+f) = vx₀ · A · [ S(n) + f · D^n ] ----
        double gCoef = a * (sN + f * geomRatio);
        if (Math.abs(gCoef) < 1e-300) return null;
        double vx0 = hDist / gCoef;

        // ---- 竖直 ----
        // Δy(n) = A·[ vy₀·S(n) − g·(S(n)−n)/(D−1) ]
        // 第 n+1 tick 的位移增量 = A · vy_n，其中 vy_n = vy₀·D^n − g·S(n)
        double inner;
        if (Math.abs(d - 1.0) < 1e-12) {
            // D→1 退化：S(k)=k，Σ_{k=0..n−1} S(k) = n(n−1)/2
            inner = n * (n - 1) / 2.0;
        } else {
            inner = (sN - n) / (d - 1.0);
        }

        // 关于 vy₀ 线性： Δy(n) = aCoef·vy₀ + bConst
        double aCoef = a * (sN + f * geomRatio);          // 与水平 gCoef 同形
        double bConst = a * (-gravity * inner) + f * a * (-gravity * sN);

        if (Math.abs(aCoef) < 1e-300) return null;
        double vy0 = (vDist - bConst) / aCoef;

        return Math.atan2(vy0, vx0);
    }

    /**
     * 物理复刻：返回弹丸跨越 {@code targetX} 时的高度。
     *
     * <p>与 {@code BallisticSolver.simulate} 一一对应（已交叉验证到 1e-14），
     * 用于把网络的飞行时间误差换算成落点高度误差——这才是真正的验收标准。
     *
     * @return 跨越高度的线性插值；未在 {@code maxSteps} 内跨越或落地过低时返回 {@code null}
     */
    public static Double heightAt(double initialSpeed, double angle, double dragCoeff,
                                  double gravity, double targetX) {
        return heightAt(initialSpeed, angle, dragCoeff, gravity, targetX, 8000);
    }

    public static Double heightAt(double initialSpeed, double angle, double dragCoeff,
                                  double gravity, double targetX, int maxSteps) {
        double vx = initialSpeed * Math.cos(angle);
        double vy = initialSpeed * Math.sin(angle);
        double x = 0.0;
        double y = 0.0;

        for (int step = 0; step < maxSteps; step++) {
            double prevX = x;
            double prevY = y;

            // 1. 自定义阻力（与 CannonProjectileEntity.tick() 一致）
            if (dragCoeff > 0) {
                double s = 1.0 / (1.0 + dragCoeff);
                vx *= s;
                vy *= s;
            }

            // 2. 位置更新
            x += vx;
            y += vy;

            // 3. 跨越目标水平距离：线性插值，避免整 tick 量化误差
            if (x >= targetX) {
                double dx = x - prevX;
                if (Math.abs(dx) < 1e-9) return y;
                return prevY + (y - prevY) * (targetX - prevX) / dx;
            }

            // 4. 原版空气阻力 + 重力
            vx *= VANILLA_AIR_DRAG;
            vy = vy * VANILLA_AIR_DRAG - gravity;

            if (y < -300.0) return null;
        }
        return null;
    }
}
