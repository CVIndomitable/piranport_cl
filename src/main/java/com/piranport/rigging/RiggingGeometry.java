package com.piranport.rigging;

/** Shared hull geometry for rendering and world-space attachment points. */
public final class RiggingGeometry {
    public static final double HULL_DOWN = 0.28;
    public static final double HULL_BACK = 0.34;
    public static final float SIDE_ITEM_SCALE = 0.42f;

    private RiggingGeometry() {}

    /** Model pixels, left and forward positive, measured from the hull origin. */
    public record SideMount(float left, float forward) {
        public static final float DOWN = -1.56f;
    }

    public static SideMount sideMount(RiggingSocket socket) {
        return switch (socket) {
            case SIDE_LEFT_1 -> new SideMount(9.5f, -6.0f);
            case SIDE_LEFT_2 -> new SideMount(9.5f, 6.0f);
            case SIDE_RIGHT_1 -> new SideMount(-9.5f, -6.0f);
            case SIDE_RIGHT_2 -> new SideMount(-9.5f, 6.0f);
            default -> throw new IllegalArgumentException("Not a side socket: " + socket);
        };
    }
}
