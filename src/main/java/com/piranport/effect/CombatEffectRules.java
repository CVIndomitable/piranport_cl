package com.piranport.effect;

/** 策划定稿的效果等级上限；命令施加超等级效果也不能越过数值边界。 */
public final class CombatEffectRules {
    private CombatEffectRules() {}

    public static float evasionChance(int amplifier) {
        return (Math.max(0, Math.min(amplifier, 2)) + 1) * 0.10f;
    }

    public static double experienceMultiplier(int amplifier) {
        return 1.2 + Math.max(0, Math.min(amplifier, 2)) * 0.2;
    }

    public static double reloadMultiplier(int amplifier) {
        return 0.9 - Math.max(0, Math.min(amplifier, 2)) * 0.1;
    }

    public static int burningInterval(int amplifier) {
        return 60 / (Math.max(0, Math.min(amplifier, 3)) + 1);
    }
}
