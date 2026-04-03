package com.piranport.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class UnicornHarpItem extends Item {

    private static final double RANGE = 16.0;
    private static final int REGEN_DURATION = 200; // 10 seconds
    private static final int REGEN_AMPLIFIER = 0;  // Level I

    public UnicornHarpItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Apply Regeneration I to all living entities within 16 blocks
            AABB area = player.getBoundingBox().inflate(RANGE);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity entity : entities) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION, REGEN_AMPLIFIER));
            }
        }

        // Play harp (note block) sound
        level.playSound(player, player.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        // Cooldown to prevent spam (1 second)
        player.getCooldowns().addCooldown(this, 20);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
