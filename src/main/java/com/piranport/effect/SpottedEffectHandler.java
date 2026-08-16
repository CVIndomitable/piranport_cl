package com.piranport.effect;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.registry.ModMobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Phase 27：保持 FireControlManager.SPOTTED_ENTITIES 与 SpottedEffect 实际持有状态一致。
 *
 * <p>effect 添加 → 标记 UUID；effect 自然到期 / 被移除 → 取消标记。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class SpottedEffectHandler {

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() != ModMobEffects.SPOTTED.get()) return;
        LivingEntity entity = event.getEntity();
        FireControlManager.markSpotted(entity.getUUID());
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModMobEffects.SPOTTED.get()) return;
        LivingEntity entity = event.getEntity();
        FireControlManager.clearSpotted(entity.getUUID());
    }

    @SubscribeEvent
    public static void onEffectExpire(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (event.getEffectInstance().getEffect() != ModMobEffects.SPOTTED.get()) return;
        LivingEntity entity = event.getEntity();
        FireControlManager.clearSpotted(entity.getUUID());
    }
}