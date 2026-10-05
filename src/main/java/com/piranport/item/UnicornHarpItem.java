package com.piranport.item;

import com.piranport.config.ModEquipmentConfig;
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

    public UnicornHarpItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Apply Regeneration to friendly players within range（范围/时长/等级均终端可调）
            AABB area = player.getBoundingBox().inflate(ModEquipmentConfig.UNICORN_HARP_RANGE.get());
            List<Player> nearby = level.getEntitiesOfClass(Player.class, area);
            for (Player target : nearby) {
                // Skip hostile players in PvP (check if they can hurt each other)
                if (target != player && player.canHarmPlayer(target)) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                        ModEquipmentConfig.UNICORN_HARP_REGEN_DURATION.get(),
                        ModEquipmentConfig.UNICORN_HARP_REGEN_AMPLIFIER.get()));
            }
        }

        // Play harp (note block) sound
        level.playSound(player, player.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        // Cooldown to prevent spam（终端可调，默认 20 tick = 1 秒）
        player.getCooldowns().addCooldown(this, ModEquipmentConfig.UNICORN_HARP_USE_COOLDOWN.get());

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
