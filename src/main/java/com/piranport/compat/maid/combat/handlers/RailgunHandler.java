package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.RailgunProjectileEntity;
import com.piranport.item.MysteriousWeaponItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RailgunHandler implements WeaponHandler {
    @Override
    public boolean handles(Item item) {
        return item instanceof MysteriousWeaponItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        return 40;
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        Level level = maid.level();
        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(aim.y));

        RailgunProjectileEntity proj = new RailgunProjectileEntity(level, maid);
        proj.setPos(origin.x, origin.y, origin.z);
        proj.shootFromRotation(maid, pitch, yaw, 0f, 4.0f, 0.1f);
        level.addFreshEntity(proj);
    }
}
