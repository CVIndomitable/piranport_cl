package com.piranport.artillery;

import com.google.gson.Gson;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.combat.cannon.CannonAmmoRules.CaliberFamily;
import com.piranport.component.EquipmentTier;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策划决策/数值/06（公式）与 07（注册表）的对表测试。
 * 期望值逐行抄自 07 表（面板/齐射/装填 tick/DPS/负重），不是用公式反算的。
 */
class CannonStatFormulaTest {

    private record Row(String id, int panel, int salvo, double reload, double dps, int weight) {}

    private static final List<Row> TABLE_07 = List.of(
            new Row("oto_twin_76mm_rapid_gun", 2, 4, 8.45, 9.47, 7),
            new Row("british_twin_3inch_rapid_gun", 2, 4, 6.5, 12.31, 7),
            new Row("type79_twin_100mm_gun", 3, 6, 16.37, 7.33, 9),
            new Row("type79b_twin_100mm_gun", 3, 6, 12.59, 9.53, 9),
            new Row("bofors_twin_120mm_gun", 5, 10, 23.02, 8.69, 12),
            new Row("german_single_127mm_gun", 5, 5, 30.0, 3.33, 8),
            new Row("us_single_5inch_gun", 5, 5, 24.0, 4.17, 8),
            new Row("japanese_127mm_twin_gun", 5, 10, 39.0, 5.13, 12),
            new Row("us_twin_5inch_gun", 5, 10, 31.2, 6.41, 12),
            new Row("us_twin_5inch_dp_gun", 5, 10, 25.35, 7.89, 12),
            new Row("twin_5inch_l54_dp_gun", 5, 10, 19.5, 10.26, 12),
            new Row("japanese_single_140mm_gun", 6, 6, 35.1, 3.42, 10),
            new Row("british_single_6inch_gun", 7, 7, 40.0, 3.5, 11),
            new Row("japanese_single_152mm_gun", 7, 7, 32.0, 4.38, 11),
            new Row("british_twin_6inch_gun", 7, 14, 52.0, 5.38, 16),
            new Row("japanese_twin_152mm_gun", 7, 14, 41.6, 6.73, 16),
            new Row("british_triple_6inch_gun", 7, 21, 51.2, 8.2, 21),
            new Row("us_triple_6inch_gun", 7, 21, 41.6, 10.1, 21),
            new Row("mk16_triple_6inch_gun_flashless", 7, 21, 41.6, 10.1, 21),
            new Row("improved_triple_6inch_gun", 7, 21, 32.0, 13.13, 21),
            new Row("triple_152mm_dp_gun_46", 7, 21, 32.0, 13.13, 21),
            new Row("swedish_triple_152mm_gun", 7, 21, 32.0, 13.13, 21),
            new Row("soviet_triple_180mm_gun", 9, 27, 52.0, 10.38, 25),
            new Row("soviet_improved_triple_180mm_gun", 9, 27, 40.0, 13.5, 25),
            new Row("british_twin_8inch_gun", 12, 24, 78.0, 6.15, 23),
            new Row("japanese_twin_203mm_gun", 12, 24, 62.4, 7.69, 23),
            new Row("german_twin_203mm_gun", 12, 24, 50.7, 9.47, 23),
            new Row("italian_twin_203mm_gun", 12, 24, 39.0, 12.31, 23),
            new Row("us_triple_8inch_gun", 12, 36, 62.4, 11.54, 30),
            new Row("german_triple_203mm_gun", 12, 36, 48.0, 15.0, 30),
            new Row("german_triple_283mm_gun", 19, 57, 93.6, 12.18, 46),
            new Row("italian_triple_305mm_gun", 22, 66, 128.0, 10.31, 52),
            new Row("us_triple_12inch_gun", 22, 66, 104.0, 12.69, 52),
            new Row("italian_triple_320mm_gun", 25, 75, 114.4, 13.11, 59),
            new Row("japanese_twin_356mm_gun", 28, 56, 124.8, 8.97, 49),
            new Row("us_triple_14inch_gun", 28, 84, 153.6, 10.94, 65),
            new Row("us_triple_14inch_gun_kai", 28, 84, 96.0, 17.5, 65),
            new Row("british_quad_14inch_gun", 28, 112, 148.2, 15.11, 82),
            new Row("british_quad_14inch_gun_284", 28, 112, 114.0, 19.65, 82),
            new Row("british_twin_15inch_gun", 31, 62, 169.0, 7.34, 54),
            new Row("german_twin_380mm_gun", 31, 62, 135.2, 9.17, 54),
            new Row("british_twin_15inch_gun_kai", 31, 62, 109.85, 11.29, 54),
            new Row("italian_triple_381mm_gun", 31, 93, 135.2, 13.76, 72),
            new Row("italian_triple_381mm_gun_kai", 31, 93, 104.0, 17.88, 72),
            new Row("us_twin_16inch_gun", 35, 70, 182.0, 7.69, 60),
            new Row("japanese_twin_410mm_gun", 35, 70, 145.6, 9.62, 60),
            new Row("us_twin_16inch_gun_kai", 35, 70, 118.3, 11.83, 60),
            new Row("german_twin_406mm_gun", 35, 70, 91.0, 15.38, 60),
            new Row("british_triple_16inch_gun", 35, 105, 179.2, 11.72, 80),
            new Row("us_triple_16inch_gun_mk6", 35, 105, 145.6, 14.42, 80),
            new Row("british_triple_16inch_gun_kai", 35, 105, 145.6, 14.42, 80),
            new Row("us_triple_16inch_gun_mk7", 35, 105, 112.0, 18.75, 80),
            new Row("german_skc40_triple_420mm_gun", 36, 108, 116.0, 18.62, 84),
            new Row("british_single_18inch_gun", 42, 42, 104.0, 8.08, 48),
            new Row("british_twin_18inch_gun", 42, 84, 166.4, 10.1, 72),
            new Row("us_twin_18inch_gun", 42, 84, 135.2, 12.43, 72),
            new Row("japanese_triple_460mm_gun", 42, 126, 128.0, 19.69, 96),
            new Row("soviet_triple_460mm_gun", 42, 126, 128.0, 19.69, 96),
            new Row("british_twin_20inch_gun", 49, 98, 187.2, 10.47, 84),
            new Row("us_twin_20inch_gun", 49, 98, 117.0, 16.75, 84)
    );

