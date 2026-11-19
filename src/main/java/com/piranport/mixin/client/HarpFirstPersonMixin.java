package com.piranport.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.piranport.client.HarpHandLayout;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public abstract class HarpFirstPersonMixin {
    // 另一件物品继续走原版持物/使用动画，只调整它在画面里的手臂。
    @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getMainArm()Lnet/minecraft/world/entity/HumanoidArm;"), require = 2)
    private HumanoidArm piranport$harpVisualArm(HumanoidArm original, AbstractClientPlayer player) {
        return HarpHandLayout.of(player).visualMainArm(original);
    }

    // 弓弩通常跳过另一手的回调；有琴时必须保留回调，让后续物品层正常作取舍。
    @ModifyExpressionValue(method = "evaluateWhichHandsToRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 4)
    private static boolean piranport$allowHarpAlongsideBow(boolean isBow, LocalPlayer player) {
        return isBow && HarpHandLayout.of(player) == HarpHandLayout.VANILLA;
    }
}
