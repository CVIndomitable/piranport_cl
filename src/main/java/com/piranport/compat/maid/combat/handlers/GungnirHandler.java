package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.GungnirEntity;
import com.piranport.item.GungnirItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GungnirHandler implements WeaponHandler {
    @Override
    public boolean handles(Item item) {
        return item instanceof GungnirItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        return 60;
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

        GungnirEntity proj = new GungnirEntity(level, maid, stack.copy());
        proj.setPos(origin.x, origin.y, origin.z);
        proj.shootFromRotation(maid, pitch, yaw, 0f, 2.5f, 0.5f);
        level.addFreshEntity(proj);

        // WHY: 玩家路径（GungnirItem.use）是「清空手持 + 实体返还时按耐久扣 1 并入背包」，
        // 枪本体在投掷瞬间就离开了玩家。女仆没有玩家背包，若照抄「清空手持」则枪会消失；
        // 因此等价语义是「枪不脱手，每次投掷扣 1 点耐久」——实体只是投射物，不代表物品所有权。
        // 注意 maid.getMainHandItem() 返回的是 Mob.handItems 里的活引用（Mob.getItemBySlot
        // 直接取自 NonNullList，非 copy），所以损坏值会直接写回女仆手部槽位，无需再 setItemSlot 回写。
        // 必须用 EntityMaid 自己的 hurtAndBreak(ItemStack, int) 重载：它内部会正确传 ServerLevel
        // 与 maid 自身，并广播 ItemBreakPackage 做损坏表现（玩家侧对应 ItemStack.hurtAndBreak(1, player, MAINHAND)）。
        maid.hurtAndBreak(stack, 1);

        // WHY: stacksTo(1) 且耐久 512，损坏时 ItemStack.hurtAndBreak 内部走 shrink(1) 让栈自身变空，
        // 但 handItems 里存的对象只是变空、槽位语义需要显式同步给 TLM（动画/装备事件）。
        // 枪碎即空手，后续 MaidWeaponFirer.findHandler(EMPTY) 返回 null，女仆自然停火。
        if (stack.isEmpty()) {
            maid.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }
}
