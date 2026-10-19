package com.piranport.dungeon.weather;

import com.piranport.dungeon.data.SceneData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherRulesTest {

    @Test
    void missingNodeSceneDefaultsToDay() {
        assertEquals(SceneData.DAY, WeatherRules.effectiveScene(null, false));
    }

    @Test
    void taihouOnFieldTurnsAnyWeatherIntoDay() {
        for (SceneData scene : SceneData.values()) {
            assertEquals(SceneData.DAY, WeatherRules.effectiveScene(scene, true));
        }
    }

    @Test
    void weatherSurvivesWithoutTaihou() {
        assertEquals(SceneData.NIGHT, WeatherRules.effectiveScene(SceneData.NIGHT, false));
        assertEquals(SceneData.CLEAR, WeatherRules.effectiveScene(SceneData.CLEAR, false));
    }

    @Test
    void onlyStormBlocksLaunch() {
        assertTrue(WeatherRules.blocksLaunch(SceneData.STORM));
        for (SceneData scene : SceneData.values()) {
            if (scene != SceneData.STORM) assertFalse(WeatherRules.blocksLaunch(scene));
        }
    }

    @Test
    void onlyNightBlocksLanding() {
        assertTrue(WeatherRules.blocksLanding(SceneData.NIGHT));
        for (SceneData scene : SceneData.values()) {
            if (scene != SceneData.NIGHT) assertFalse(WeatherRules.blocksLanding(scene));
        }
    }

    @Test
    void onlyClearHighlightsSubmarines() {
        assertTrue(WeatherRules.highlightsSubmarines(SceneData.CLEAR));
        assertFalse(WeatherRules.highlightsSubmarines(SceneData.HOT_SUN));
        assertFalse(WeatherRules.highlightsSubmarines(SceneData.DAY));
    }

    @Test
    void onlyHotSunGlowsEnemies() {
        assertTrue(WeatherRules.glowsEnemies(SceneData.HOT_SUN));
        assertFalse(WeatherRules.glowsEnemies(SceneData.CLEAR));
    }

    @Test
    void followRangeMultiplierFollowsFogAndSun() {
        assertEquals(0.6, WeatherRules.followRangeMultiplier(SceneData.FOG, 0.6, 1.4), 1e-9);
        assertEquals(1.4, WeatherRules.followRangeMultiplier(SceneData.HOT_SUN, 0.6, 1.4), 1e-9);
        assertEquals(1.0, WeatherRules.followRangeMultiplier(SceneData.STORM, 0.6, 1.4), 1e-9);
        assertEquals(1.0, WeatherRules.followRangeMultiplier(SceneData.DAY, 0.6, 1.4), 1e-9);
    }

    @Test
    void lightningFiresOnlyOnPeriodBoundaries() {
        assertTrue(WeatherRules.isLightningTick(0, 200));
        assertTrue(WeatherRules.isLightningTick(400, 200));
        assertFalse(WeatherRules.isLightningTick(399, 200));
        assertFalse(WeatherRules.isLightningTick(200, 0));
    }

    @Test
    void airOrdnanceReducedOnlyForProtectedAirHits() {
        assertEquals(WeatherRules.TAIHOU_AIR_ORDNANCE_FACTOR,
                WeatherRules.airOrdnanceDamageFactor(true, true), 1e-6f);
        assertEquals(1.0f, WeatherRules.airOrdnanceDamageFactor(true, false), 1e-6f);
        assertEquals(1.0f, WeatherRules.airOrdnanceDamageFactor(false, true), 1e-6f);
    }
}
