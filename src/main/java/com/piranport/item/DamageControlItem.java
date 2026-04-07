package com.piranport.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DamageControlItem extends Item {

    /** 2 minutes = 2400 ticks */
    private static final int COOLDOWN_TICKS = 2400;

    public DamageControlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Collect harmful effects first to avoid ConcurrentModification
            List<MobEffectInstance> toRemove = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    toRemove.add(effect);
                }
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
