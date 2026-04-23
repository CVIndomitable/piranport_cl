package com.piranport.compat.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.task.TaskInjector;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class MaidCombatEvents {
    private MaidCombatEvents() {}

    @SubscribeEvent
    public static void onMaidTick(MaidTickEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide) return;
        if (maid.tickCount % 20 != 0) return;
        if (!TaskInjector.isInjected()) TaskInjector.tryInject();
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof EntityMaid maid) {
            MaidCombatStats.remove(maid.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        EntityMaid attacker = resolveMaidAttacker(event.getSource());
        if (attacker != null) {
            MaidCombatStats.recordDamage(attacker, event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        EntityMaid attacker = resolveMaidAttacker(event.getSource());
        if (attacker != null) {
            MaidCombatStats.recordKill(attacker);
        }
    }

    private static EntityMaid resolveMaidAttacker(DamageSource source) {
        if (source == null) return null;
        if (source.getEntity() instanceof EntityMaid m) return m;
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile p && p.getOwner() instanceof EntityMaid m) return m;
        return null;
    }
}
