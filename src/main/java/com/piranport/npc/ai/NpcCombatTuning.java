package com.piranport.npc.ai;

import com.piranport.config.ModProjectilesConfig;

/**
 * NPC 舰船「炮击 / 雷击」手感数值的唯一来源。
 *
 * <p>WHY 需要这个类：{@code TorpedoAttackGoal} 与 {@code ShipGirlCombatGoal}
 * 原先各自逐字复制了同一组常量（射程 20 / 冷却 200 / 3 发 / 散布 8° / 初速 0.8），
 * {@code CannonAttackGoal} 与 {@code ShipGirlCombatGoal} 又各自写了一份炮弹速度 1.5 / 散布 2.0；
 * 两处一旦漂移就无法解释「同一种敌人为什么手感不同」。现统一从调试终端读取，
 * 策划改一处两端同时生效。
 *
 * <p>键：{@code global.torpedo_salvo.*} 与 {@code global.shell.*}（见 {@link ModProjectilesConfig}）。
 */
public final class NpcCombatTuning {

    private NpcCombatTuning() {}

    // ===== 鱼雷齐射 =====

    /** 齐射触发距离（格）。默认 20.0。 */
    public static double torpedoRange() {
        return ModProjectilesConfig.NPC_TORPEDO_SALVO_RANGE.get();
    }

    /** 齐射冷却（ticks）。默认 200。 */
    public static int salvoCooldown() {
        return ModProjectilesConfig.NPC_TORPEDO_SALVO_COOLDOWN.get();
    }

    /** 每次齐射的鱼雷枚数。默认 3。 */
    public static int torpedoesPerSalvo() {
        return ModProjectilesConfig.NPC_TORPEDO_SALVO_COUNT.get();
    }

    /** 每枚鱼雷的扇面偏角（度）。默认 8.0。 */
    public static double spreadAngle() {
        return ModProjectilesConfig.NPC_TORPEDO_SALVO_SPREAD.get();
    }

    /** 齐射鱼雷初速（格/tick）。默认 0.8。 */
    public static float torpedoSalvoSpeed() {
        return (float) (double) ModProjectilesConfig.NPC_TORPEDO_SALVO_SPEED.get();
    }

    /** 舰娘随从每次炮击后触发雷击的判定分母（1/N 概率）。默认 200。 */
    public static int torpedoTriggerRollBound() {
        return ModProjectilesConfig.NPC_TORPEDO_TRIGGER_ROLL_BOUND.get();
    }

    // ===== 炮弹 =====

    /** 炮弹初速（格/tick）。默认 1.5。 */
    public static float shellSpeed() {
        return (float) (double) ModProjectilesConfig.NPC_SHELL_SPEED.get();
    }

    /** 炮弹散布（度）。默认 2.0。 */
    public static float shellInaccuracy() {
        return (float) (double) ModProjectilesConfig.NPC_SHELL_INACCURACY.get();
    }
}
