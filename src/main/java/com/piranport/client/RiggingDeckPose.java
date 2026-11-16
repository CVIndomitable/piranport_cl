package com.piranport.client;

/** 与 carrier_deck 几何共用的铰链尺寸。 */
public final class RiggingDeckPose {
    public static final float HINGE_X = 6.5f;
    public static final float HINGE_Y = -0.8f;
    public static final float CENTER_X = -10.0f;
    public static final float AIRCRAFT_Y = -0.64f;
    public static final float EDGE_Z = 9.0f;

    public static float aircraftZ(boolean firstSocket) {
        return firstSocket ? -3.0f : 4.0f;
    }

    private RiggingDeckPose() {}

    public static float pitchDegrees(float launchWeight, float landingWeight, float idlePitch, float launchPitch) {
        float launch = idlePitch + (launchPitch - idlePitch) * Math.clamp(launchWeight, 0.0f, 1.0f);
        // 同时收放时优先保持接机面水平；离板飞机仍跟随实际甲板姿态。
        return launch + (0.0f - launch) * Math.clamp(landingWeight, 0.0f, 1.0f);
    }
}
