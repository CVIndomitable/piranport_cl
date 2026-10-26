package com.piranport.combat;

import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HE 爆炸解耦的纯数值测试。
 *
 * <p>口径：半径倍率只改传给 {@code level().explode} 的 power；实体伤害用同一 power
 * 按原版 {@code ExplosionDamageCalculator#getEntityDamageAmount} 的公式重算，最后再乘
 * 单独的实体伤害倍率；Detonate 阶段必须清空原版实体列表，否则会双倍结算。
 */
class HeExplosionMathTest {

    @Test
    void radiusMultiplierScalesExplosionPower() {
        assertEquals(1.5f, HeExplosionMath.effectivePower(1.5f, 1.0), 1.0e-6f,
                "默认倍率 1.0 不能改变原威力");
        assertEquals(3.0f, HeExplosionMath.effectivePower(1.5f, 2.0), 1.0e-6f);
        assertEquals(0.15f, HeExplosionMath.effectivePower(1.5f, 0.1), 1.0e-6f);
    }

    @Test
    void effectivePowerRejectsInvalidInput() {
        assertEquals(0.0f, HeExplosionMath.effectivePower(-1.0f, 2.0), 0.0f);
        assertEquals(0.0f, HeExplosionMath.effectivePower(1.5f, -1.0), 0.0f);
        assertEquals(0.0f, HeExplosionMath.effectivePower(Float.NaN, 2.0), 0.0f);
        assertEquals(0.0f, HeExplosionMath.effectivePower(1.5f, Double.POSITIVE_INFINITY), 0.0f);
    }

    @Test
    void centreDamageMatchesVanillaFormula() {
        // power 1.5 -> f = 3；distance 0、全可见：((1+1)/2 * 7 * 3 + 1) = 22
        assertEquals(22.0f, HeExplosionMath.entityDamage(1.5f, 0.0, 1.0f, 1.0), 1.0e-4f);
        // 半径倍率把 power 抬到 3.0 -> f = 6：(1 * 7 * 6 + 1) = 43
        assertEquals(43.0f, HeExplosionMath.entityDamage(3.0f, 0.0, 1.0f, 1.0), 1.0e-4f);
    }

    @Test
    void damagedMultiplierOnlyScalesEntityDamage() {
        float base = HeExplosionMath.entityDamage(1.5f, 0.0, 1.0f, 1.0);
        assertEquals(base * 2.5f, HeExplosionMath.entityDamage(1.5f, 0.0, 1.0f, 2.5), 1.0e-4f);
        assertEquals(0.0f, HeExplosionMath.entityDamage(1.5f, 0.0, 1.0f, 0.0), 0.0f,
                "倍率 0 表示 HE 爆炸不伤实体");
    }

    @Test
    void distanceFalloffMatchesVanillaCurve() {
        // f = 3：距离 1.5 时 falloff = 0.5 -> (0.25 + 0.5)/2 * 21 + 1 = 8.875
        assertEquals(8.875f, HeExplosionMath.entityDamage(1.5f, 1.5, 1.0f, 1.0), 1.0e-4f);
        // 半径边缘 falloff = 0，原版仍保底 1 点伤害
        assertEquals(1.0f, HeExplosionMath.entityDamage(1.5f, 3.0, 1.0f, 1.0), 1.0e-4f);
        // 超出 2 * power 不在原版受影响范围
        assertEquals(0.0f, HeExplosionMath.entityDamage(1.5f, 3.0001, 1.0f, 1.0), 0.0f);
    }

    @Test
    void blockedLineOfSightStillDealsVanillaMinimum() {
        // 原版 1.21.1 即使完全被挡住也保底 1 点；倍率照常作用
        assertEquals(1.0f, HeExplosionMath.entityDamage(1.5f, 0.5, 0.0f, 1.0), 1.0e-4f);
        assertEquals(2.0f, HeExplosionMath.entityDamage(1.5f, 0.5, 0.0f, 2.0), 1.0e-4f);
    }

    @Test
    void hitboxDistanceTreatsDirectHitAsZero() {
        // 舰船碰撞箱：宽 1.6、高 2.38（深海轻巡）。爆心落在箱面/箱内时距离必须为 0，
        // 不能像原版量脚底那样把命中上甲板算成约 1.2 格。
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                0.0, 0.0, 0.0, 1.6, 2.38, 1.6);
        assertEquals(0.0, HeExplosionMath.distanceToHitbox(
                box, new net.minecraft.world.phys.Vec3(0.8, 2.2, 0.8)), 1.0e-9);
        assertEquals(0.0, HeExplosionMath.distanceToHitbox(
                box, new net.minecraft.world.phys.Vec3(1.6, 1.0, 0.8)), 1.0e-9);

        // 箱外 2 格：几何最近距离
        assertEquals(2.0, HeExplosionMath.distanceToHitbox(
                box, new net.minecraft.world.phys.Vec3(3.6, 1.0, 0.8)), 1.0e-9);
        // 斜向：Δx=3、Δy=1 -> sqrt(10)
        assertEquals(Math.sqrt(10.0), HeExplosionMath.distanceToHitbox(
                box, new net.minecraft.world.phys.Vec3(4.6, 3.38, 0.8)), 1.0e-9);
    }

    @Test
    void takeOverCopiesThenClearsAffectedEntities() {
        List<Entity> affected = new ArrayList<>();
        affected.add(null); // 只验证列表契约，不需要真实实体
        List<Entity> snapshot = HeExplosionMath.takeOverAffectedEntities(affected);

        assertEquals(1, snapshot.size(), "快照保留原列表内容供自行结算");
        assertTrue(affected.isEmpty(), "原列表必须清空，阻止原版 Explosion 再次 hurt");
    }
}
