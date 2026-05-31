package com.piranport.compat.maid.brain;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.piranport.compat.maid.combat.FireControlHelper;
import com.piranport.compat.maid.combat.MaidWeaponFirer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Optional;

/**
 * 舰装任务专用的攻击目标清理。
 *
 * <p>普通自动索敌仍使用女仆 canAttack 与 32 格距离限制；火控锁定目标由玩家主动指定，
 * 只要锁定仍有效就不按普通索敌规则清掉。
 */
public class RiggingStopAttackTask extends Behavior<EntityMaid> {
    private static final float SEARCH_RADIUS = 32f;

    public RiggingStopAttackTask() {
        super(ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return true;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return true;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> current = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (current.isEmpty()) return;

        LivingEntity target = current.get();
        Optional<LivingEntity> fireControlTarget = FireControlHelper.getOwnerFireControlTarget(maid);
        if (fireControlTarget.filter(target::equals).isPresent()) {
            if (isTargetGone(maid, target) || !MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem())) {
                eraseAttackTarget(maid);
            }
            return;
        }

        if (fireControlTarget.isPresent()
                || isTargetGone(maid, target)
                || !MaidWeaponFirer.isOffensiveWeapon(maid.getMainHandItem())
                || maid.distanceTo(target) > SEARCH_RADIUS
                || !maid.canAttack(target)) {
            eraseAttackTarget(maid);
        }
    }

    private static boolean isTargetGone(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || target.isRemoved() || target.level() != maid.level();
    }

    private static void eraseAttackTarget(EntityMaid maid) {
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
