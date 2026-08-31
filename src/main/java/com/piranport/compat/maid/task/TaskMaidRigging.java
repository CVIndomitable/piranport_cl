package com.piranport.compat.maid.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.piranport.compat.maid.brain.RiggingFireControlTargetTask;
import com.piranport.compat.maid.brain.RiggingMovementTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.piranport.compat.maid.brain.RiggingShootTask;
import com.piranport.compat.maid.brain.RiggingStopAttackTask;
import com.piranport.compat.maid.combat.MaidWeaponFirer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class TaskMaidRigging implements IRangedAttackTask {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("piranport", "maid_rigging");
    private static final float SEARCH_RADIUS = 32f;
    private static final TargetingConditions SIGHT_CONDITIONS = TargetingConditions.forCombat().range(SEARCH_RADIUS);
    private static final AtomicReference<ItemStack> ICON = new AtomicReference<>(ItemStack.EMPTY);

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("task.piranport.maid_rigging.name");
    }

    @Override
    public List<String> getDescription(EntityMaid maid) {
        return List.of("task.piranport.maid_rigging.desc");
    }

    @Override
    public ItemStack getIcon() {
        // Registry initialization and maid task registration can occur on different threads,
        // so publish the resolved stack atomically instead of caching it in a plain field.
        ICON.updateAndGet(current -> {
            if (!current.isEmpty()) return current;

            Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("piranport", "medium_cannon"));
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        });
        return ICON.get();
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundEvents.ARMOR_EQUIP_IRON.value();
    }

    @Override
    public boolean enableLookAndRandomWalk(EntityMaid maid) {
        return false;
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return MaidWeaponFirer.isSupported(stack);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> fireControlTarget = new RiggingFireControlTargetTask();
        BehaviorControl<EntityMaid> startAttack = StartAttacking.create(
                this::hasOffensiveWeapon,
                TaskMaidRigging::findTarget);
        BehaviorControl<EntityMaid> stopAttack = new RiggingStopAttackTask();
        BehaviorControl<EntityMaid> walkToTarget = new RiggingMovementTask();
        BehaviorControl<EntityMaid> shoot = new RiggingShootTask();

        return Lists.newArrayList(
                Pair.of(4, fireControlTarget),
                Pair.of(5, startAttack),
                Pair.of(5, stopAttack),
                Pair.of(5, walkToTarget),
                Pair.of(5, shoot)
        );
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> fireControlTarget = new RiggingFireControlTargetTask();
        BehaviorControl<EntityMaid> startAttack = StartAttacking.create(
                this::hasOffensiveWeapon,
                TaskMaidRigging::findTarget);
        BehaviorControl<EntityMaid> stopAttack = new RiggingStopAttackTask();
        BehaviorControl<EntityMaid> shoot = new RiggingShootTask();
        return Lists.newArrayList(
                Pair.of(4, fireControlTarget),
                Pair.of(5, startAttack),
                Pair.of(5, stopAttack),
                Pair.of(5, shoot)
        );
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float distanceFactor) {
        MaidWeaponFirer.fire(maid, target);
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        if (hasOffensiveWeapon(maid)) {
            if (maid.hasRestriction()) {
                return new AABB(maid.getRestrictCenter()).inflate(SEARCH_RADIUS);
            }
            return maid.getBoundingBox().inflate(SEARCH_RADIUS);
        }
        return IRangedAttackTask.super.searchDimension(maid);
    }

    @Override
    public float searchRadius(EntityMaid maid) {
        return SEARCH_RADIUS;
    }

    @Override
    public boolean canSee(EntityMaid maid, LivingEntity target) {
        return SIGHT_CONDITIONS.test(maid, target);
    }

    private boolean hasOffensiveWeapon(EntityMaid maid) {
        return MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem());
    }

    private static Optional<? extends LivingEntity> findTarget(EntityMaid maid) {
        return IRangedAttackTask.findFirstValidAttackTarget(maid);
    }
}
