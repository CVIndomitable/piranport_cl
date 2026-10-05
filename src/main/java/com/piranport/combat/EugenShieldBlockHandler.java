package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.item.EugenShieldItem;
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
 * 欧根的舰盾格挡处理 — 当玩家右键举起盾牌时，
 * 格挡来自前方150度扇形范围内的伤害。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class EugenShieldBlockHandler {

    /**
     * 格挡锥半角（度）的真源在 {@link ModEquipmentConfig#DEFENSE_CONE_HALF_ANGLE_DEG}
     * （{@code global.defense_cone.half_angle_deg}，默认 75 → 150 度锥）。
     * 与大凤的伞共用同一参数：两者语义相同 —— 都按「方向向量 · 锥轴 ≥ cos(半角)」判定是否落在对称锥内。
     */
    private static double cosHalfAngle() {
        return Math.cos(Math.toRadians(ModEquipmentConfig.DEFENSE_CONE_HALF_ANGLE_DEG.get()));
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 必须正在使用盾牌
        if (!player.isUsingItem()) return;
        ItemStack useItem = player.getUseItem();
        if (!(useItem.getItem() instanceof EugenShieldItem)) return;

        // Don't block unblockable damage
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) return;

        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos == null) return;

        // 玩家视线方向（仅水平分量）
        Vec3 lookDir = player.getViewVector(1.0F);
        Vec3 lookHorizontal = new Vec3(lookDir.x, 0, lookDir.z);
        if (lookHorizontal.lengthSqr() < 1e-6) return;
        lookHorizontal = lookHorizontal.normalize();

        // 玩家到伤害源的方向（仅水平分量）
        Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 dirToSource = sourcePos.subtract(playerCenter);
        Vec3 dirHorizontal = new Vec3(dirToSource.x, 0, dirToSource.z);

        if (dirHorizontal.lengthSqr() < 1e-6) return; // source is at player center
        dirHorizontal = dirHorizontal.normalize();

        // 检查伤害源是否在 150 度正面锥形内
        double dot = lookHorizontal.dot(dirHorizontal);
        if (dot >= cosHalfAngle()) {
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
