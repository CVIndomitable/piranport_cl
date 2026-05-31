package com.piranport.compat.maid.brain;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.piranport.compat.maid.combat.FireControlHelper;
import com.piranport.compat.maid.combat.MaidWeaponFirer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Optional;

/**
 * 将主人的火控锁定直接接管为女仆攻击目标。
 *
 * <p>原版 StartAttacking/StopAttackingIfTargetInvalid 会调用 canAttack，
 * 会把玩家、友方或超出普通索敌距离的测试目标过滤掉；火控锁定是玩家主动指令，
 * 因此在这里单独写入攻击记忆。
 */
public class RiggingFireControlTargetTask extends Behavior<EntityMaid> {
    public RiggingFireControlTargetTask() {
        super(ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem())
                && FireControlHelper.getOwnerFireControlTarget(maid).isPresent();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return checkExtraStartConditions(level, maid);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        syncTarget(maid);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        syncTarget(maid);
    }

    private static void syncTarget(EntityMaid maid) {
        Optional<LivingEntity> target = FireControlHelper.getOwnerFireControlTarget(maid);
        if (target.isEmpty()) return;
        LivingEntity living = target.get();
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, living);
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(living, true));
    }
}
