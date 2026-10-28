package com.piranport.entity;

import com.piranport.aviation.SelfDefense;
import com.piranport.aviation.SelfDefenseZone;
import com.piranport.combat.CombatTargeting;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.terminal.TerminalParameters;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * 自卫机枪：按机型启用的半球分区，在不改变航线的前提下射击敌对空中单位。
 *
 * <p>四个开关分两组：上/下是一组，前/后是一组。目标必须同时落在
 * 已启用的垂直半空间和水平半空间内才会被射击，因此“上半球 + 前半球 + 后半球”
 * 等价于“除下半球外全覆盖”。
 *
 * <p>参数每次射击前从调试终端读取，终端修改立即生效。
 */
final class AircraftSelfDefense {
    private AircraftSelfDefense() {}

    static void tick(AircraftEntity craft, @Nullable Player owner) {
        if (owner == null) return;
        SelfDefense config = craft.getResolvedDefinition().selfDefense();
        if (config == null) return;

        if (craft.selfDefenseCooldown > 0) {
            craft.selfDefenseCooldown--;
            return;
        }

        String key = "aircraft." + craft.getAircraftDefinitionId() + ".";
        boolean upper = TerminalParameters.getBoolean(key + "self_defense_upper",
                config.covers(SelfDefenseZone.UPPER));
        boolean lower = TerminalParameters.getBoolean(key + "self_defense_lower",
                config.covers(SelfDefenseZone.LOWER));
        boolean front = TerminalParameters.getBoolean(key + "self_defense_front",
                config.covers(SelfDefenseZone.FRONT));
        boolean rear = TerminalParameters.getBoolean(key + "self_defense_rear",
                config.covers(SelfDefenseZone.REAR));
        double range = TerminalParameters.getDouble(key + "self_defense_range", config.range());
        int fireCooldown = TerminalParameters.getInt(key + "self_defense_fire_cooldown",
                config.fireCooldown());
        float damage = (float) TerminalParameters.getDouble(key + "self_defense_damage",
                config.damage());

        Vec3 forward = horizontalForward(craft);
        AABB box = craft.getBoundingBox().inflate(Math.max(0.0, range));
        AircraftEntity target = craft.level().getEntitiesOfClass(AircraftEntity.class, box,
                        candidate -> candidate != craft && candidate.isAlive()
                                && candidate.distanceTo(craft) <= range
                                && CombatTargeting.isHostileTarget(owner, candidate)
                                && covered(craft, candidate, forward, upper, lower, front, rear))
                .stream()
                .min(Comparator.comparingDouble(craft::distanceToSqr))
                .orElse(null);
        if (target == null) return;

        Vec3 direction = target.getEyePosition().subtract(craft.position());
        if (direction.lengthSqr() < 1.0e-6) return;
        direction = direction.normalize();

        BulletEntity bullet = new BulletEntity(craft.level(), damage);
        bullet.moveTo(craft.getX(), craft.getY() + 0.3, craft.getZ(),
                bullet.getYRot(), bullet.getXRot());
        bullet.setDeltaMovement(direction.scale(ModEquipmentConfig.FIGHTER_BULLET_SPEED.get()));
        bullet.setOwner(owner);
        // 命中提示的武器名要用载机物品名（如「飞行圆饼」），不能用实体类型名（「战斗机」）。
        bullet.setSourceAircraftName(craft.displayName());
        bullet.setSourceAircraft(craft);
        craft.level().addFreshEntity(bullet);
        craft.selfDefenseCooldown = Math.max(1, fireCooldown);
    }

    private static Vec3 horizontalForward(AircraftEntity craft) {
        double yaw = Math.toRadians(craft.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    /**
     * 目标是否落在覆盖范围内。
     *
     * <p>上/下按世界垂直方向判定，前/后按机体水平朝向判定；两组都必须命中。
     * 等高/正前等边界值归入 upper/front，避免出现无法命中的缝隙。
     */
    static boolean covered(AircraftEntity craft, AircraftEntity target, Vec3 forward,
                           boolean upper, boolean lower, boolean front, boolean rear) {
        return covered(craft.position(), forward, target.position(), upper, lower, front, rear);
    }

    /** 纯几何版本，便于单元测试：目标必须同时命中垂直与水平两组半空间。 */
    static boolean covered(Vec3 craftPos, Vec3 forward, Vec3 targetPos,
                           boolean upper, boolean lower, boolean front, boolean rear) {
        Vec3 to = targetPos.subtract(craftPos);
        boolean vertical = to.y >= 0.0 ? upper : lower;
        double dot = forward.x * to.x + forward.z * to.z;
        boolean horizontal = dot >= 0.0 ? front : rear;
        return vertical && horizontal;
    }
}
