package com.piranport.mixin.client;

import com.piranport.client.UnicornHarpPerformance;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class HarpPlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void piranport$playHarp(LivingEntity entity, float limbSwing, float amount, float ageInTicks,
                                    float yaw, float pitch, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            UnicornHarpPerformance.pose((PlayerModel<?>) (Object) this, player, ageInTicks);
        }
    }
}
