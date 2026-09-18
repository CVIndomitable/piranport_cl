package com.piranport.handler;

import com.piranport.config.ModCommonConfig;
import com.piranport.registry.ModAttachmentTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * 水上行走的物理与生命周期入口；由客户端和服务端各自驱动各自的玩家实例。
 * attachment 只保存本地瞬时缓存，不保存到存档，也不参与网络同步。
 */
public final class WaterWalkingHandler {
    private WaterWalkingHandler() {}

    public static void tick(Player player, boolean isSubmarine) {
        if (isSubmarine) {
            clear(player);
            return;
        }

        WaterWalkingState state = player.getData(ModAttachmentTypes.WATER_WALKING.get());
        state.bindToLevel(player.level());

        if (!player.isInWater()) {
            state.clearSurface();
            return;
        }
        if (player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
            applyUnderwaterBuoyancy(player);
            state.clearSurface();
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < 0) {
            player.setDeltaMovement(velocity.x, 0.0, velocity.z);
        }

        double currentY = player.getY();
        double surfaceY = state.resolveSurfaceY(currentY);
        if (surfaceY != currentY) {
            player.setPos(player.getX(), surfaceY, player.getZ());
            player.setDeltaMovement(velocity.x, 0.0, velocity.z);
        }
        player.resetFallDistance();
        applyHorizontalMovement(player, state);
    }

    /** 解除变身、燃料耗尽或切换为潜艇后，移除整个瞬时状态以避免再次变身时复用。 */
    public static void clear(Player player) {
        player.removeData(ModAttachmentTypes.WATER_WALKING.get());
    }

    private static void applyUnderwaterBuoyancy(Player player) {
        Vec3 velocity = player.getDeltaMovement();
        double buoyancy = ModCommonConfig.WATER_SURFACE_BUOYANCY.get();
        double maxRiseSpeed = Math.max(0.05, Math.min(0.6, buoyancy));
        double rise = Math.min(maxRiseSpeed, Math.max(velocity.y, 0.0) + buoyancy * 0.08);
        player.setDeltaMovement(velocity.x, rise, velocity.z);
        player.resetFallDistance();
    }

    private static void applyHorizontalMovement(Player player, WaterWalkingState state) {
        double acceleration = ModCommonConfig.WATER_WALKING_ACCELERATION.get();
        if (acceleration <= 0.0001) return;

        Vec3 velocity = player.getDeltaMovement();
        float inputX = player.xxa;
        float inputZ = player.zza;
        boolean hasInput = Math.abs(inputX) > 0.01f || Math.abs(inputZ) > 0.01f;

        if (hasInput) {
            WaterWalkingState.Direction direction = state.forwardDirection(player.getYRot());
            double dirX = direction.x() * inputZ + direction.z() * inputX;
            double dirZ = direction.z() * inputZ - direction.x() * inputX;
            double dirLength = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (dirLength > 0.001) {
                dirX /= dirLength;
                dirZ /= dirLength;
                player.setDeltaMovement(
                        velocity.x + dirX * acceleration,
                        velocity.y,
                        velocity.z + dirZ * acceleration);
            }
        } else {
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed > 0.001) {
                double deceleration = ModCommonConfig.WATER_WALKING_DECELERATION.get();
                player.setDeltaMovement(
                        velocity.x * deceleration,
                        velocity.y,
                        velocity.z * deceleration);
            }
        }
    }
}
