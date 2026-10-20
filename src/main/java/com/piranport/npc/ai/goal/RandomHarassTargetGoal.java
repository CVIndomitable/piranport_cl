package com.piranport.npc.ai.goal;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 《深海/01》§七：驱逐随机骚扰，不锁定单一目标。
 *
 * <p>作为 TARGET 目标参与优先级仲裁，而不是在 customServerAiStep 里直接 setTarget：
 * 最近玩家的索敌目标在未运行时会随机抢回目标，直接写目标会被立刻覆盖。</p>
 */
public class RandomHarassTargetGoal extends Goal {

    /** 每次换人的间隔（tick），沿用原骚扰节奏。 */
    private static final int MIN_INTERVAL = 60;
    private static final int MAX_INTERVAL = 120;
    /** 候选扫描节流，避免每 tick 遍历玩家列表。 */
    private static final int SCAN_INTERVAL = 10;

    private final AbstractDeepOceanEntity mob;
    private int nextScanTick;
    private int reselectAt;
    /** 本 goal 亲自挑选的目标，stop() 时只清理自己设置的失效目标。 */
    @Nullable
    private LivingEntity picked;

    public RandomHarassTargetGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) return false;
        if (mob.tickCount < nextScanTick) return false;
        nextScanTick = mob.tickCount + SCAN_INTERVAL;

        List<Player> candidates = findCandidates();
        if (candidates.isEmpty()) {
            // 没有候选时清掉已经失效的玩家目标，避免驱逐继续追着死亡或离开的玩家。
            if (mob.getTarget() instanceof Player stale && !isValidTarget(stale)) {
                mob.setTarget(null);
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.tickCount < reselectAt && isValidTarget(mob.getTarget());
    }

    @Override
    public void start() {
        reselectAt = mob.tickCount + MIN_INTERVAL + mob.getRandom().nextInt(MAX_INTERVAL - MIN_INTERVAL + 1);
        List<Player> candidates = findCandidates();
        if (candidates.isEmpty()) return;
        // 多候选时排除当前目标，确保真正换人而不是反复锁同一艘。
        if (mob.getTarget() instanceof Player current && candidates.size() > 1) {
            candidates.remove(current);
        }
        picked = candidates.get(mob.getRandom().nextInt(candidates.size()));
        mob.setTarget(picked);
    }

    @Override
    public void stop() {
        if (picked != null && mob.getTarget() == picked && !isValidTarget(picked)) {
            mob.setTarget(null);
        }
        picked = null;
        // 窗口到期后下一 tick 立即重新挑选，不等待扫描节流。
        nextScanTick = 0;
    }

    private List<Player> findCandidates() {
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Player> result = new ArrayList<>();
        for (Player player : mob.level().getEntitiesOfClass(Player.class,
                mob.getBoundingBox().inflate(range), this::isValidTarget)) {
            result.add(player);
        }
        return result;
    }

    private boolean isValidTarget(@Nullable LivingEntity target) {
        if (!(target instanceof Player player)) return false;
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return false;
        if (!mob.canAttack(player)) return false;
        if (!mob.getSensing().hasLineOfSight(player)) return false;
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return mob.distanceToSqr(player) <= range * range;
    }
}
