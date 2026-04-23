package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.item.SmokeCandleItem;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SmokeCandleHandler implements WeaponHandler {
    @Override
    public boolean handles(Item item) {
        return item instanceof SmokeCandleItem;
    }

    @Override
    public boolean isOffensive() {
        return false;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        return 20;
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        Level level = maid.level();
        BlockState smoke = ModBlocks.SMOKE_SCREEN.get().defaultBlockState();
        BlockPos center = maid.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, smoke, 3);
                    }
                }
            }
        }
    }
}