    @Test
    void catalogMatchesTable07RowForRow() {
        assertEquals(60, TABLE_07.size());
        assertEquals(60, CannonCatalog.ENTRIES.size());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < TABLE_07.size(); i++) {
            Row row = TABLE_07.get(i);
            CannonCatalog.Entry e = CannonCatalog.ENTRIES.get(i);
            assertEquals(row.id(), e.id(), "第 " + (i + 1) + " 行顺序");
            assertTrue(ids.add(e.id()), "重复 id " + e.id());
        }
    }

    @Test
    void formulaReproducesTable07() {
        for (int i = 0; i < TABLE_07.size(); i++) {
            Row row = TABLE_07.get(i);
            CannonCatalog.Entry e = CannonCatalog.ENTRIES.get(i);
            double c = e.caliberInches();
            int n = e.barrels();
            String id = row.id();
            assertEquals(row.panel(), CannonStatFormula.panelDamage(c), id + " 面板");
            assertEquals(row.salvo(), CannonStatFormula.salvoDamage(c, n), id + " 齐射");
            // 07 表装填列按 1~2 位小数显示（如日本14厘米单装炮 35.118 → 35.1），容差取 0.05
            assertEquals(row.reload(), CannonStatFormula.reloadTicks(c, n, e.tier()), 0.05, id + " 装填");
            assertEquals(row.dps(), CannonStatFormula.dps(c, n, e.tier()), 0.01, id + " DPS");
            assertEquals(row.weight(), CannonStatFormula.weight(c, n, CannonStatFormula.VelocityClass.STANDARD),
                    id + " 负重");
        }
    }

    @Test
    void velocityClassAdjustsWeightAfterBaseCeil() {
        // 5 英寸双联：基础 12 → 高初速 ceil(15.6)=16，低初速 ceil(10.2)=11
        assertEquals(12, CannonStatFormula.baseWeight(5, 2));
        assertEquals(16, CannonStatFormula.weight(5, 2, CannonStatFormula.VelocityClass.HIGH));
        assertEquals(11, CannonStatFormula.weight(5, 2, CannonStatFormula.VelocityClass.LOW));
        assertEquals(CannonStatFormula.VelocityClass.STANDARD, CannonStatFormula.VelocityClass.parse("bogus"));
    }

    @Test
    void tierMultipliers() {
        double base = CannonStatFormula.baseReloadTicks(3, 2);
        assertEquals(13.0, base, 1e-9);
        assertEquals(13.0, CannonStatFormula.reloadTicks(3, 2, EquipmentTier.INITIAL), 1e-9);
        assertEquals(10.4, CannonStatFormula.reloadTicks(3, 2, EquipmentTier.STANDARD), 1e-9);
        assertEquals(8.45, CannonStatFormula.reloadTicks(3, 2, EquipmentTier.IMPROVED), 1e-9);
        assertEquals(6.5, CannonStatFormula.reloadTicks(3, 2, EquipmentTier.ADVANCED), 1e-9);
    }

    @Test
    void resolveTicksAveragesToFractionalReload() {
        assertEquals(6, CannonStatFormula.resolveTicks(6.5, 0.7));
        assertEquals(7, CannonStatFormula.resolveTicks(6.5, 0.2));
        assertEquals(20, CannonStatFormula.resolveTicks(20.0, 0.0));
        assertEquals(0, CannonStatFormula.resolveTicks(-1, 0.1));
        int steps = 10_000;
        long sum = 0;
        for (int i = 0; i < steps; i++) sum += CannonStatFormula.resolveTicks(8.45, (i + 0.5) / steps);
        assertEquals(8.45, sum / (double) steps, 1e-3);
    }

    @Test
    void caliberFamilyThresholds() {
        assertEquals(CaliberFamily.SMALL, CannonAmmoRules.familyForInches(4.99));
        assertEquals(CaliberFamily.MEDIUM, CannonAmmoRules.familyForInches(5));
        assertEquals(CaliberFamily.MEDIUM, CannonAmmoRules.familyForInches(12.99));
        assertEquals(CaliberFamily.LARGE, CannonAmmoRules.familyForInches(13));
        assertEquals(4, ArtilleryCannonData.legacyCaliberForInches(4.9));
        assertEquals(8, ArtilleryCannonData.legacyCaliberForInches(5));
        assertEquals(8, ArtilleryCannonData.legacyCaliberForInches(8));
        assertEquals(16, ArtilleryCannonData.legacyCaliberForInches(13));
    }

    @Test
    void cannonJsonMatchesCatalogFallback() throws Exception {
        Gson gson = new Gson();
        for (CannonCatalog.Entry e : CannonCatalog.ENTRIES) {
            String res = "data/piranport/artillery/cannons/" + e.id() + ".json";
            ArtilleryCannonData json;
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(res)) {
                assertNotNull(in, "缺少 " + res);
                json = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), ArtilleryCannonData.class);
            }
            ArtilleryCannonData fb = CannonCatalog.fallbackData(e);
            assertEquals(fb, json, e.id() + " JSON 与代码回退值不一致");
            assertEquals(e.caliberInches(), json.caliberInches(), 1e-4, e.id());
            assertEquals(e.tier().id(), json.tier(), e.id());
            assertEquals(CannonStatFormula.panelDamage(e.caliberInches()), (int) json.damage(), e.id());
            assertEquals(CannonStatFormula.reloadTicks(e.caliberInches(), e.barrels(), e.tier()),
                    json.reloadTime(), 1e-3, e.id());
            String assetRoot = "assets/piranport/models/item/" + e.id();
            assertNotNull(getClass().getClassLoader().getResource(assetRoot + ".json"), "缺少模型 " + e.id());
            assertNotNull(getClass().getClassLoader().getResource(assetRoot + "_loaded.json"), "缺少装填模型 " + e.id());
        }
    }
}
