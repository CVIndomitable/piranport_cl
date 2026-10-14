package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.item.TaihouUmbrellaItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 大凤的伞格挡处理 — 当玩家右键举起伞时，
 * 格挡来自玩家碰撞箱上方的伤害。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class UmbrellaBlockHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 必须正在使用伞
        if (!player.isUsingItem()) return;
        ItemStack useItem = player.getUseItem();
        if (!(useItem.getItem() instanceof TaihouUmbrellaItem)) return;

        // Don't block unblockable damage
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) return;

        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos == null) return;

        // 伞只挡「上方来弹」：伤害源高于玩家碰撞箱顶即格挡。
        // 旧实现用「玩家中心→伤害源」向量与竖直方向的夹角判定，零距离/体内命中时向量退化、
        // 归一化方向随机导致格挡概率性失效；高度判定没有这个退化情况。
        // 注意：DEFENSE_CONE_HALF_ANGLE_DEG 是欧根舰盾的方向锥参数，伞不再引用它。
        if (sourcePos.y > player.getY() + player.getBbHeight()) {
            // 格挡伤害
            event.setCanceled(true);

            // 消耗耐久
            useItem.hurtAndBreak(1, player, player.getEquipmentSlotForItem(useItem));

            // 盾牌格挡音效
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
