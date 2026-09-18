package com.piranport.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 独立灭火器物品（消耗品/02-灭火器设计）。
 * <p>右键使用：清除身上所有 HARMFUL 效果并灭火，冷却 120 秒（2400 tick）。
 * 与损管区别：不消耗"强化槽"概念，背包槽位消耗，配方原料不同。</p>
 */
public class FireExtinguisherItem extends Item {

    /** 2 minutes = 2400 ticks cooldown */
    private static final int COOLDOWN_TICKS = 2400;

    public FireExtinguisherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 清除所有 HARMFUL 效果
            List<MobEffectInstance> toRemove = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    toRemove.add(effect);
                }
            }
            for (MobEffectInstance effect : toRemove) {
                player.removeEffect(effect.getEffect());
            }

            // 灭火
            player.clearFire();
        }

        // 音效
        level.playSound(player, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1.0F, 1.0F);

        // 冷却
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
