package com.piranport.combat;

import com.piranport.effect.BurningEffect;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 起火 Debuff 施加规则 — 策划决策/战斗/04-起火Debuff替代原版着火.md
 *
 * <p>依据 2026-09-09 项目所有者定稿的施加规则表：</p>
 * <ul>
 *   <li>小口径 HE：5% 概率；中口径 HE：15%；大口径 HE：40%</li>
 *   <li>航空炸弹：单次判定 r∈[0,1)：r<0.4 → 等级 +2；0.4≤r<0.8 → +1；r≥0.8 → 失败</li>
 *   <li>着火时长固定 15 秒（300 tick）</li>
 *   <li>目标未着火时增量即初始等级；目标已着火时等级叠加对应增量，最高 4 级</li>
 *   <li>持续时间一律刷新至 15 秒</li>
 * </ul>
 */
public final class FireApplyHelper {

    /** 着火时长固定 15 秒 = 300 tick（策划 §4.3） */
    public static final int FIRE_DURATION_TICKS = 300;

    /** 起火最大等级（amplifier 0-3 → 等级 1-4） */
    public static final int MAX_LEVEL = 4;

    private FireApplyHelper() {}

    /**
     * 依据伤害源弹种判定是否施加起火 Debuff；命中实体后调用。
     * 仅命中活体（{@link LivingEntity}）有效；非活体直接返回。
     *
     * @param target  命中目标
     * @param ammoItem 命中本发的弹种物品（可为空——空时按通用航空炸弹路径）
     * @param aerialBomb 是否走航空炸弹分支
     */
    public static void tryApplyFire(LivingEntity target, Item ammoItem, boolean aerialBomb) {
        int delta = computeDelta(ammoItem, aerialBomb, target.getRandom());
        if (delta <= 0) return;
        applyBurning(target, delta);
    }

    /** 概率/判定分支：返回 0 表示失败；1-3 表示本次叠级增量 */
    public static int computeDelta(Item ammoItem, boolean aerialBomb, net.minecraft.util.RandomSource rng) {
        if (aerialBomb) {
            // 航空炸弹：单次随机数判定
            float r = rng.nextFloat();
            if (r < 0.4f) return 2;
            if (r < 0.8f) return 1;
            return 0;
        }
        if (ammoItem == null) return 0;
        ItemStack probe = new ItemStack(ammoItem);
        float prob;
        if (probe.is(ShipCoreItem.SMALL_SHELLS)) {
            prob = 0.05f;
        } else if (probe.is(ShipCoreItem.MEDIUM_SHELLS)) {
            prob = 0.15f;
        } else if (probe.is(ShipCoreItem.LARGE_SHELLS)) {
            prob = 0.40f;
        } else {
            return 0;
        }
        if (rng.nextFloat() < prob) return 1;
        return 0;
    }

    /** 在目标上叠加/刷新燃烧效果（amplifier 0-3，等级 1-4）。 */
    public static void applyBurning(LivingEntity target, int delta) {
        if (delta <= 0) return;
        MobEffectInstance existing = target.getEffect(ModMobEffects.BURNING);
        int oldAmplifier = existing != null ? existing.getAmplifier() : -1;
        int newAmplifier = Math.min(MAX_LEVEL - 1, Math.max(0, oldAmplifier) + delta);
        target.addEffect(new MobEffectInstance(
                ModMobEffects.BURNING, FIRE_DURATION_TICKS, newAmplifier, false, false, true));
        // 防 effect 实例未触发刷新时强制刷新：移除后重新施加保证 15s
        if (existing != null && existing.getAmplifier() == newAmplifier
                && existing.getDuration() < FIRE_DURATION_TICKS) {
            target.removeEffect(ModMobEffects.BURNING);
            target.addEffect(new MobEffectInstance(
                    ModMobEffects.BURNING, FIRE_DURATION_TICKS, newAmplifier, false, false, true));
        }
    }

    /**
     * 仅施加等级（不掷骰）—— 给外部已经判定成功的流程（如俯冲轰炸机挂载）直接使用。
     */
    public static void applyBurningDirect(LivingEntity target, int delta) {
        applyBurning(target, delta);
    }
}