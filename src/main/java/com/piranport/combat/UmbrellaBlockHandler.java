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

        // Must be actively using the umbrella
        if (!player.isUsingItem()) return;
        ItemStack useItem = player.getUseItem();
        if (!(useItem.getItem() instanceof TaihouUmbrellaItem)) return;

        // Don't block unblockable damage
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos == null) return;

        // Direction from player center to damage source
        Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 dirToSource = sourcePos.subtract(playerCenter).normalize();

        // Check if the source is within the 150-degree cone from above.
        // dot(dirToSource, UP) = dirToSource.y
        if (dirToSource.y >= COS_HALF_ANGLE) {
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
