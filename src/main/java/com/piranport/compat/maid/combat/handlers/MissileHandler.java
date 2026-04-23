package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.combat.AmmoConsumer;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.MissileEntity;
import com.piranport.item.MissileLauncherItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MissileHandler implements WeaponHandler {
    @Override
    public boolean handles(Item item) {
        return item instanceof MissileLauncherItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        if (stack.getItem() instanceof MissileLauncherItem l) {
            int raw = l.getCooldownTicks();
            return raw > 0 ? raw : 40;
        }
        return 40;
    }

    @Override
    public boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        if (!(stack.getItem() instanceof MissileLauncherItem launcher)) return false;
        Item ammo = launcher.getAmmoItem();
        if (ammo == null) return true;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        int need = Math.max(1, launcher.getBurstCount());
        return AmmoConsumer.hasItem(owner, ammo, need);
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof MissileLauncherItem launcher)) return;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        int burst = Math.max(1, launcher.getBurstCount());
        Item ammo = launcher.getAmmoItem();
        int loaded = ammo == null ? burst : AmmoConsumer.consumeItem(owner, ammo, burst);
        if (loaded <= 0) return;

        Level level = maid.level();
        float damage = launcher.getDamage();
        float armorPen = launcher.getArmorPen();
        float explosion = launcher.getExplosionPower();
        String ammoId = ammo != null ? BuiltInRegistries.ITEM.getKey(ammo).toString() : "";

        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(aim.y));

        for (int i = 0; i < loaded; i++) {
            MissileEntity missile = new MissileEntity(level, launcher.getMissileType(), damage, armorPen, explosion, ammoId);
            missile.setOwner(maid);
            missile.setPos(origin.x, origin.y, origin.z);
            missile.setTrackedTarget(target);
            missile.shootFromRotation(maid, pitch, yaw, 0f, 0.5f, 1.5f);
            level.addFreshEntity(missile);
        }
    }
}
