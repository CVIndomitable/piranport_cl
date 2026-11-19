package com.piranport.aviation;

/** 返航近身后的视觉时间轴：进场缩小、着舰滑行、空甲板复位。 */
public record AircraftLandingTimeline(int approachTicks, int rollTicks, int settleTicks) {
    public AircraftLandingTimeline {
        if (approachTicks < 1 || rollTicks < 1 || settleTicks < 1) {
            throw new IllegalArgumentException("降落各阶段至少一 tick");
        }
    }

    public int rollEndTick() { return approachTicks + rollTicks; }
    public int durationTicks() { return rollEndTick() + settleTicks; }

    /** 滑行完即隐藏该飞机，甲板复位阶段不再显示停放副本。 */
    public boolean showsAircraft(float age) { return age >= 0 && age < rollEndTick(); }

    /** 等复位完成才恢复停放展示；已收到但尚未到起始 tick 的动画也先清空甲板。 */
    public boolean holdsDeck(float age) { return age < durationTicks(); }

    private static float progress(float age, float start, int ticks) {
        float t = Math.clamp((age - start) / ticks, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public float approach(float age) { return progress(age, 0, approachTicks); }
    public float roll(float age) { return progress(age, approachTicks, rollTicks); }
    public float levelWeight(float age) {
        if (age < approachTicks) return progress(age, 0, Math.min(settleTicks, approachTicks));
        return 1.0f - progress(age, rollEndTick(), settleTicks);
    }

    public float scale(float age, float deckScale) {
        return 1.5f + (deckScale - 1.5f) * approach(age);
    }
}
