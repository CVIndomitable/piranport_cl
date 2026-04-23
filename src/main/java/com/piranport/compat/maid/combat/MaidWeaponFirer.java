package com.piranport.compat.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.PiranPort;
import com.piranport.compat.maid.combat.handlers.AircraftHandler;
import com.piranport.compat.maid.combat.handlers.CannonHandler;
import com.piranport.compat.maid.combat.handlers.DepthChargeHandler;
import com.piranport.compat.maid.combat.handlers.FlareHandler;
import com.piranport.compat.maid.combat.handlers.GungnirHandler;
import com.piranport.compat.maid.combat.handlers.MissileHandler;
import com.piranport.compat.maid.combat.handlers.RailgunHandler;
import com.piranport.compat.maid.combat.handlers.SmokeCandleHandler;
import com.piranport.compat.maid.combat.handlers.TorpedoHandler;
import com.piranport.component.WeaponCooldown;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class MaidWeaponFirer {
    private static final List<WeaponHandler> HANDLERS = List.of(
            new CannonHandler(),
            new TorpedoHandler(),
            new MissileHandler(),
            new DepthChargeHandler(),
            new GungnirHandler(),
            new RailgunHandler(),
            new FlareHandler(),
            new SmokeCandleHandler(),
            new AircraftHandler()
    );

    private MaidWeaponFirer() {}

    public static WeaponHandler findHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (WeaponHandler h : HANDLERS) {
            if (h.handles(stack.getItem())) return h;
        }
        return null;
    }

    public static boolean isSupported(ItemStack stack) {
        return findHandler(stack) != null;
    }

    public static boolean isOffensiveWeapon(ItemStack stack) {
        WeaponHandler h = findHandler(stack);
        return h != null && h.isOffensive();
    }

    public static boolean canFire(EntityMaid maid, ItemStack stack) {
        WeaponHandler h = findHandler(stack);
        if (h == null) return false;
        WeaponCooldown cd = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cd != null && cd.isOnCooldown(maid.level().getGameTime())) return false;
        return h.hasAmmo(maid, stack);
    }

    public static void fire(EntityMaid maid, LivingEntity target) {
        ItemStack stack = maid.getMainHandItem();
        WeaponHandler h = findHandler(stack);
        if (h == null) return;
        long now = maid.level().getGameTime();
        WeaponCooldown cd = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (cd != null && cd.isOnCooldown(now)) return;
        if (!h.hasAmmo(maid, stack)) return;
        try {
            h.fire(maid, target, stack);
            MaidCombatStats.recordShot(maid);
        } catch (Throwable t) {
            PiranPort.LOGGER.error("[Compat/Maid] Weapon fire failed for {}", stack.getItem(), t);
            return;
        }
        int ticks = h.cooldownTicks(stack);
        if (ticks > 0) {
            stack.set(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.of(now + ticks, ticks));
        }
    }
}
