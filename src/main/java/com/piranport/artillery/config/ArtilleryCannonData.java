package com.piranport.artillery.config;

import java.util.List;

/** JSON 火炮定义数据模型 */
public record ArtilleryCannonData(
        int caliber,
        int barrels,
        float damage,
        /** 装填 tick，允许小数（策划决策/数值/07 表含 8.45 等）；开火时按小数概率进位。 */
        float reloadTime,
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
        float turretSpeed,
        /**
         * 装填模式：策划决策/武器/07-火炮装填双模式.md
         * <ul>
         *   <li>"AUTO" — 开火后冷却（FPS 风格，发射即进入装填）</li>
         *   <li>"MANUAL" — 装填时冷却（玩家按 R 键启动读条）</li>
         * </ul>
         * 默认按口径：<=4 = AUTO（与"小口径自动"设计参考一致）；>4 = MANUAL。
         */
        String loadingMode,
        /**
         * 真实口径（英寸），策划决策/数值/06、07。&gt;0 时：
         * 弹药族按英寸判定（&lt;7 小 / 7～&lt;13 中 / ≥13 大），
         * {@code caliber} 档位由英寸派生（4/8/16），damage/reloadTime 缺省时由公式派生，负重走公式。
         * 0 = 旧炮，沿用 int caliber 档位语义。
         */
        float caliberInches,
        /** 稀有度：initial/standard/improved/advanced；只改装填。缺省 standard。 */
        String tier,
        /** 初速档：high/standard/low；只改负重。缺省 standard。 */
        String velocityClass
) {
    public static final ArtilleryCannonData DEFAULT = new ArtilleryCannonData(
            14, 1, 6f, 30, 500, 4.0f, List.of(new MuzzlePos(0, 0, 0)),
            3.0f, 0.01f, 9.8f, 1.0f, 0.5f,
            20, 1, 5.0f,
            200.0f, 0.5f, 0.5f, 45.0f, 3.0f,
            "MANUAL"
    );

    /** 旧构造器兼容（缺少物理参数时使用默认值） */
    public ArtilleryCannonData {
        // 英寸口径炮：档位与面板/装填由策划决策/数值/06 公式派生，先于下面的口径兜底执行。
        if (caliberInches > 0 && Float.isFinite(caliberInches)) {
            caliber = legacyCaliberForInches(caliberInches);
            if (tier == null || tier.isBlank()) tier = "standard";
            if (velocityClass == null || velocityClass.isBlank()) velocityClass = "standard";
            int n = barrels <= 0 ? 1 : barrels;
            if (damage <= 0) damage = com.piranport.artillery.CannonStatFormula.panelDamage(caliberInches);
            if (reloadTime <= 0) {
                reloadTime = (float) com.piranport.artillery.CannonStatFormula.reloadTicks(
                        caliberInches, n, parseTier(tier));
            }
        } else {
            caliberInches = 0f;
        }
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
        if (turretSpeed <= 0) turretSpeed = 3.0f;
        // 装填模式：缺省按口径（<=4 = AUTO, >4 = MANUAL）
        if (loadingMode == null || loadingMode.isBlank()) {
            loadingMode = (caliber <= 4) ? "AUTO" : "MANUAL";
        } else {
            String up = loadingMode.toUpperCase(java.util.Locale.ROOT);
            if (!"AUTO".equals(up) && !"MANUAL".equals(up)) {
                loadingMode = (caliber <= 4) ? "AUTO" : "MANUAL";
            } else {
                loadingMode = up;
            }
        }
    }

    /** 旧 21 字段构造器（无英寸口径/稀有度/初速档）。 */
    public ArtilleryCannonData(int caliber, int barrels, float damage, float reloadTime, int durability,
                               float scopeZoom, List<MuzzlePos> muzzles, float initialSpeed, float dragCoeff,
                               float gravity, float explosionPower, float dispersion, int fireCooldown,
                               int salvoCount, float salvoInterval, float projectileWeight,
                               float verticalSpread, float horizontalSpread, float maxElevation,
                               float turretSpeed, String loadingMode) {
        this(caliber, barrels, damage, reloadTime, durability, scopeZoom, muzzles, initialSpeed, dragCoeff,
                gravity, explosionPower, dispersion, fireCooldown, salvoCount, salvoInterval, projectileWeight,
                verticalSpread, horizontalSpread, maxElevation, turretSpeed, loadingMode, 0f, null, null);
    }

    /**
     * 英寸 → 旧 int 档位（用户 2026-09-30 拍板：&lt;7 小口径、7～&lt;13 中口径、≥13 大口径）。
     * 取 4/8/16 是为了让所有仍读 int 档位的旧判据（弹药族 ≤4/≤8、AP 过穿 &gt;8、默认散布/弹重/仰角、
     * 装填模式 ≤4）对英寸炮给出与新映射一致的结果。
     */
    public static int legacyCaliberForInches(double inches) {
        if (inches < 7.0) return 4;
        if (inches < 13.0) return 8;
        return 16;
    }

    private static com.piranport.component.EquipmentTier parseTier(String id) {
        try {
            return com.piranport.component.EquipmentTier.byId(id);
        } catch (IllegalArgumentException e) {
            return com.piranport.component.EquipmentTier.STANDARD;
        }
    }

    /** 是否按英寸口径公式定义（新注册表火炮）。 */
    public boolean usesInchCaliber() {
        return caliberInches > 0;
    }

    /** 稀有度档；未知值按标准型。 */
    public com.piranport.component.EquipmentTier tierOrDefault() {
        return parseTier(tier == null ? "standard" : tier);
    }

    public com.piranport.artillery.CannonStatFormula.VelocityClass velocityClassOrDefault() {
        return com.piranport.artillery.CannonStatFormula.VelocityClass.parse(velocityClass);
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
                getDefaultMaxElevation(caliber), 3.0f,
                (caliber <= 4) ? "AUTO" : "MANUAL");
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

    // compact constructor 无法提供默认值，用静态工厂
    public static ArtilleryCannonData of(int caliber, int barrels, float damage, int reloadTime,
                                          int durability, float scopeZoom, List<MuzzlePos> muzzles) {
        return new ArtilleryCannonData(caliber, barrels, damage, reloadTime, durability,
                scopeZoom, muzzles, 3.0f, 0.01f, 9.8f, 1.0f, 0.0f,
                20, 1, 5.0f);
    }

    /** 是否为自动装填模式（开火后自动进入装填） */
    public boolean isAutoLoading() {
        return "AUTO".equals(loadingMode);
    }
}
