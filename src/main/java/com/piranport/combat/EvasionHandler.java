package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import com.piranport.effect.CombatEffectRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 高速规避只在变身时生效，I/II/III 为 10%/20%/30%，超过 III 不再增加。 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class EvasionHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 不可闪避绝对/穿透伤害类型（虚空、/kill、饥饿）
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.STARVE)) return;

        if (!TransformationManager.isPlayerTransformed(player)) return;

        // 必须激活规避效果
        MobEffectInstance effectInstance = player.getEffect(ModMobEffects.EVASION);
        if (effectInstance == null) return;

        int amplifier = effectInstance.getAmplifier();
        // 策划要求 10%/20%/30%，无额外偏移
        float dodgeChance = CombatEffectRules.evasionChance(amplifier);

        float roll = player.getRandom().nextFloat();
        if (roll < dodgeChance) {
            event.setCanceled(true);
            com.piranport.debug.PiranPortDebug.event(
                    "Evasion DODGE | chance={}% roll={} source={}",
                    Math.round(dodgeChance * 100),
                    Math.round(roll * 100.0) / 100.0,
                    event.getSource().type().msgId());

            // 音效 — 向附近玩家广播
            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS,
                    0.8f, 1.5f);

            // 粒子（服务端发送到客户端）
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }
}
