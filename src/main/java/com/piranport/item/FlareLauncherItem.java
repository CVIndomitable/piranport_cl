package com.piranport.item;

import com.piranport.entity.FlareProjectileEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Flare Launcher (照明弹发射器) — fires a soft white light orb.
 * Each use consumes 1 durability. No ammo required.
 * Durability: 4096.
 */
public class FlareLauncherItem extends Item {

    public FlareLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            FlareProjectileEntity flare = new FlareProjectileEntity(level, player);
            flare.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 1.0f);
            level.addFreshEntity(flare);

            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        player.getCooldowns().addCooldown(this, 10); // 0.5s cooldown

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
