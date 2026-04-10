package com.piranport.combat;

import com.piranport.PiranPort;
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

    /** cos(75°) — 150度扇形的半角 */
    private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(75.0));

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // Must be actively using the shield
        if (!player.isUsingItem()) return;
        ItemStack useItem = player.getUseItem();
        if (!(useItem.getItem() instanceof EugenShieldItem)) return;

        // Don't block unblockable damage
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) return;

        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos == null) return;

        // Player look direction (horizontal only)
        Vec3 lookDir = player.getViewVector(1.0F);
        Vec3 lookHorizontal = new Vec3(lookDir.x, 0, lookDir.z);
        if (lookHorizontal.lengthSqr() < 1e-6) return;
        lookHorizontal = lookHorizontal.normalize();

        // Direction from player to damage source (horizontal only)
        Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 dirToSource = sourcePos.subtract(playerCenter);
        Vec3 dirHorizontal = new Vec3(dirToSource.x, 0, dirToSource.z);

        if (dirHorizontal.lengthSqr() < 1e-6) return; // source is at player center
        dirHorizontal = dirHorizontal.normalize();

        // Check if the source is within the 150-degree frontal cone
        double dot = lookHorizontal.dot(dirHorizontal);
        if (dot >= COS_HALF_ANGLE) {
            // Block the damage
            event.setCanceled(true);

            // Consume durability
            useItem.hurtAndBreak(1, player, player.getEquipmentSlotForItem(useItem));

            // Shield block sound
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
