package com.piranport.dungeon.escort;

/**
 * 《副本/12》护航节点刷怪几何：运输船沿航线推进，波次按进度刷在航线前方与侧翼。
 *
 * <p>全部是 XZ 平面的纯计算，不依赖世界，便于单测。进度 0 = 节点中心，1 = 目标点。</p>
 */
public final class EscortWaveGeometry {

    private static final double EPS = 1e-6;

    /** 刷怪锚点：路径前方点与侧翼点，均为 XZ 坐标。 */
    public record Anchors(double pathX, double pathZ, double flankX, double flankZ) {}

    private EscortWaveGeometry() {}

    /** 运输船当前进度：由起点到目标的直线距离占比，超出范围按端点钳制；起点与目标重合视为已到达。 */
    public static double progress(double startX, double startZ, double targetX, double targetZ,
                                  double shipX, double shipZ) {
        double total = Math.hypot(targetX - startX, targetZ - startZ);
        if (total < EPS) return 1.0;
        double remaining = Math.hypot(targetX - shipX, targetZ - shipZ);
        return Math.max(0.0, Math.min(1.0, 1.0 - remaining / total));
    }

    /**
     * 路径前方点位于航线上运输船前方 {@code aheadDistance} 格处（超出目标则钳到目标）；
     * 侧翼点在其上垂直于航线偏移 {@code flankOffset} 格，{@code sideSign} 为 +1/-1 选左右。
     */
    public static Anchors anchors(double startX, double startZ, double targetX, double targetZ,
                                  double progress, double aheadDistance, double flankOffset, double sideSign) {
        double length = Math.hypot(targetX - startX, targetZ - startZ);
        double ux = 0.0;
        double uz = 0.0;
        if (length > EPS) {
            ux = (targetX - startX) / length;
            uz = (targetZ - startZ) / length;
        }
        double along = Math.min(progress * length + aheadDistance, length);
        double pathX = startX + ux * along;
        double pathZ = startZ + uz * along;
        double flankX = pathX - uz * flankOffset * sideSign;
        double flankZ = pathZ + ux * flankOffset * sideSign;
        return new Anchors(pathX, pathZ, flankX, flankZ);
    }
}
