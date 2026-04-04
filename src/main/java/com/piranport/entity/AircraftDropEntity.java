package com.piranport.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 飞机击坠掉落物 — 类似下界之星：
 * 1. 不被爆炸摧毁
 * 2. 不沉入水中，浮在水面
 */
public class AircraftDropEntity extends ItemEntity {

    public AircraftDropEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(EntityType.ITEM, level);
        setPos(x, y, z);
        setItem(stack);
        setPickUpDelay(20); // 1秒拾取延迟
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
        // 水中浮力：在水里时向上推，浮到水面
        if (isInWater()) {
            setDeltaMovement(getDeltaMovement().add(0, 0.06, 0));
        }
    }
}
