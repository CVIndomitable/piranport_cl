package com.piranport.compat.maid.combat.handlers;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.PiranPort;
import com.piranport.compat.maid.combat.AmmoConsumer;
import com.piranport.compat.maid.combat.WeaponHandler;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AircraftItem;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AircraftHandler implements WeaponHandler {
    private static final int FUEL_PER_LAUNCH = 1;

    @Override
    public boolean handles(Item item) {
        return item instanceof AircraftItem;
    }

    @Override
    public int cooldownTicks(ItemStack stack) {
        return 600;
    }

    @Override
    public boolean hasAmmo(EntityMaid maid, ItemStack stack) {
        Player owner = AmmoConsumer.ownerPlayer(maid);
        return AmmoConsumer.hasItem(owner, ModItems.AVIATION_FUEL.get(), FUEL_PER_LAUNCH);
    }

    @Override
    public void fire(EntityMaid maid, LivingEntity target, ItemStack stack) {
        Player owner = AmmoConsumer.ownerPlayer(maid);
        if (AmmoConsumer.consumeItem(owner, ModItems.AVIATION_FUEL.get(), FUEL_PER_LAUNCH) < FUEL_PER_LAUNCH) {
            return;
        }

        Level level = maid.level();
        Vec3 spawn = maid.position().add(0, 2.0, 0);
        try {
            AircraftEntity plane = AircraftEntity.createAutonomous(level, spawn, stack.copy());
            if (plane != null) {
                level.addFreshEntity(plane);
            }
        } catch (Throwable t) {
            PiranPort.LOGGER.warn("[Compat/Maid] Aircraft autonomous launch failed: {}", t.toString());
        }
    }
}
