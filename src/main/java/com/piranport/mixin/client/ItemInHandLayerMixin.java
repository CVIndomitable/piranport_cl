package com.piranport.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.client.RiggingClientState;
import com.piranport.item.AircraftItem;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第三人称：变身玩家默认不显示手持物品。
 *
 * <p>WHY：变身舰娘后，武器/工具/食物只以舰装挂点的 3D 模型出现，
 * 双手再拿着物品会与其重叠。唯一例外是主手拿着舰载机——飞机本来就要“拿在手里”
 * 放飞，所以主手飞机的模型保留；副手（核心）在任何情况下都隐藏。
 *
 * <p>第一人称的同一规则在 {@code ClientGameEvents.onRenderHand} 里用
 * {@code RenderHandEvent} 处理（原版第一人称不经过 RenderLayer）。
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    /**
     * 在渲染每个手持物品之前拦截。只有「已变身 + 主手 + 舰载机」放行，
     * 其余全部取消，连副手核心也不画。
     */
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void piranport$hideRiggingHandItems(LivingEntity entity, ItemStack stack,
                                                ItemDisplayContext displayContext, HumanoidArm arm,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        if (!RiggingClientState.isRenderedAsTransformed(player)) return;
        if (player.getMainArm() == arm && stack.getItem() instanceof AircraftItem) {
            return;
        }
        ci.cancel();
    }
}
