package com.piranport.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.client.HarpHandLayout;
import com.piranport.client.RiggingClientState;
import com.piranport.client.UnicornHarpPerformance;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 第三人称：变身玩家不显示「已经挂在舰装上」的武器。
 *
 * <p>WHY：变身后炮/鱼雷/导弹/深弹只以舰装挂点的 3D 模型出现，手里再拿一份会重叠；
 * 但工具、食物、方块等普通物品仍应按原版显示，所以不能整体取消这一层，
 * 必须按「哪只手 + 手里是什么」逐个判断。规则集中在
 * {@link RiggingClientState#isHandItemHidden}，与第一人称共用。
 *
 * <p>要点：主手舰载机只在甲板上显示；副手只隐藏变身核心。
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    // 在专用物品层分派之前替换参数：空副手也能画琴，望远镜层不会漏画被隐藏的物品。
    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/layers/ItemInHandLayer;renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"), require = 2)
    private void piranport$routeHarp(Args args) {
        if (!(args.get(0) instanceof AbstractClientPlayer player)) return;
        HarpHandLayout layout = HarpHandLayout.of(player);
        if (layout != HarpHandLayout.VANILLA) {
            args.set(1, layout.visibleStack(player, args.get(3), UnicornHarpPerformance.isPerforming(player)));
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void piranport$hideRiggingHandItems(LivingEntity entity, ItemStack stack,
                                                ItemDisplayContext displayContext, HumanoidArm arm,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        boolean mainHand = HarpHandLayout.of(player).visualMainArm(player.getMainArm()) == arm;
        if (RiggingClientState.isHandItemHidden(player, mainHand, stack)) {
            ci.cancel();
        }
    }
}
