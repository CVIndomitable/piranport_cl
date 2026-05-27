package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * 大型船（LARGE 吨位）变身态免疫击退与 hurt 视觉摇晃：
 * - 取消 vanilla 击退位移（LivingKnockBackEvent cancel）
 * - 归零 hurtTime / hurtDir，阻止客户端播放受击方向倾斜动画
 *
 * 仅当玩家处于 LARGE ShipCore 变身态时生效。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class LargeShipImmunityHandler {

    private LargeShipImmunityHandler() {}

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;
        if (!isLargeShip(player)) return;
        event.setCanceled(true);
    }

    /**
     * 清除受击抖动状态 —— hurtTime==0 时受击方向倾斜动画不渲染。
     *
     * 注意：ClientboundHurtAnimationPacket 在 hurt() 方法内部（事件触发前）已发送，
     * 服务端修改 hurtTime 不会同步回客户端，因此客户端仍会看到一帧受击动画。
     * 完美修复需要使用 Mixin 在 hurt() 发包前拦截，当前为已知限制。
     */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;
        if (!isLargeShip(player)) return;
        player.hurtTime = 0;
        player.hurtDuration = 0;
    }

    private static boolean isLargeShip(ServerPlayer player) {
        if (!TransformationManager.isPlayerTransformed(player)) return false;
        ItemStack coreStack = TransformationManager.findTransformedCore(player);
        if (coreStack.isEmpty()) return false;
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;
        return sci.getShipType() == ShipType.LARGE;
    }
}
