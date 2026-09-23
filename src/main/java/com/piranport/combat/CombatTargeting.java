package com.piranport.combat;

import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 共享的敌对判定 —— 近防炮与火控雷达共用。
 *
 * <p>WHY 需要下沉到这里：{@code AutoCIWSItem} 原判据末尾是
 * {@code (FlyingMob || Phantom || Vex)}，只认「空中敌人」，导致对地/对海完全漏判。
 * 深海敌人 {@code AbstractDeepOceanEntity extends Monster}，也就是 {@code Enemy}，
 * 但它不属于上述三者，近防炮因此对深海地面单位视而不见。
 * 此处统一为「敌对生物一律算」，供两套系统共用同一口径，避免判据再次漂移。
 */
public final class CombatTargeting {

    private CombatTargeting() {
        // 纯静态工具类，禁止实例化
    }

    /**
     * 判断 target 是否为 player 的敌对目标（敌方飞机或敌对生物）。
     *
     * @param player 判定发起者（玩家）
     * @param target 待判定实体
     * @return true 表示可以攻击/吸附
     */
    public static boolean isHostileTarget(Player player, Entity target) {
        if (!target.isAlive() || target == player || target.isAlliedTo(player)) return false;

        // 飞机分支：敌机包括敌对生物、深海自主飞机和非友军玩家飞机
        if (target instanceof AircraftEntity aircraft) {
            if (aircraft.getOwnerUUID() != null && aircraft.getOwnerUUID().equals(player.getUUID())) return false;
            Player owner = aircraft.getOwner();
            if (owner != null) return player.canHarmPlayer(owner) && !player.isAlliedTo(owner);
            return aircraft.isAutonomous();
        }

        // 生物分支：敌对生物一律算（含深海地面单位），不再只认空中敌人
        if (target instanceof LivingEntity living && living instanceof Enemy) return true;

        return false;
    }
}
