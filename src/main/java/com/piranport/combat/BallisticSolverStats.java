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
    private double ternaryVerticalError = Double.MAX_VALUE;
    private double ternaryHorizontalError = Double.MAX_VALUE;
    private double newtonVerticalError = Double.MAX_VALUE;
    private double newtonHorizontalError = Double.MAX_VALUE;
    private double combinedVerticalError = Double.MAX_VALUE;
    private double combinedHorizontalError = Double.MAX_VALUE;
    private long lastTernaryNs = 0;
    private long lastNewtonNs = 0;
    private long lastTotalNs = 0;

    // ===== 最终选择的算法 =====
    private Algorithm lastChosen = Algorithm.TERNARY;

    // ===== 最近一次解算的实际迭代次数 =====
    private int lastTernaryIters = 0;
    private int lastNewtonIters = 0;

    // ===== 缓存命中率统计 =====
    // 用途：判断「神经网络替换解算器」方案的收益。该方案的加速只体现在
    // cache-miss 路径上，而稳态瞄准时缓存全命中、上网络反而是负优化
    // （实测 cache-hit P50=0.25us，网络推理约 1.4us）。因此 cache-miss
    // 的实际发生率直接决定该方案是否值得推进。
    // 依据：docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md
    private long cacheHits = 0;
    private long cacheMisses = 0;
    /** cache-miss 的累计耗时（纳秒），用于算真实平均成本 */
    private long cacheMissTotalNs = 0;
    /** cache-miss 耗时上限（最坏一次），用于观察长尾 */
    private long cacheMissMaxNs = 0;
    /** 最近一次 cache-miss 的累计耗时，用于观察连续变速时的成本 */
    private long lastCacheMissNs = 0;

    private static final BallisticSolverStats INSTANCE = new BallisticSolverStats();

    private BallisticSolverStats() {}

    public static BallisticSolverStats getInstance() { return INSTANCE; }

    // ===== 缓存命中率 =====

    /** 记录一次缓存命中（零成本路径）。 */
    public void recordCacheHit() { cacheHits++; }

    /**
     * 记录一次缓存未命中（完整解算）。
     * @param elapsedNs 本次解算耗时
     */
    public void recordCacheMiss(long elapsedNs) {
        cacheMisses++;
        cacheMissTotalNs += elapsedNs;
        if (elapsedNs > cacheMissMaxNs) cacheMissMaxNs = elapsedNs;
        lastCacheMissNs = elapsedNs;
    }

    public long getCacheHits() { return cacheHits; }
    public long getCacheMisses() { return cacheMisses; }
    public long getCacheTotalRequests() { return cacheHits + cacheMisses; }
    public long getLastCacheMissUs() { return lastCacheMissNs / 1000; }
    public long getCacheMissMaxUs() { return cacheMissMaxNs / 1000; }
    public long getCacheMissAvgUs() { return cacheMisses == 0 ? 0 : (cacheMissTotalNs / cacheMisses) / 1000; }

    /** 缓存未命中率 [0,1]。样本不足时返回 0。 */
    public double getCacheMissRate() {
        long total = getCacheTotalRequests();
        return total == 0 ? 0.0 : (double) cacheMisses / total;
    }

    /** 记录三分法计算 */
    public void recordTernary(long elapsedNs, double verticalError, double horizontalError) {
        ternaryMinNs = Math.min(ternaryMinNs, elapsedNs);
        ternaryMaxNs = Math.max(ternaryMaxNs, elapsedNs);
        ternaryTotalNs += elapsedNs;
        ternaryCount++;
        lastTernaryNs = elapsedNs;
        ternaryVerticalError = verticalError;
        ternaryHorizontalError = horizontalError;
    }

    /** 记录牛顿迭代计算 */
    public void recordNewton(long elapsedNs, double verticalError, double horizontalError) {
        newtonMinNs = Math.min(newtonMinNs, elapsedNs);
        newtonMaxNs = Math.max(newtonMaxNs, elapsedNs);
        newtonTotalNs += elapsedNs;
        newtonCount++;
        lastNewtonNs = elapsedNs;
        newtonVerticalError = verticalError;
        newtonHorizontalError = horizontalError;
    }

    /** 记录组合解算的最终精度和选择 */
    public void recordCombined(double verticalError, double horizontalError, Algorithm chosen, long totalNs) {
        combinedVerticalError = verticalError;
        combinedHorizontalError = horizontalError;
        lastChosen = chosen;
        lastTotalNs = totalNs;
    }

    // ===== Getters =====

    public long getTernaryMinUs() { return ternaryMinNs == Long.MAX_VALUE ? 0 : ternaryMinNs / 1000; }
    public long getTernaryMaxUs() { return ternaryMaxNs / 1000; }
    public long getTernaryAvgUs() { return ternaryCount == 0 ? 0 : (ternaryTotalNs / ternaryCount) / 1000; }
    public int getTernaryCount() { return ternaryCount; }
    public double getTernaryAccuracy() { return getTernaryVerticalError(); }
    public double getTernaryVerticalError() { return ternaryVerticalError; }
    public double getTernaryHorizontalError() { return ternaryHorizontalError; }
    public long getLastTernaryUs() { return lastTernaryNs / 1000; }

    public long getNewtonMinUs() { return newtonMinNs == Long.MAX_VALUE ? 0 : newtonMinNs / 1000; }
    public long getNewtonMaxUs() { return newtonMaxNs / 1000; }
    public long getNewtonAvgUs() { return newtonCount == 0 ? 0 : (newtonTotalNs / newtonCount) / 1000; }
    public int getNewtonCount() { return newtonCount; }
    public double getNewtonAccuracy() { return getNewtonVerticalError(); }
    public double getNewtonVerticalError() { return newtonVerticalError; }
    public double getNewtonHorizontalError() { return newtonHorizontalError; }
    public long getLastNewtonUs() { return lastNewtonNs / 1000; }

    public double getCombinedAccuracy() { return getCombinedTotalError(); }
    public double getCombinedVerticalError() { return combinedVerticalError; }
    public double getCombinedHorizontalError() { return combinedHorizontalError; }
    public double getCombinedTotalError() { return Math.hypot(combinedVerticalError, combinedHorizontalError); }
    public long getLastTotalUs() { return lastTotalNs / 1000; }
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
        ternaryVerticalError = Double.MAX_VALUE;
        ternaryHorizontalError = Double.MAX_VALUE;
        newtonVerticalError = Double.MAX_VALUE;
        newtonHorizontalError = Double.MAX_VALUE;
        combinedVerticalError = Double.MAX_VALUE;
        combinedHorizontalError = Double.MAX_VALUE;
        lastTernaryNs = 0;
        lastNewtonNs = 0;
        lastTotalNs = 0;
        lastChosen = Algorithm.TERNARY;
        lastTernaryIters = 0;
        lastNewtonIters = 0;
        cacheHits = 0;
        cacheMisses = 0;
        cacheMissTotalNs = 0;
        cacheMissMaxNs = 0;
        lastCacheMissNs = 0;
    }
}
