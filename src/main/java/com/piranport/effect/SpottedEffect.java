package com.piranport.effect;

import com.piranport.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 策划 §7.7：被发现！Buff
 *
 * <p>标记 effect，无自带 tick 逻辑。被标记的生物会被火控系统优先锁定 —
 * 见 {@code FireControlManager.spottedEntities()}。客户端粒子/音效由 {@code BurningEffect}
 * 共用的服务端 sendParticles 通道触发（每 20t 一束烟雾）。
 */
public class SpottedEffect extends MobEffect {

    public SpottedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFF00);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            // 每 20 tick（1 秒）触发一束红色烟雾粒子作为视觉指示
            if (entity.tickCount % 20 == 0) {
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    // 烟雾粒子作"被发现"标记的视觉指示（黄色 SMOKE 不可用，直接用红色烟雾）
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            entity.getX(), entity.getY() + entity.getBbHeight() + 0.3, entity.getZ(),
                            4, 0.2, 0.1, 0.2, 0.02);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0;
    }

    /** 便利方法：检查实体是否被"被发现"标记。 */
    public static boolean isSpotted(LivingEntity entity) {
        return entity.hasEffect(ModMobEffects.SPOTTED);
    }
}