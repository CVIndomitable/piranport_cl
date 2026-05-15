package com.piranport.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 皮兰港所有弹药的友军伤害判定逻辑。
 * 返回 true 表示该弹药应跳过对此目标的命中。
 *
 * 决策树（首次匹配即返回）：
 *   1. 深海实体 → 深海实体    → 拦截（禁止自相残杀）
 *   2. 女仆 → 自身             → 拦截（禁止自伤）
 *   3. 女仆 → 同主人女仆        → 拦截（禁止友伤）
 *   4. 玩家 → 玩家（友伤关闭）   → 拦截（配置开关）
 *   5. 玩家 → 自己的飞机        → 始终拦截
 *   6. 玩家 → 他人飞机（友伤关闭）→ 拦截
 *   7. 自主飞机 → 自主飞机      → 拦截（禁止AI飞机互相残杀）
 *   8. 以上都不匹配             → 放行（允许命中）
 */
public final class FriendlyFireHelper {

    private FriendlyFireHelper() {}

    /**
     * @param target 弹药将要命中的实体
     * @param owner  发射弹药的实体（可为 null）
     * @return true 表示应拦截（目标为友方）
     */
    public static boolean shouldBlockHit(Entity target, Entity owner) {
        // 1. 深海实体禁止互相残杀
        if (target instanceof AbstractDeepOceanEntity && owner instanceof AbstractDeepOceanEntity) {
            return true;
        }
        // 2. 女仆禁止被自己的弹药命中
        if (target instanceof EntityMaid && owner instanceof EntityMaid && target == owner) {
            return true;
        }
        // 3. 同主人女仆禁止互相伤害
        if (target instanceof EntityMaid targetMaid && owner instanceof EntityMaid ownerMaid) {
            LivingEntity targetOwner = targetMaid.getOwner();
            LivingEntity ownerOwner = ownerMaid.getOwner();
            if (targetOwner != null && ownerOwner != null && targetOwner.getUUID().equals(ownerOwner.getUUID())) {
                return true;
            }
        }
        // 4. 玩家友伤关闭时禁止攻击其他玩家
        if (!ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()
                && target instanceof Player && owner instanceof Player) {
            return true;
        }
        // 5-6. 飞机友伤保护
        if (target instanceof AircraftEntity aircraft && aircraft.getOwnerUUID() != null) {
            if (owner instanceof Player p && p.getUUID().equals(aircraft.getOwnerUUID())) {
                return true; // 始终保护自己的飞机
            }
            if (owner instanceof AircraftEntity ownerAir && ownerAir.getOwnerUUID() != null
                    && ownerAir.getOwnerUUID().equals(aircraft.getOwnerUUID())) {
                return true; // 自航飞机保护同主人的玩家飞机
            }
            if (!ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()) {
                if (owner instanceof Player) {
                    return true; // 友伤关闭时玩家保护他人飞机
                }
                if (owner instanceof AircraftEntity ownerAir && ownerAir.getOwnerUUID() != null) {
                    return true; // 友伤关闭时自航飞机保护所有玩家飞机
                }
            }
        }
        // 5b. 玩家飞机友伤保护（自航飞机攻击玩家飞机的情况）
        if (target instanceof AircraftEntity targetAir && targetAir.getOwnerUUID() != null
                && owner instanceof AircraftEntity ownerAir && ownerAir.isAutonomous()
                && !ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()) {
            return true; // 友伤关闭时自航飞机不能攻击任何玩家飞机
        }
        // 7. 自主飞机之间禁止互相残杀
        if (target instanceof AircraftEntity targetAir && targetAir.isAutonomous()
                && owner instanceof AircraftEntity ownerAir && ownerAir.isAutonomous()
                && owner != target) {
            return true;
        }
        // 8. 以上都不匹配，允许命中
        return false;
    }
}
