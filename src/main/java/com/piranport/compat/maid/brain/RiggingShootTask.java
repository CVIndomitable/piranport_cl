package com.piranport.compat.maid.brain;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.piranport.compat.maid.combat.MaidWeaponFirer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class RiggingShootTask extends Behavior<EntityMaid> {
    private static final int FIRE_INTERVAL = 10;
    private int cooldownTick;

    public RiggingShootTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        ItemStack weapon = maid.getMainHandItem();
        if (!MaidWeaponFirer.isOffensiveWeapon(weapon)) return false;
        Optional<LivingEntity> target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (target.isEmpty()) return false;
        LivingEntity t = target.get();
        return t.isAlive() && !t.isRemoved() && maid.canSee(t);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (target.isEmpty()) return false;
        LivingEntity t = target.get();
        if (!t.isAlive() || t.isRemoved()) return false;
        return MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem());
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        this.cooldownTick = FIRE_INTERVAL;
        maid.setSwingingArms(true);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (target.isEmpty()) return;
        LivingEntity t = target.get();
        maid.getLookControl().setLookAt(t, 30f, 30f);
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(t, true));
        if (cooldownTick-- > 0) return;
        cooldownTick = FIRE_INTERVAL;
        if (!maid.canSee(t)) return;
        ItemStack weapon = maid.getMainHandItem();
        if (MaidWeaponFirer.canFire(maid, weapon)) {
            MaidWeaponFirer.fire(maid, t);
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.setSwingingArms(false);
    }
}
