package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Phase 26: 高速规避 — dodge handler for EvasionEffect.
 *
 * When a transformed player takes damage and has EvasionEffect active,
 * there is a chance to completely negate the hit:
 *   Level I  (amplifier 0) → 15% dodge
 *   Level II (amplifier 1) → 25% dodge
 *   Level III(amplifier 2) → 35% dodge
 *
 * Formula: chance = (amplifier + 1) * 0.10 + 0.05
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class EvasionHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Must be transformed — check main hand first, then full inventory for no-GUI mode
        boolean isTransformed = false;
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof com.piranport.item.ShipCoreItem
                && TransformationManager.isTransformed(mainHand)) {
            isTransformed = true;
        } else {
            for (ItemStack s : player.getInventory().items) {
                if (s.getItem() instanceof com.piranport.item.ShipCoreItem
                        && TransformationManager.isTransformed(s)) {
                    isTransformed = true;
                    break;
                }
            }
            if (!isTransformed) {
                ItemStack offhand = player.getOffhandItem();
                if (offhand.getItem() instanceof com.piranport.item.ShipCoreItem
                        && TransformationManager.isTransformed(offhand)) {
                    isTransformed = true;
                }
            }
        }
        if (!isTransformed) return;

        // Must have EvasionEffect active
        MobEffectInstance effectInstance = player.getEffect(ModMobEffects.EVASION);
        if (effectInstance == null) return;

        int amplifier = effectInstance.getAmplifier();
        float dodgeChance = (amplifier + 1) * 0.10f + 0.05f;

        float roll = player.getRandom().nextFloat();
        if (roll < dodgeChance) {
            event.setCanceled(true);
            com.piranport.debug.PiranPortDebug.event(
                    "Evasion DODGE | chance={}% roll={} source={}",
                    String.format("%.0f", dodgeChance * 100), String.format("%.2f", roll),
                    event.getSource().type().msgId());

            // Sound — broadcast to nearby players
            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS,
                    0.8f, 1.5f);

            // Particles (server-side send to clients)
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }
}
