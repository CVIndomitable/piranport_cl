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

    public static float foldDegrees(float deployment) {
        return 90.0f * (1.0f - Math.clamp(deployment, 0.0f, 1.0f));
    }
}
