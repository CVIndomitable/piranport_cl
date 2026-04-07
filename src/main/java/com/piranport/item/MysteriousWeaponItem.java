package com.piranport.item;

import com.piranport.entity.RailgunProjectileEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Mysterious Weapon (神秘武器) — fires a railgun projectile.
 * 128 durability, 1.5s cooldown, repaired with iron, no ammo consumption.
 */
public class MysteriousWeaponItem extends Item {

    public MysteriousWeaponItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            RailgunProjectileEntity projectile = new RailgunProjectileEntity(level, player);
            // High speed (3.0), no inaccuracy
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.0f, 0.0f);
            level.addFreshEntity(projectile);

            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5f, 0.5f);
        }

        player.getCooldowns().addCooldown(this, 30); // 1.5s cooldown

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.IRON_INGOT);
    }
}
