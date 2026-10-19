package com.piranport.dungeon.weather;

import com.piranport.dungeon.data.SceneData;

/** 天气战斗影响的纯规则，规则见《副本/27-天气与昼夜》。无 Minecraft 依赖，便于单测。 */
public final class WeatherRules {

    /** 《深海/01》大凤「晴空万里」：所在队伍受航空炸弹、航空鱼雷伤害的倍率。 */
    public static final float TAIHOU_AIR_ORDNANCE_FACTOR = 0.2f;

    private WeatherRules() {}

    /** 大凤在场时该节点视为白天；无节点天气时视为白天。 */
    public static SceneData effectiveScene(SceneData nodeScene, boolean taihouOnField) {
        if (taihouOnField || nodeScene == null) return SceneData.DAY;
        return nodeScene;
    }

    public static boolean blocksLaunch(SceneData scene) {
        return scene == SceneData.STORM;
    }

    public static boolean blocksLanding(SceneData scene) {
        return scene == SceneData.NIGHT;
    }

    public static boolean highlightsSubmarines(SceneData scene) {
        return scene == SceneData.CLEAR;
    }

    public static boolean glowsEnemies(SceneData scene) {
        return scene == SceneData.HOT_SUN;
    }

    public static double followRangeMultiplier(SceneData scene, double fogMultiplier, double hotSunMultiplier) {
        return switch (scene) {
            case FOG -> fogMultiplier;
            case HOT_SUN -> hotSunMultiplier;
            default -> 1.0;
        };
    }

    public static boolean isLightningTick(long gameTime, int periodTicks) {
        return periodTicks > 0 && gameTime % periodTicks == 0;
    }

    public static float airOrdnanceDamageFactor(boolean airOrdnance, boolean protectedByTaihou) {
        return airOrdnance && protectedByTaihou ? TAIHOU_AIR_ORDNANCE_FACTOR : 1.0f;
    }
}
