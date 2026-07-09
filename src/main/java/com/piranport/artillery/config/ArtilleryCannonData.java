package com.piranport.artillery.config;

import java.util.List;

/** JSON 火炮定义数据模型 */
public record ArtilleryCannonData(
        int caliber,
        int barrels,
        float damage,
        int reloadTime,
        int durability,
        float scopeZoom,
        List<MuzzlePos> muzzles,
        float initialSpeed,
        float dragCoeff,
        float gravity,
        float explosionPower,
        float dispersion,
        int fireCooldown,
        int salvoCount,
        float salvoInterval,
        float projectileWeight,
        float verticalSpread,
        float horizontalSpread,
        float maxElevation,
        float minElevation,
        float turretSpeed
) {
    public static final ArtilleryCannonData DEFAULT = new ArtilleryCannonData(
            14, 1, 6f, 30, 500, 4.0f, List.of(new MuzzlePos(0, 0, 0)),
            3.0f, 0.01f, 9.8f, 1.0f, 0.5f,
            20, 1, 5.0f,
            200.0f, 0.5f, 0.5f, 45.0f, -5.0f, 3.0f
    );

    /** 旧构造器兼容（缺少物理参数时使用默认值） */
    public ArtilleryCannonData {
        if (caliber <= 0) caliber = 1;
        if (barrels <= 0) barrels = 1;
        if (initialSpeed <= 0) initialSpeed = 3.0f;
        if (dragCoeff <= 0) dragCoeff = 0.01f;
        if (gravity <= 0) gravity = 9.8f;
        if (explosionPower <= 0) explosionPower = 1.0f;
        if (dispersion <= 0) dispersion = getDefaultDispersion(caliber);
        if (fireCooldown < 0) fireCooldown = 20;
        if (salvoCount < 1) salvoCount = 1;
        if (salvoInterval < 0) salvoInterval = 5.0f;
        if (projectileWeight <= 0) projectileWeight = getDefaultProjectileWeight(caliber);
        if (verticalSpread <= 0) verticalSpread = dispersion;
        if (horizontalSpread <= 0) horizontalSpread = dispersion;
        if (maxElevation <= 0) maxElevation = getDefaultMaxElevation(caliber);
        if (minElevation >= maxElevation) minElevation = -5.0f;
        if (turretSpeed <= 0) turretSpeed = 3.0f;
    }

    /** 兼容旧代码/旧数据结构的 16 字段构造器。 */
    public ArtilleryCannonData(int caliber,
                               int barrels,
                               float damage,
                               int reloadTime,
                               int durability,
                               float scopeZoom,
                               List<MuzzlePos> muzzles,
                               float initialSpeed,
                               float dragCoeff,
                               float gravity,
                               float explosionPower,
                               float dispersion,
                               int fireCooldown,
                               int salvoCount,
                               float salvoInterval) {
        this(caliber, barrels, damage, reloadTime, durability, scopeZoom, muzzles,
                initialSpeed, dragCoeff, gravity, explosionPower, dispersion,
                fireCooldown, salvoCount, salvoInterval,
                getDefaultProjectileWeight(caliber), dispersion, dispersion,
                getDefaultMaxElevation(caliber), getDefaultMinElevation(caliber), 3.0f);
    }

    /** 根据口径计算默认散布角（度），保持向后兼容 */
    private static float getDefaultDispersion(int caliber) {
        if (caliber <= 4) return 1.5f;
        if (caliber <= 8) return 1.0f;
        return 0.5f;
    }

    private static float getDefaultProjectileWeight(int caliber) {
        if (caliber <= 4) return 50.0f;
        if (caliber <= 8) return 200.0f;
        return 800.0f;
    }

    private static float getDefaultMaxElevation(int caliber) {
        if (caliber <= 4) return 60.0f;
        if (caliber <= 8) return 50.0f;
        return 45.0f;
    }

    private static float getDefaultMinElevation(int caliber) {
        return caliber <= 4 ? -10.0f : -5.0f;
    }

    // compact constructor 无法提供默认值，用静态工厂
    public static ArtilleryCannonData of(int caliber, int barrels, float damage, int reloadTime,
                                          int durability, float scopeZoom, List<MuzzlePos> muzzles) {
        return new ArtilleryCannonData(caliber, barrels, damage, reloadTime, durability,
                scopeZoom, muzzles, 3.0f, 0.01f, 9.8f, 1.0f, 0.0f,
                20, 1, 5.0f);
    }
}
