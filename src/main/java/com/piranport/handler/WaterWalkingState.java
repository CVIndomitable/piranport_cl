package com.piranport.handler;

/**
 * 单个玩家实例的水上行走瞬时状态。
 *
 * <p>通过不序列化、不复制的 attachment 挂在玩家实例上，两逻辑端不共享状态。
 * 状态只由所属玩家的 tick 线程访问，断连或重生后随旧实体释放。
 */
public final class WaterWalkingState {
    private Object levelIdentity;
    private double surfaceY = Double.NaN;
    private float lastYaw;
    private Direction direction;

    /**
     * 换世界时不能沿用旧水面高度；使用实例身份也覆盖同维度世界被重新创建的情况。
     * 参数只用于身份比较，保持状态逻辑不依赖世界加载，便于独立验证生命周期。
     */
    void bindToLevel(Object level) {
        if (levelIdentity != level) {
            levelIdentity = level;
            clearSurface();
            direction = null;
        }
    }

    /** 保留原有 0.1 格容差：轻微波动不修正，明显下沉时回到进入水面的位置。 */
    double resolveSurfaceY(double currentY) {
        if (Double.isNaN(surfaceY)) {
            surfaceY = currentY;
        }
        return currentY < surfaceY - 0.1 ? surfaceY : currentY;
    }

    /** 离开水面或完全潜入水下后，下次浮起需要重新采样高度。 */
    void clearSurface() {
        surfaceY = Double.NaN;
    }

    /** 保留原有超过 5 度才重算的方向缓存阈值。 */
    Direction forwardDirection(float yaw) {
        if (direction == null || Math.abs(yaw - lastYaw) > 5.0f) {
            float yawRad = yaw * ((float) Math.PI / 180f);
            direction = new Direction(-Math.sin(yawRad), Math.cos(yawRad));
            lastYaw = yaw;
        }
        return direction;
    }

    record Direction(double x, double z) {}
}
