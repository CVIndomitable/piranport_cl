package com.piranport.handler;

import com.piranport.aviation.ReconManager;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * 侦察模式身体锁定与残留减速清理，仅在服务端主线程执行。
 * 身体锁定在变身检查前执行；残留效果按原20 tick周期清理。
 */
final class PlayerReconTick {
    private PlayerReconTick() {}

    private static final int SLOWNESS_CLEANUP_INTERVAL = 20;

    /** 侦察模式：锁定玩家身体位置，阻止移动 */
    static void tickReconBodyLock(Player player) {
        if (ReconManager.isInRecon(player.getUUID())) {
            Vec3 vel = player.getDeltaMovement();
            double newY = vel.y > 0 ? 0 : vel.y;
            player.setDeltaMovement(0, newY, 0);
            player.xxa = 0;
            player.zza = 0;
            if (player.isInWater() && !player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
                player.setDeltaMovement(0, 0, 0);
                player.resetFallDistance();
            }
        }
    }

    /** 清除侦察模式残留的减速效果（每20tick） */
    static void tickCleanupResidualSlowdown(Player player) {
        if (player.tickCount % SLOWNESS_CLEANUP_INTERVAL != 0) return;
        MobEffectInstance slowness = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null && slowness.getAmplifier() >= 9
                && !ReconManager.isInRecon(player.getUUID())) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }
}
