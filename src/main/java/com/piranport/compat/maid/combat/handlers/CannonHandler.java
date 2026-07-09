package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.compat.maid.combat.AmmoConsumer;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CannonHandler implements WeaponHandler {
    @Override
    public boolean handles(Item item) {
        return item instanceof ArtilleryItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        if (stack.getItem() instanceof ArtilleryItem ai) return ai.getCooldownTicks();
        return 30;
    }

    @Override
    public boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        if (!(stack.getItem() instanceof ArtilleryItem ai)) return false;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        if (AmmoConsumer.isFreebie(owner)) return true;
        List<Item> candidates = shellsFor(ai.getDamage());
        return AmmoConsumer.getPreferredAmmo(owner, candidates) != null;
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        int barrels;
        float damage;
        float explosion;
        float velocity;
        float inaccuracy;

        if (stack.getItem() instanceof ArtilleryItem ai) {
            // 使用有效数据（考虑配置覆盖）
            var effectiveData = ai.getEffectiveData(maid.level());
            barrels = Math.max(1, effectiveData.barrels());
            damage = effectiveData.damage();
            explosion = effectiveData.explosionPower();
            velocity = effectiveData.initialSpeed();
            // 根据散布角计算不精确度（简化映射）
            inaccuracy = effectiveData.dispersion();
        } else {
            return;
        }

        Player owner = AmmoConsumer.ownerPlayer(maid);
        List<Item> candidates = shellsFor(damage);
        Item preferred = AmmoConsumer.getPreferredAmmo(owner, candidates);
        int loaded = 0;

        if (preferred != null) {
            loaded = AmmoConsumer.consumeItem(owner, preferred, barrels);
        }

        if (loaded < barrels) {
            loaded += consumeShells(owner, damage, barrels - loaded);
        }

        if (loaded <= 0) return;
        boolean grenadeShell = preferred != null && isGrenadeShell(preferred);
        boolean flareShell = preferred != null && isFlareShell(preferred);
        boolean smokeShell = preferred != null && isSmokeShell(preferred);
        float shotDamage = grenadeShell ? damage * 0.55f : damage;
        float shotExplosion = grenadeShell ? explosion * 1.8f : explosion;
        if (flareShell || smokeShell) {
            shotDamage = 0.0f;
            shotExplosion = Math.max(0.8f, explosion * 0.55f);
        }

        Level level = maid.level();

        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(aim.y));

        for (int i = 0; i < loaded; i++) {
            CannonProjectileEntity proj = new CannonProjectileEntity(level, maid,
                    preferred != null ? new ItemStack(preferred) : ItemStack.EMPTY,
                    shotDamage, !(flareShell || smokeShell), shotExplosion);
            proj.setFlareShell(flareShell);
            proj.setSmokeShell(smokeShell);
            proj.setPos(origin.x, origin.y, origin.z);
            proj.shootFromRotation(maid, pitch, yaw, 0f, velocity, inaccuracy);
            level.addFreshEntity(proj);
        }
    }

    private static int consumeShells(Player owner, float damage, int amount) {
        if (owner == null) return 0;
        if (AmmoConsumer.isFreebie(owner)) return amount;
        int remaining = amount;
        for (Item shell : shellsFor(damage)) {
            if (remaining <= 0) break;
            remaining -= AmmoConsumer.consumeItem(owner, shell, remaining);
        }
        return amount - remaining;
    }

    private static List<Item> shellsFor(float damage) {
        if (damage >= 20f) {
            return List.of(
                    ModItems.LARGE_HE_SHELL.get(),
                    ModItems.LARGE_GRENADE_SHELL.get(),
                    ModItems.LARGE_FLARE_SHELL.get(),
                    ModItems.LARGE_SMOKE_SHELL.get(),
                    ModItems.LARGE_AP_SHELL.get(),
                    ModItems.LARGE_TYPE3_SHELL.get()
            );
        }
        if (damage >= 12f) {
            return List.of(
                    ModItems.MEDIUM_HE_SHELL.get(),
                    ModItems.MEDIUM_GRENADE_SHELL.get(),
                    ModItems.MEDIUM_FLARE_SHELL.get(),
                    ModItems.MEDIUM_SMOKE_SHELL.get(),
                    ModItems.MEDIUM_AP_SHELL.get(),
                    ModItems.MEDIUM_TYPE3_SHELL.get()
            );
        }
        return List.of(
                ModItems.SMALL_HE_SHELL.get(),
                ModItems.SMALL_GRENADE_SHELL.get(),
                ModItems.SMALL_FLARE_SHELL.get(),
                ModItems.SMALL_SMOKE_SHELL.get(),
                ModItems.SMALL_AP_SHELL.get(),
                ModItems.SMALL_VT_SHELL.get(),
                ModItems.SMALL_TYPE3_SHELL.get()
        );
    }

    private static boolean isGrenadeShell(Item item) {
        return item == ModItems.SMALL_GRENADE_SHELL.get()
                || item == ModItems.MEDIUM_GRENADE_SHELL.get()
                || item == ModItems.LARGE_GRENADE_SHELL.get();
    }

    private static boolean isFlareShell(Item item) {
        return item == ModItems.SMALL_FLARE_SHELL.get()
                || item == ModItems.MEDIUM_FLARE_SHELL.get()
                || item == ModItems.LARGE_FLARE_SHELL.get();
    }

    private static boolean isSmokeShell(Item item) {
        return item == ModItems.SMALL_SMOKE_SHELL.get()
                || item == ModItems.MEDIUM_SMOKE_SHELL.get()
                || item == ModItems.LARGE_SMOKE_SHELL.get();
    }

    private static float guessExplosion(float damage) {
        if (damage >= 20f) return 2.0f;
        if (damage >= 12f) return 1.5f;
        return 1.0f;
    }

    private static float guessVelocity(float damage) {
        if (damage >= 20f) return 3.0f;
        if (damage >= 12f) return 2.5f;
        return 2.0f;
    }

    private static float guessInaccuracy(float damage) {
        if (damage >= 20f) return 0.5f;
        if (damage >= 12f) return 1.0f;
        return 1.5f;
    }
}
