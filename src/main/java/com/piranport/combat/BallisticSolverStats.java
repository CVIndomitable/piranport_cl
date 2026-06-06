package com.piranport.combat;

/**
 * 弹道解算器性能统计。
 * 记录每种算法的计算时间（最快/最慢/平均）和计算精度。
 */
public final class BallisticSolverStats {

    /** 算法类型 */
    public enum Algorithm {
        TERNARY("三分法"),
        NEWTON("牛顿迭代"),
        COMBINED("组合");

        private final String displayName;
        Algorithm(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ===== 计算时间统计（纳秒） =====
    private long ternaryMinNs = Long.MAX_VALUE;
    private long ternaryMaxNs = 0;
    private long ternaryTotalNs = 0;
    private int ternaryCount = 0;

    private long newtonMinNs = Long.MAX_VALUE;
    private long newtonMaxNs = 0;
    private long newtonTotalNs = 0;
    private int newtonCount = 0;

    // ===== 计算精度（误差，格） =====
    private double ternaryAccuracy = Double.MAX_VALUE;
    private double newtonAccuracy = Double.MAX_VALUE;
    private double combinedAccuracy = Double.MAX_VALUE;

    // ===== 最终选择的算法 =====
    private Algorithm lastChosen = Algorithm.TERNARY;

    // ===== 最近一次解算的实际迭代次数 =====
    private int lastTernaryIters = 0;
    private int lastNewtonIters = 0;

    private static final BallisticSolverStats INSTANCE = new BallisticSolverStats();

    private BallisticSolverStats() {}

    public static BallisticSolverStats getInstance() { return INSTANCE; }

    /** 记录三分法计算 */
    public void recordTernary(long elapsedNs, double accuracy) {
        ternaryMinNs = Math.min(ternaryMinNs, elapsedNs);
        ternaryMaxNs = Math.max(ternaryMaxNs, elapsedNs);
        ternaryTotalNs += elapsedNs;
        ternaryCount++;
        ternaryAccuracy = accuracy;
    }

    /** 记录牛顿迭代计算 */
    public void recordNewton(long elapsedNs, double accuracy) {
        newtonMinNs = Math.min(newtonMinNs, elapsedNs);
        newtonMaxNs = Math.max(newtonMaxNs, elapsedNs);
        newtonTotalNs += elapsedNs;
        newtonCount++;
        newtonAccuracy = accuracy;
    }

    /** 记录组合解算的最终精度和选择 */
    public void recordCombined(double accuracy, Algorithm chosen) {
        combinedAccuracy = accuracy;
        lastChosen = chosen;
    }

    // ===== Getters =====

    public long getTernaryMinUs() { return ternaryMinNs == Long.MAX_VALUE ? 0 : ternaryMinNs / 1000; }
    public long getTernaryMaxUs() { return ternaryMaxNs / 1000; }
    public long getTernaryAvgUs() { return ternaryCount == 0 ? 0 : (ternaryTotalNs / ternaryCount) / 1000; }
    public int getTernaryCount() { return ternaryCount; }
    public double getTernaryAccuracy() { return ternaryAccuracy; }

    public long getNewtonMinUs() { return newtonMinNs == Long.MAX_VALUE ? 0 : newtonMinNs / 1000; }
    public long getNewtonMaxUs() { return newtonMaxNs / 1000; }
    public long getNewtonAvgUs() { return newtonCount == 0 ? 0 : (newtonTotalNs / newtonCount) / 1000; }
    public int getNewtonCount() { return newtonCount; }
    public double getNewtonAccuracy() { return newtonAccuracy; }

    public double getCombinedAccuracy() { return combinedAccuracy; }
    public Algorithm getLastChosen() { return lastChosen; }

    // ===== 迭代次数 =====
    public void setLastTernaryIters(int iters) { lastTernaryIters = iters; }
    public void setLastNewtonIters(int iters) { lastNewtonIters = iters; }
    public int getLastTernaryIters() { return lastTernaryIters; }
    public int getLastNewtonIters() { return lastNewtonIters; }

    /** 重置统计（用于新一轮测量） */
    public void reset() {
        ternaryMinNs = Long.MAX_VALUE;
        ternaryMaxNs = 0;
        ternaryTotalNs = 0;
        ternaryCount = 0;
        newtonMinNs = Long.MAX_VALUE;
        newtonMaxNs = 0;
        newtonTotalNs = 0;
        newtonCount = 0;
        ternaryAccuracy = Double.MAX_VALUE;
        newtonAccuracy = Double.MAX_VALUE;
        combinedAccuracy = Double.MAX_VALUE;
        lastChosen = Algorithm.TERNARY;
        lastTernaryIters = 0;
        lastNewtonIters = 0;
    }
}
