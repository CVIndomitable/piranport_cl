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
        return AmmoConsumer.hasItem(owner, ammo, 1);
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof MissileLauncherItem launcher)) return;

        // 防空导弹目标预检查
        if (launcher.getMissileType() == MissileEntity.MissileType.ANTI_AIR) {
            if (target.onGround() || target.isInWater()) {
                // 防空导弹无法攻击地面/水中目标，静默跳过
                return;
            }
        }

        // 弹道判定先于扣弹药：射击方向由 getBoundingBox().getCenter() 得出，几何上极少退化，
        // 但 Math.asin 在 |y| > 1 时返回 NaN，会把导弹射向一个无意义的俯仰角。
        // 同一场景下若已先扣弹药，就变成「扣了弹却打空炮」——判定必须前置。
        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(clampUnit(aim.y)));

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

        for (int i = 0; i < loaded; i++) {
            MissileEntity missile = new MissileEntity(level, launcher.getMissileType(), damage, armorPen, explosion, ammoId);
            missile.setOwner(maid);
            missile.setPos(origin.x, origin.y, origin.z);
            missile.setTrackedTarget(target);
            missile.shootFromRotation(maid, pitch, yaw, 0f, 0.5f, 1.5f);
            level.addFreshEntity(missile);
        }
    }

    /** 把方向向量的 y 分量夹到 asin 定义域内，防浮点误差产生 NaN 俯仰角。 */
    private static double clampUnit(double v) {
        return v < -1.0 ? -1.0 : Math.min(v, 1.0);
    }
}
