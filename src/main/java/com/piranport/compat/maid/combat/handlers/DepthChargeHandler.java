package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.combat.AmmoConsumer;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DepthChargeHandler implements WeaponHandler {
    private static final float DAMAGE = 14f;
    private static final float EXPLOSION = 3.0f;

    @Override
    public boolean handles(Item item) {
        return item instanceof DepthChargeLauncherItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        return stack.getItem() instanceof DepthChargeLauncherItem l ? l.getCooldownTicks() : 80;
    }

    @Override
    public boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        Player owner = AmmoConsumer.ownerPlayer(maid);
        return AmmoConsumer.hasItem(owner, ModItems.DEPTH_CHARGE.get(), 1);
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof DepthChargeLauncherItem launcher)) return;
        Player owner = AmmoConsumer.ownerPlayer(maid);
        int requested = Math.max(1, launcher.getChargeCount());
        int loaded = AmmoConsumer.consumeItem(owner, ModItems.DEPTH_CHARGE.get(), requested);
        if (loaded <= 0) return;

        Level level = maid.level();
        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.position().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(aim.y));

        for (int i = 0; i < loaded; i++) {
            DepthChargeEntity dc = new DepthChargeEntity(level, maid, DAMAGE, EXPLOSION);
            dc.setPos(origin.x, origin.y, origin.z);
            float offsetPitch = pitch + (i - (loaded - 1) / 2f) * 4f;
            dc.shootFromRotation(maid, offsetPitch, yaw, 0f, 1.6f, 1.0f);
            level.addFreshEntity(dc);
        }
    }
}
