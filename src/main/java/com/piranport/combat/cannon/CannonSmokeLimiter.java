package com.piranport.combat.cannon;

import java.util.Map;
import java.util.WeakHashMap;

/** 短暂记录各门炮的出烟时间，不延长武器实例的生命周期。 */
final class CannonSmokeLimiter<K> {
    private final int intervalTicks;
    private final Map<K, Long> lastEmission = new WeakHashMap<>();

    CannonSmokeLimiter(int intervalTicks) {
        if (intervalTicks <= 0) throw new IllegalArgumentException("intervalTicks must be positive");
        this.intervalTicks = intervalTicks;
    }

    boolean shouldEmit(K weapon, long tick) {
        Long lastTick = lastEmission.get(weapon);
        if (lastTick != null && tick >= lastTick && tick - lastTick < intervalTicks) return false;
        lastEmission.put(weapon, tick);
        return true;
    }
}
