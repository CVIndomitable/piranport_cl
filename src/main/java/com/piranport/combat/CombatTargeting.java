package com.piranport.combat;

import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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
     * @return true 表示可以攻击
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

    /**
     * 火控统一瞄准点 —— 取目标眼睛位置。
     *
     * <p>WHY 用眼睛高度而不是「碰撞箱高度百分比」：
     * <ol>
     *   <li>原版判断「眼睛是否浸在液体里」（{@code isEyeInFluid} / 窒息、溺水提示的那个位置）
     *       用的就是 {@code getEyeY()}，火控瞄点对齐到同一处，才和玩家看到的「瞄准目标的头」
     *       这一直觉一致。</li>
     *   <li>此前各调用点各写一套 {@code getBbHeight() * 0.4~0.5}，HUD 预瞄圈与实际弹道会
     *       按不同百分比取高，出现「圈画在头高、炮弹打腰线」的不一致。此处收敛为唯一真源，
     *       所有火控路径共用同一口径，百分比不再漂移。</li>
     * </ol>
     *
     * <p><b>必须用 {@link Entity#getEyeY()}</b>：目标可能是 {@code AircraftEntity}，它并不继承
     * {@code LivingEntity}，而 {@code getEyeY()} 定义在 {@link Entity} 上（{@code LivingEntity}
     * 上那些眼睛相关 API 对飞机不可用）。返回值与 {@code position()} 同源，均直接读实体的
     * {@code getX/getY/getZ}，不含任何插值。
     *
     * @param target 瞄准的目标实体
     * @return 目标眼睛位置的世界坐标
     */
    public static Vec3 aimPoint(Entity target) {
        return new Vec3(target.getX(), target.getEyeY(), target.getZ());
    }
}
