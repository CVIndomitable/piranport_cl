package com.piranport.entity;

import com.piranport.registry.ModEntityTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AircraftDropEntity extends ItemEntity {

    public AircraftDropEntity(EntityType<? extends AircraftDropEntity> type, Level level) {
        super(type, level);
    }

    public AircraftDropEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntityTypes.AIRCRAFT_DROP.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setPickUpDelay(20);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (isInWater()) {
            setDeltaMovement(getDeltaMovement().add(0, 0.06, 0));
        }
    }
}
