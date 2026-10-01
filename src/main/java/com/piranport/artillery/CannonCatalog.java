package com.piranport.artillery;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.MuzzlePos;
import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.component.EquipmentTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 火炮注册表目录（依据：策划决策/数值/07-火炮注册表.md，60 门）。
 *
 * <p>只存定位：英寸口径、联装、稀有度。面板/装填/负重由 {@link CannonStatFormula} 派生，
 * 物理参数按口径族沿用现有同族炮。运行时以 {@code data/piranport/artillery/cannons/<id>.json}
 * 为准；{@link #fallbackData} 只是资源未加载时的注册期回退，单测保证两者一致。
 *
 * <p>本表顺序即创造栏顺序（与 07 表一致）。不注册的定位见 07 文末；不写新配方。
 */
public final class CannonCatalog {
    private CannonCatalog() {}

    public record Entry(String id, float caliberInches, int barrels, EquipmentTier tier) {}

    public static final List<Entry> ENTRIES = List.of(
            new Entry("oto_twin_76mm_rapid_gun", 3.0f, 2, EquipmentTier.IMPROVED),
            new Entry("british_twin_3inch_rapid_gun", 3.0f, 2, EquipmentTier.ADVANCED),
            new Entry("type79_twin_100mm_gun", (float) (100 / 25.4), 2, EquipmentTier.IMPROVED),
            new Entry("type79b_twin_100mm_gun", (float) (100 / 25.4), 2, EquipmentTier.ADVANCED),
            new Entry("bofors_twin_120mm_gun", (float) (120 / 25.4), 2, EquipmentTier.IMPROVED),
            new Entry("german_single_127mm_gun", 5.0f, 1, EquipmentTier.INITIAL),
            new Entry("us_single_5inch_gun", 5.0f, 1, EquipmentTier.STANDARD),
            new Entry("japanese_127mm_twin_gun", 5.0f, 2, EquipmentTier.INITIAL),
            new Entry("us_twin_5inch_gun", 5.0f, 2, EquipmentTier.STANDARD),
            new Entry("us_twin_5inch_dp_gun", 5.0f, 2, EquipmentTier.IMPROVED),
            new Entry("twin_5inch_l54_dp_gun", 5.0f, 2, EquipmentTier.ADVANCED),
            new Entry("japanese_single_140mm_gun", (float) (140 / 25.4), 1, EquipmentTier.INITIAL),
            new Entry("british_single_6inch_gun", 6.0f, 1, EquipmentTier.INITIAL),
            new Entry("japanese_single_152mm_gun", 6.0f, 1, EquipmentTier.STANDARD),
            new Entry("british_twin_6inch_gun", 6.0f, 2, EquipmentTier.INITIAL),
            new Entry("japanese_twin_152mm_gun", 6.0f, 2, EquipmentTier.STANDARD),
            new Entry("british_triple_6inch_gun", 6.0f, 3, EquipmentTier.STANDARD),
            new Entry("us_triple_6inch_gun", 6.0f, 3, EquipmentTier.IMPROVED),
            new Entry("mk16_triple_6inch_gun_flashless", 6.0f, 3, EquipmentTier.IMPROVED),
            new Entry("improved_triple_6inch_gun", 6.0f, 3, EquipmentTier.ADVANCED),
            new Entry("triple_152mm_dp_gun_46", 6.0f, 3, EquipmentTier.ADVANCED),
            new Entry("swedish_triple_152mm_gun", 6.0f, 3, EquipmentTier.ADVANCED),
            new Entry("soviet_triple_180mm_gun", 7.0f, 3, EquipmentTier.IMPROVED),
            new Entry("soviet_improved_triple_180mm_gun", 7.0f, 3, EquipmentTier.ADVANCED),
            new Entry("british_twin_8inch_gun", 8.0f, 2, EquipmentTier.INITIAL),
            new Entry("japanese_twin_203mm_gun", 8.0f, 2, EquipmentTier.STANDARD),
            new Entry("german_twin_203mm_gun", 8.0f, 2, EquipmentTier.IMPROVED),
            new Entry("italian_twin_203mm_gun", 8.0f, 2, EquipmentTier.ADVANCED),
            new Entry("us_triple_8inch_gun", 8.0f, 3, EquipmentTier.IMPROVED),
            new Entry("german_triple_203mm_gun", 8.0f, 3, EquipmentTier.ADVANCED),
            new Entry("german_triple_283mm_gun", 11.0f, 3, EquipmentTier.IMPROVED),
            new Entry("italian_triple_305mm_gun", 12.0f, 3, EquipmentTier.STANDARD),
            new Entry("us_triple_12inch_gun", 12.0f, 3, EquipmentTier.IMPROVED),
            new Entry("italian_triple_320mm_gun", 13.0f, 3, EquipmentTier.IMPROVED),
            new Entry("japanese_twin_356mm_gun", 14.0f, 2, EquipmentTier.STANDARD),
            new Entry("us_triple_14inch_gun", 14.0f, 3, EquipmentTier.STANDARD),
            new Entry("us_triple_14inch_gun_kai", 14.0f, 3, EquipmentTier.ADVANCED),
            new Entry("british_quad_14inch_gun", 14.0f, 4, EquipmentTier.IMPROVED),
            new Entry("british_quad_14inch_gun_284", 14.0f, 4, EquipmentTier.ADVANCED),
            new Entry("british_twin_15inch_gun", 15.0f, 2, EquipmentTier.INITIAL),
            new Entry("german_twin_380mm_gun", 15.0f, 2, EquipmentTier.STANDARD),
            new Entry("british_twin_15inch_gun_kai", 15.0f, 2, EquipmentTier.IMPROVED),
            new Entry("italian_triple_381mm_gun", 15.0f, 3, EquipmentTier.IMPROVED),
            new Entry("italian_triple_381mm_gun_kai", 15.0f, 3, EquipmentTier.ADVANCED),
            new Entry("us_twin_16inch_gun", 16.0f, 2, EquipmentTier.INITIAL),
            new Entry("japanese_twin_410mm_gun", 16.0f, 2, EquipmentTier.STANDARD),
            new Entry("us_twin_16inch_gun_kai", 16.0f, 2, EquipmentTier.IMPROVED),
            new Entry("german_twin_406mm_gun", 16.0f, 2, EquipmentTier.ADVANCED),
            new Entry("british_triple_16inch_gun", 16.0f, 3, EquipmentTier.STANDARD),
            new Entry("us_triple_16inch_gun_mk6", 16.0f, 3, EquipmentTier.IMPROVED),
            new Entry("british_triple_16inch_gun_kai", 16.0f, 3, EquipmentTier.IMPROVED),
            new Entry("us_triple_16inch_gun_mk7", 16.0f, 3, EquipmentTier.ADVANCED),
            new Entry("german_skc40_triple_420mm_gun", 16.5f, 3, EquipmentTier.ADVANCED),
            new Entry("british_single_18inch_gun", 18.0f, 1, EquipmentTier.IMPROVED),
            new Entry("british_twin_18inch_gun", 18.0f, 2, EquipmentTier.STANDARD),
            new Entry("us_twin_18inch_gun", 18.0f, 2, EquipmentTier.IMPROVED),
            new Entry("japanese_triple_460mm_gun", 18.0f, 3, EquipmentTier.ADVANCED),
            new Entry("soviet_triple_460mm_gun", 18.0f, 3, EquipmentTier.ADVANCED),
            new Entry("british_twin_20inch_gun", 20.0f, 2, EquipmentTier.STANDARD),
            new Entry("us_twin_20inch_gun", 20.0f, 2, EquipmentTier.ADVANCED)
    );

    public static Optional<Entry> find(String id) {
        for (Entry e : ENTRIES) if (e.id().equals(id)) return Optional.of(e);
        return Optional.empty();
    }

    /** 物理模板：small=日本12.7厘米连装炮、medium=中国双联140毫米炮、large=德国双联380毫米炮。 */
    private record Template(int durability, float scopeZoom, float initialSpeed, float dragCoeff, float gravity,
                            float explosionPower, float dispersion, float projectileWeight,
                            float horizontalSpread, float verticalSpread, float maxElevation, float turretSpeed,
                            double muzzleDx, double muzzleY) {}

    private static final Template SMALL = new Template(500, 2.0f, 2.5f, 0.015f, 9.8f, 1.0f, 1.5f, 50.0f,
            1.5f, 1.0f, 60.0f, 3.5f, 0.3, 0.2);
    private static final Template MEDIUM = new Template(1000, 3.0f, 3.0f, 0.01f, 9.8f, 1.5f, 1.0f, 200.0f,
            0.8f, 0.8f, 50.0f, 3.0f, 0.3, 0.2);
    private static final Template LARGE = new Template(2500, 4.0f, 3.5f, 0.008f, 9.8f, 2.0f, 0.5f, 800.0f,
            0.4f, 0.4f, 40.0f, 1.2f, 0.2, 0.3);

    private static Template template(float inches) {
        return switch (CannonAmmoRules.familyForInches(inches)) {
            case SMALL -> SMALL;
            case MEDIUM -> MEDIUM;
            case LARGE -> LARGE;
        };
    }

    /** 炮口沿 x 轴等距排开、居中；与生成 JSON 的规则相同。 */
    static List<MuzzlePos> muzzles(int barrels, double dx, double y) {
        List<MuzzlePos> list = new ArrayList<>(barrels);
        for (int i = 0; i < barrels; i++) {
            double x = Math.round(((barrels - 1) / 2.0 - i) * dx * 1000.0) / 1000.0;
            list.add(new MuzzlePos(x + 0.0, y, 0));
        }
        return List.copyOf(list);
    }

    /** 注册期回退数据；damage/reloadTime 传 0，由 ArtilleryCannonData 按公式派生。 */
    public static ArtilleryCannonData fallbackData(Entry e) {
        Template t = template(e.caliberInches());
        int n = e.barrels();
        return new ArtilleryCannonData(0, n, 0f, 0f, t.durability(), t.scopeZoom(),
                muzzles(n, t.muzzleDx(), t.muzzleY()), t.initialSpeed(), t.dragCoeff(), t.gravity(),
                t.explosionPower(), t.dispersion(), 0, n, n > 1 ? 3.0f : 0.0f, t.projectileWeight(),
                t.verticalSpread(), t.horizontalSpread(), t.maxElevation(), t.turretSpeed(), "MANUAL",
                e.caliberInches(), e.tier().id(), "standard");
    }

    public static ArtilleryCannonData fallbackData(String id) {
        return fallbackData(find(id).orElseThrow(() -> new IllegalArgumentException("Unknown cannon " + id)));
    }
}
