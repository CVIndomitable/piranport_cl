package com.piranport.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 灭火器 / 损管（精英/普通共用 use 逻辑，按 stacksTo 与触发位置区分）。
 * 策划决策：消耗品/01-损管设计.md、消耗品/02-灭火器设计.md
 *
 * <p>右键使用：清除身上所有 {@link MobEffectCategory#HARMFUL} 效果并灭火。
 * <b>白名单保留"超载相关 buff"</b>（决策 §关键约束：挖掘疲劳/虚弱/中毒不得清除，
 * 这些是舰装过载的安全警示）。</p>
 */
public class DamageControlItem extends Item {

    /** 2 minutes = 2400 ticks */
    private static final int COOLDOWN_TICKS = 2400;

    /**
     * 超载相关 buff 白名单（决策 §关键约束：必须保留，不可清除）。
     * 玩家超载时由 {@code TransformationManager.applyOverweightPenalty} 施加这些效果。
     * 通过 Holder 引用的 ResourceKey 标识，运行时查找避免依赖静态字段。
     */
    private static final Set<net.minecraft.resources.ResourceKey<MobEffect>> OVERLOAD_EFFECT_KEYS = new HashSet<>();

    static {
        OVERLOAD_EFFECT_KEYS.add(BuiltInRegistries.MOB_EFFECT.getResourceKey(MobEffects.DIG_SLOWDOWN.value())
                .orElseThrow());
        OVERLOAD_EFFECT_KEYS.add(BuiltInRegistries.MOB_EFFECT.getResourceKey(MobEffects.WEAKNESS.value())
                .orElseThrow());
        OVERLOAD_EFFECT_KEYS.add(BuiltInRegistries.MOB_EFFECT.getResourceKey(MobEffects.POISON.value())
                .orElseThrow());
    }

    public DamageControlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 收集有害效果 — 但跳过"超载相关 buff"白名单（决策 §关键约束）
            List<MobEffectInstance> toRemove = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;
                var key = effect.getEffect().unwrapKey();
                if (key.isPresent() && OVERLOAD_EFFECT_KEYS.contains(key.get())) continue;
                toRemove.add(effect);
            }
            for (MobEffectInstance effect : toRemove) {
                player.removeEffect(effect.getEffect());
            }

            // Extinguish fire
            player.clearFire();
        }

        level.playSound(player, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1.0F, 1.0F);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
