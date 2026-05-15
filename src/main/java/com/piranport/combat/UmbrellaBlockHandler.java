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
 * 格挡来自上方150度锥形范围内的伤害。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class UmbrellaBlockHandler {

    /** cos(75°) — 150度锥的半角 */
    private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(75.0));

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

        // 玩家中心到伤害源的方向
        Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 dirToSource = sourcePos.subtract(playerCenter);
        if (dirToSource.lengthSqr() < 1e-6) return;
        dirToSource = dirToSource.normalize();

        // 检查伤害源是否在正上方 150 度锥形内
        // The umbrella protects against attacks coming from above (anti-air).
        // We check the angle between dirToSource and the UP vector (0, 1, 0).
        // dot(dirToSource, UP) = dirToSource.y = cos(angle)
        // If angle <= 75° (half of 150°), then cos(angle) >= cos(75°)
        if (dirToSource.y >= COS_HALF_ANGLE) {
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
