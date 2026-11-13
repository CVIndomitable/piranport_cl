package com.piranport.aviation;

/** 玩家飞机出击的纯视觉时间轴；各段时长在放飞时快照。 */
public record AircraftLaunchTimeline(int deployTicks, int rollTicks, int flightTicks) {
    public AircraftLaunchTimeline {
        if (deployTicks < 1 || rollTicks < 1 || flightTicks < 1) {
            throw new IllegalArgumentException("动画各阶段至少一 tick");
        }
    }

    public int departureTick() { return deployTicks + rollTicks; }
    public int durationTicks() { return departureTick() + flightTicks; }

    private static float progress(float age, float start, float duration) {
        return Math.clamp((age - start) / duration, 0.0f, 1.0f);
    }

    private static float smooth(float t) { return t * t * (3.0f - 2.0f * t); }

    public float deployment(float age) { return smooth(progress(age, 0, deployTicks)); }
    public float deckDeployment(float age) {
        if (age < departureTick()) return deployment(age);
        return 1.0f - smooth(progress(age, departureTick(), Math.min(deployTicks, flightTicks)));
    }
    public float roll(float age) { return smooth(progress(age, deployTicks, rollTicks)); }
    public float flight(float age) { return smooth(progress(age, departureTick(), flightTicks)); }
    public float scale(float age) { return 0.38f + (1.5f - 0.38f) * flight(age); }
}
