package com.piranport.item;

import com.piranport.config.ModEquipmentConfig;
import com.piranport.network.HarpPerformancePayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class UnicornHarpItem extends Item {

    public UnicornHarpItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 恢复仍在右键成功时结算一次，不随演奏节拍重复触发。
            AABB area = player.getBoundingBox().inflate(ModEquipmentConfig.UNICORN_HARP_RANGE.get());
            List<Player> nearby = level.getEntitiesOfClass(Player.class, area);
            for (Player target : nearby) {
                if (target != player && player.canHarmPlayer(target)) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                        ModEquipmentConfig.UNICORN_HARP_REGEN_DURATION.get(),
                        ModEquipmentConfig.UNICORN_HARP_REGEN_AMPLIFIER.get()));
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new HarpPerformancePayload(player.getId(), hand));
        }

        level.playSound(player, player.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        player.getCooldowns().addCooldown(this, ModEquipmentConfig.UNICORN_HARP_USE_COOLDOWN.get());

        // 让专属拨弦动作取代普通右键挥手；无需持续按住使用键。
        return InteractionResultHolder.consume(stack);
    }
}
