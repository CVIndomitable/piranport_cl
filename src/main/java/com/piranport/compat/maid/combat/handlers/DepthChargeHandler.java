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

        // 弹道判定先于扣弹药：目标位置与发射点重合时 aim 退化为零向量，
        // 且 Math.asin 在 |y| > 1 时返回 NaN。判定若放在消耗之后，
        // 就会出现「深弹已扣掉却一枚未投」的净损失。
        Vec3 origin = maid.getEyePosition();
        Vec3 aim = target.position().subtract(origin);
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.asin(clampUnit(aim.y)));

        Player owner = AmmoConsumer.ownerPlayer(maid);
        int requested = Math.max(1, launcher.getChargeCount());
        int loaded = AmmoConsumer.consumeItem(owner, ModItems.DEPTH_CHARGE.get(), requested);
        if (loaded <= 0) return;

        Level level = maid.level();

        for (int i = 0; i < loaded; i++) {
            DepthChargeEntity dc = new DepthChargeEntity(level, maid, DAMAGE, EXPLOSION);
            dc.setPos(origin.x, origin.y, origin.z);
            float offsetPitch = pitch + (i - (loaded - 1) / 2f) * 4f;
            dc.shootFromRotation(maid, offsetPitch, yaw, 0f, 1.6f, 1.0f);
            level.addFreshEntity(dc);
        }
    }

    /** 把方向向量的 y 分量夹到 asin 定义域内，防浮点误差产生 NaN 俯仰角。 */
    private static double clampUnit(double v) {
        return v < -1.0 ? -1.0 : Math.min(v, 1.0);
    }
}
