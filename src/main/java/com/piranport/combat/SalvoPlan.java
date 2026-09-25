package com.piranport.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable timing plan for the remaining guns in one salvo.
 * Delays are measured from the tick at which the plan is created.
 */
public record SalvoPlan(List<Integer> delays) {
    public SalvoPlan {
        if (delays == null || delays.stream().anyMatch(delay -> delay < 0)) {
            throw new IllegalArgumentException("salvo delays must be non-negative");
        }
        delays = Collections.unmodifiableList(new ArrayList<>(delays));
    }

    /** Creates one delay per remaining gun. Zero means same tick as the first shot. */
    public static SalvoPlan remaining(int count, float intervalTicks) {
        if (count < 0 || !Float.isFinite(intervalTicks) || intervalTicks < 0) {
            throw new IllegalArgumentException("invalid salvo plan");
        }
        // Minecraft advances in whole ticks. Never collapse a positive configured
        // interval into same-tick fire because it is below one tick.
        int interval = intervalTicks == 0.0f
                ? 0
                : Math.max(1, (int) Math.ceil(intervalTicks));
        List<Integer> delays = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // The first gun was fired before this plan was submitted. Positive intervals
            // therefore start after one interval; zero keeps every remaining gun on this tick.
            delays.add(interval == 0 ? 0 : interval * (i + 1));
        }
        return new SalvoPlan(delays);
    }
}
