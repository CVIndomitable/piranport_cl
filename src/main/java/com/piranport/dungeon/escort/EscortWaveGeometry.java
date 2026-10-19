package com.piranport.dungeon.escort;

import net.minecraft.world.phys.Vec3;

/**
 * 《副本/12》护航节点刷怪几何：波次沿运输船航线推进，刷在航线前方与侧翼。
 *
 * <p>全部是 XZ 平面的纯计算，不依赖世界，便于单测。进度为航线弧长占比，见 {@link EscortRoute#progress}。</p>
 */
public final class EscortWaveGeometry {

    /** 刷怪锚点：路径前方点与侧翼点，均为 XZ 坐标。 */
    public record Anchors(double pathX, double pathZ, double flankX, double flankZ) {}

    private EscortWaveGeometry() {}

    /**
     * 路径前方点位于航线弧长 {@code progress} 处再前进 {@code aheadDistance} 格（超出终点则钳到终点）；
     * 侧翼点在其上垂直于局部航向偏移 {@code flankOffset} 格，{@code sideSign} 为 +1/-1 选左右。
     */
    public static Anchors anchors(EscortRoute route, double progress, double aheadDistance,
                                  double flankOffset, double sideSign) {
        double along = Math.min(progress * route.length() + aheadDistance, route.length());
        Vec3 path = route.pointAt(along);
        Vec3 dir = route.directionAt(along);
        double flankX = path.x - dir.z * flankOffset * sideSign;
        double flankZ = path.z + dir.x * flankOffset * sideSign;
        return new Anchors(path.x, path.z, flankX, flankZ);
    }
}
