package com.piranport.item;

import com.piranport.entity.FloatingTargetEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FloatingTargetItem extends Item {

    public FloatingTargetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        HitResult hit = player.pick(5.0, 1.0f, true);
        if (hit instanceof BlockHitResult blockHit) {
            if (level.getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) {
                if (!level.isClientSide()) {
                    FloatingTargetEntity target = new FloatingTargetEntity(
                            ModEntityTypes.FLOATING_TARGET.get(), level);
                    double x = blockHit.getBlockPos().getX() + 0.5;
                    double y = blockHit.getBlockPos().getY() + 0.3;
                    double z = blockHit.getBlockPos().getZ() + 0.5;
                    target.setPos(x, y, z);
                    level.addFreshEntity(target);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
