package com.piranport.combat.cannon.ammo;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable behavior strategy registry for ordinary cannon projectiles. */
public final class AmmoBehaviorRegistry {
    private static final Map<AmmoBehavior, AmmoBehaviorStrategy> BUILT_INS = createBuiltIns();
    private static volatile Map<AmmoBehavior, AmmoBehaviorStrategy> strategies = BUILT_INS;

    private AmmoBehaviorRegistry() {}

    public static AmmoBehaviorStrategy forBehavior(AmmoBehavior behavior) {
        Objects.requireNonNull(behavior, "behavior");
        return strategies.getOrDefault(behavior, strategies.get(AmmoBehavior.AP));
    }

    public static Map<AmmoBehavior, AmmoBehaviorStrategy> snapshot() {
        return strategies;
    }

    public static synchronized void register(AmmoBehaviorStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(strategy.behavior(), "strategy.behavior");
        EnumMap<AmmoBehavior, AmmoBehaviorStrategy> next = new EnumMap<>(AmmoBehavior.class);
        next.putAll(strategies);
        next.put(strategy.behavior(), strategy);
        strategies = Map.copyOf(next);
    }

    public static synchronized void resetToBuiltIns() {
        strategies = BUILT_INS;
    }

    private static Map<AmmoBehavior, AmmoBehaviorStrategy> createBuiltIns() {
        EnumMap<AmmoBehavior, AmmoBehaviorStrategy> map = new EnumMap<>(AmmoBehavior.class);
        map.put(AmmoBehavior.HE, simple(AmmoBehavior.HE, true, false, true,
                AmmoBehaviorStrategy.ImpactKind.HE));
        map.put(AmmoBehavior.AP, simple(AmmoBehavior.AP, false, false, false,
                AmmoBehaviorStrategy.ImpactKind.AP));
        map.put(AmmoBehavior.VT, simple(AmmoBehavior.VT, true, true, false,
                AmmoBehaviorStrategy.ImpactKind.VT));
        map.put(AmmoBehavior.TYPE3, simple(AmmoBehavior.TYPE3, true, false, false,
                AmmoBehaviorStrategy.ImpactKind.HE));
        map.put(AmmoBehavior.MK23, simple(AmmoBehavior.MK23, true, false, true,
                AmmoBehaviorStrategy.ImpactKind.HE));
        return Map.copyOf(map);
    }

    private static AmmoBehaviorStrategy simple(AmmoBehavior behavior, boolean highExplosive,
                                                boolean proximityFuse, boolean underwater,
                                                AmmoBehaviorStrategy.ImpactKind impactKind) {
        return new AmmoBehaviorStrategy() {
            @Override public AmmoBehavior behavior() { return behavior; }
            @Override public boolean highExplosive() { return highExplosive; }
            @Override public boolean proximityFuse() { return proximityFuse; }
            @Override public boolean explodesUnderwater() { return underwater; }
            @Override public ImpactKind impactKind() { return impactKind; }
        };
    }
}
