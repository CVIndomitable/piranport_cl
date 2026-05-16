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
        int barrels;
        List<Item> candidates;
        if (stack.getItem() instanceof ArtilleryItem ai) {
            barrels = Math.max(1, ai.getBarrelCount());
            candidates = shellsFor(ai.getDamage());
        } else {
            return false;
        }
        Player owner = AmmoConsumer.ownerPlayer(maid);
        if (owner == null) return false;
        Item preferred = AmmoConsumer.getPreferredAmmo(owner, candidates);
        if (preferred != null) {
            return AmmoConsumer.hasItem(owner, preferred, barrels) || AmmoConsumer.isFreebie(owner);
        }
        return AmmoConsumer.isFreebie(owner);
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        int barrels;
        float damage;
        if (stack.getItem() instanceof ArtilleryItem ai) {
            barrels = Math.max(1, ai.getBarrelCount());
            damage = ai.getDamage();
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

        Level level = maid.level();
        float explosion = guessExplosion(damage);
        float velocity = guessVelocity(damage);
        float inaccuracy = guessInaccuracy(damage);

        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(aim.y));

        for (int i = 0; i < loaded; i++) {
            CannonProjectileEntity proj = new CannonProjectileEntity(level, maid, ItemStack.EMPTY, damage, true, explosion);
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
                    ModItems.LARGE_AP_SHELL.get(),
                    ModItems.LARGE_TYPE3_SHELL.get()
            );
        }
        if (damage >= 12f) {
            return List.of(
                    ModItems.MEDIUM_HE_SHELL.get(),
                    ModItems.MEDIUM_AP_SHELL.get(),
                    ModItems.MEDIUM_TYPE3_SHELL.get()
            );
        }
        return List.of(
                ModItems.SMALL_HE_SHELL.get(),
                ModItems.SMALL_AP_SHELL.get(),
                ModItems.SMALL_VT_SHELL.get()
        );
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
