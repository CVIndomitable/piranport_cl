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
        float dispersion
) {
    public static final ArtilleryCannonData DEFAULT = new ArtilleryCannonData(
            14, 1, 6f, 30, 500, 4.0f, List.of(new MuzzlePos(0, 0, 0)),
            3.0f, 0.01f, 9.8f, 1.0f, 0.5f
    );

    /** 旧构造器兼容（缺少物理参数时使用默认值） */
    public ArtilleryCannonData {
        if (initialSpeed <= 0) initialSpeed = 3.0f;
        if (dragCoeff <= 0) dragCoeff = 0.01f;
        if (gravity <= 0) gravity = 9.8f;
        if (explosionPower <= 0) explosionPower = 1.0f;
        if (dispersion <= 0) dispersion = getDefaultDispersion(caliber);
    }

    /** 根据口径计算默认散布角（度），保持向后兼容 */
    private static float getDefaultDispersion(int caliber) {
        if (caliber <= 4) return 1.5f;
        if (caliber <= 8) return 1.0f;
        return 0.5f;
    }

    // compact constructor 无法提供默认值，用静态工厂
    public static ArtilleryCannonData of(int caliber, int barrels, float damage, int reloadTime,
                                          int durability, float scopeZoom, List<MuzzlePos> muzzles) {
        return new ArtilleryCannonData(caliber, barrels, damage, reloadTime, durability,
                scopeZoom, muzzles, 3.0f, 0.01f, 9.8f, 1.0f, 0.0f);
    }
}
