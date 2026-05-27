package com.piranport.combat;

import com.piranport.PiranPort;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Armor plate protection handler — reduces incoming damage based on
 * the total protection level of equipped ArmorPlateItems.
 *
 * Each protection level reduces damage by 4% (same as vanilla Protection enchantment).
 * Formula: damage * (1 - min(protectionLevel, 20) * 0.04)
 * Cap at 20 levels = 80% max reduction (matches vanilla enchantment cap).
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ArmorProtectionHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 仅忽略不可抗伤害（虚空、/kill）
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        // 合并 findTransformedCore 调用，避免重复背包扫描
        ItemStack coreStack = TransformationManager.findTransformedCore(player);
        if (coreStack.isEmpty()) return;

        int protLevel = TransformationManager.getEquippedProtectionLevel(coreStack);
        if (protLevel <= 0) return;

        int capped = Math.min(protLevel, 20);
        float reduction = capped * 0.04f;
        float newAmount = event.getAmount() * (1.0f - reduction);
        event.setAmount(newAmount);
    }
}
