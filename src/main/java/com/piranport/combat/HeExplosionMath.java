package com.piranport.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * HE 爆炸的纯数值规则。
 *
 * <p>原版 {@code Explosion} 用同一个 power 决定半径、方块破坏和实体伤害，无法分别调。
 * 现在的拆法：传给 {@code level().explode} 的 power 只放大半径（{@link #effectivePower}），
 * 实体伤害改由 {@link HeExplosionHandler} 在 {@code ExplosionEvent.Detonate} 里按这里的公式
 * 自行结算（{@link #entityDamage}）。两处都从同一个 effective power 取值，因此半径变化
 * 会自然带动伤害曲线，但伤害倍率可以单独再乘。
 *
 * <p>公式与 1.21.1 原版 {@code ExplosionDamageCalculator#getEntityDamageAmount} 逐项一致：
 * {@code f = power * 2; d = (1 - distance / f) * seenPercent; amount = ((d*d + d) / 2 * 7 * f + 1) * multiplier}。
 */
public final class HeExplosionMath {
    private HeExplosionMath() {}

    /**
     * 实际传给 {@code level().explode} 的爆炸威力。
     *
     * @param basePower        炮弹数据里的 explosionPower（水下爆炸时已乘过水下倍率）
     * @param radiusMultiplier 终端参数 {@code he_explosion_radius_multiplier}
     */
    public static float effectivePower(float basePower, double radiusMultiplier) {
        double scaled = basePower * radiusMultiplier;
        if (!Double.isFinite(scaled) || scaled < 0.0) return 0.0f;
        return (float) scaled;
    }

    /**
     * 与原版一致的爆炸实体伤害（未计入护甲/抗性，由 {@code LivingEntity#hurt} 处理）。
     *
     * @param power            实际爆炸威力（即 {@link net.minecraft.world.level.Explosion#radius()}）
     * @param distance         实体中心到爆心的距离，口径同原版 {@code entity.distanceToSqr(center)}
     * @param seenPercent      原版 {@code Explosion#getSeenPercent} 的可见比例
     * @param damageMultiplier 终端参数 {@code he_entity_damage_multiplier}
     * @return 伤害值；超出半径或倍率为 0 时返回 0
     */
    public static float entityDamage(float power, double distance, float seenPercent,
                                     double damageMultiplier) {
        float f = power * 2.0f;
        if (!(f > 0.0f) || !Double.isFinite(distance) || distance > (double) f) return 0.0f;
        double normalized = 1.0 - distance / (double) f;
        double falloff = normalized * (double) Math.max(0.0f, Math.min(1.0f, seenPercent));
        double amount = ((falloff * falloff + falloff) / 2.0 * 7.0 * (double) f + 1.0)
                * Math.max(0.0, damageMultiplier);
        if (!Double.isFinite(amount) || amount <= 0.0) return 0.0f;
        return (float) amount;
    }

    /**
     * 爆心到实体碰撞箱的最近距离（点已在箱内时为 0）。
     *
     * <p>原版用 {@code entity.distanceToSqr(center)} 量到实体脚底（{@code position()}），
     * 对 1.6×2.38 的舰船会把“命中船体中上部”算成 1–2 格距离，爆炸伤害被严重低估甚至归零。
     * HE 是接触/近失爆炸，按碰撞箱计算才符合直觉：直接命中时爆心就在箱面上，距离为 0，
     * 必定吃到最大伤害。</p>
     */
    public static double distanceToHitbox(Entity entity, Vec3 point) {
        return distanceToHitbox(entity.getBoundingBox(), point);
    }

    /** {@link #distanceToHitbox(Entity, Vec3)} 的纯几何版本，便于单测。 */
    public static double distanceToHitbox(AABB box, Vec3 point) {
        double dx = Math.max(box.minX - point.x, Math.max(0.0, point.x - box.maxX));
        double dy = Math.max(box.minY - point.y, Math.max(0.0, point.y - box.maxY));
        double dz = Math.max(box.minZ - point.z, Math.max(0.0, point.z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 接管本次爆炸的受影响实体：先复制一份供自行结算，再清空原列表阻止原版再次
     * {@code hurt}。方块列表、粒子与音效不受影响（它们不读这个实体列表）。
     */
    public static List<Entity> takeOverAffectedEntities(List<Entity> affected) {
        List<Entity> snapshot = new ArrayList<>(affected);
        affected.clear();
        return snapshot;
    }
}
