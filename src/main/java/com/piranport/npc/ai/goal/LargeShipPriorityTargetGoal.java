package com.piranport.npc.ai.goal;

import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * 航母 / 战列 / 旗舰专用索敌：索敌半径内存在「大型船」玩家时优先锁定，
 * 没有大船时让位给低优先级的 {@code NearestAttackableTargetGoal} 最近玩家兜底。
 *
 * <p>依据《深海/01》§七：默认最近目标，航母/战列优先锁定大型船（呼应大船扛线职能）。
 * 大型船口径与 {@code LargeShipImmunityHandler} 一致：已变身且核心类型为
 * {@link ShipType#LARGE}。</p>
 */
public class LargeShipPriorityTargetGoal extends Goal {

    /** 扫描节流：每 10 tick 最多找一次大船，避免每 tick 遍历玩家列表。 */
    private static final int SCAN_INTERVAL = 10;

    private final AbstractDeepOceanEntity mob;
    private int nextScanTick;
    /** 本 goal 亲自锁定的目标，用于 stop() 时只清理自己的目标，不误伤编队共享目标。 */
    @Nullable
    private LivingEntity priorityTarget;

    public LargeShipPriorityTargetGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) return false;
        // 已经锁着有效大船：保持 goal 运行，持续占用 TARGET flag，
        // 防止低优先级的最近玩家索敌把目标抢走。
        LivingEntity current = mob.getTarget();
        if (isValidLargeTarget(current)) {
            priorityTarget = current;
            return true;
        }
        if (mob.tickCount < nextScanTick) return false;
        nextScanTick = mob.tickCount + SCAN_INTERVAL;

        Player large = findNearestLargePlayer();
        if (large == null) return false;
        priorityTarget = large;
        mob.setTarget(large);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return isValidLargeTarget(mob.getTarget());
    }

    @Override
    public void stop() {
        // 只交还本 goal 自己锁定的目标；若目标已被 FleetAlertGoal 等改成别的实体，
        // 不要顺手清掉对方刚设置的目标。
        if (priorityTarget != null && mob.getTarget() == priorityTarget
                && !isValidLargeTarget(priorityTarget)) {
            mob.setTarget(null);
        }
        priorityTarget = null;
    }

    private boolean isValidLargeTarget(@Nullable LivingEntity target) {
        if (!(target instanceof Player player)) return false;
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return false;
        if (!isLargeShip(player)) return false;
        if (!mob.getSensing().hasLineOfSight(player)) return false;
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return mob.distanceToSqr(player) <= range * range;
    }

    @Nullable
    private Player findNearestLargePlayer() {
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        double bestDistance = range * range;
        Player best = null;
        for (Player player : mob.level().getEntitiesOfClass(Player.class,
                mob.getBoundingBox().inflate(range), this::isValidLargeTarget)) {
            double distance = mob.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private boolean isLargeShip(Player player) {
        ItemStack core = TransformationManager.findTransformedCore(player);
        return core.getItem() instanceof ShipCoreItem coreItem
                && coreItem.getShipType() == ShipType.LARGE;
    }
}
